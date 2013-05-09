package simplejpa.transaction

import org.codehaus.griffon.ast.AbstractASTTransformation
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.runtime.metaclass.MethodHelper
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import griffon.util.GriffonClassUtils

import javax.persistence.PersistenceException

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class TransactionTransformation extends AbstractASTTransformation {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionTransformation.class)

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        LOG.info "AST Transformation for SimpleJPATransaction is being executed..."
        checkNodesForAnnotationAndType(astNodes[0], astNodes[1])

        ClassNode classNode = astNodes[1]

        classNode.fields?.each { FieldNode field ->
            LOG.info "Inspecting field $field.name..."
            if (field.initialExpression instanceof ClosureExpression) {
                wrapStatements(field.initialExpression, classNode)
            }
        }

        classNode.methods?.each { MethodNode method ->
            LOG.info "Inspecting method $method.name..."
            if (GriffonClassUtils.isPlainMethod(methodDescriptorFor(method))) {
                wrapStatements(method, classNode)
            }
        }
    }

    private static GriffonClassUtils.MethodDescriptor methodDescriptorFor(MethodNode method) {
        if (method==null) return null
        String[] parameterTypes = method.getParameters().collect { it.getType().getPlainNodeReference().getName() }
        new GriffonClassUtils.MethodDescriptor(method.name, parameterTypes, method.modifiers)
    }

    private static void wrapStatements(MethodNode method, ClassNode classNode) {
        LOG.info "Transforming method..."
        Statement code = method.getCode()
        Statement wrappedCode = wrapStatements(code, classNode)
        if (code!=wrappedCode) {
            LOG.info "Set a new code to method..."
            method.setCode(wrappedCode)
        }
    }

    private static void wrapStatements(ClosureExpression closure, ClassNode classNode) {
        LOG.info "Transforming closure..."
        Statement code = closure.getCode()
        Statement wrappedCode = wrapStatements(code, classNode)
        if (code!=wrappedCode) {
            LOG.info "Set a new code to closure..."
            closure.setCode(wrappedCode)
        }
    }

    private static Statement wrapStatements(Statement code, ClassNode classNode) {
        if (!(code instanceof BlockStatement)) return code

        BlockStatement originalBlock = (BlockStatement) code
        BlockStatement newBlock = new BlockStatement()

        BlockStatement catchPersistenceBlock = new BlockStatement()
        catchPersistenceBlock.addStatement(new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
            log.debug "start of catch_persistence_block"
            try {
                model.errors.'exception' = ex.message
                log.debug "trying to rollback from catch_persistence_block"
                rollbackTransaction()
            } finally {
                log.error "Persistence Error: ${ex.message}"
                log.debug "signaling error event from catch_persistence_block..."
                app.event('jpaError', [ex])
                log.debug "rethrowing exception from catch_persistence_block..."
                throw new Exception(ex)
            }
            log.info "end of catch_persistence_block"
        }[0])

        BlockStatement catchReturnFailedBlock = new BlockStatement()
        catchReturnFailedBlock.addStatement(new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
            log.debug "trying to rollback from catch_return_failed"
            rollbackTransaction()
            log.debug "end of catch_return_failed"
        })

        BlockStatement catchGenericBlock = new BlockStatement()
        catchGenericBlock.addStatement(new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
           log.debug "start of catch_generic_block"
           try {
               model.errors.'exception' = ex.message
               log.debug "trying to rollback from catch_generic_block"
               rollbackTransaction()
           } finally {
               log.error "Exception: ${ex.message}"
               log.debug "rethrowing exception from catch_generic_block"
               throw new Exception(ex)
           }
           log.debug "end of catch_generic_block"
        }[0])

        BlockStatement finallyBlock = new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
            log.debug "start of finally_block"
            if (!model.errors.'exception') {
                log.debug "committing transaction in finally_block..."
                commitTransaction()
            }
            model.errors.remove('exception')
            log.debug "end of finally_block"
        }[0]

        TryCatchStatement tryCatchStatement = new TryCatchStatement(originalBlock, finallyBlock)
        tryCatchStatement.addCatch(new CatchStatement(new Parameter(new ClassNode(PersistenceException.class), "ex"),
            catchPersistenceBlock))
        tryCatchStatement.addCatch(new CatchStatement(new Parameter(new ClassNode(ReturnFailedSignal.class), "ex"),
            catchReturnFailedBlock))
        tryCatchStatement.addCatch(new CatchStatement(new Parameter(new ClassNode(Exception.class), "ex"),
            catchGenericBlock))

        newBlock.addStatement(new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
            beginTransaction()
        })
        newBlock.addStatement(tryCatchStatement)

        LOG.info "New code for closure has been created"
        newBlock
    }

}
