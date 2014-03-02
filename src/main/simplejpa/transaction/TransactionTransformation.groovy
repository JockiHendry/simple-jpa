package simplejpa.transaction

import org.codehaus.griffon.ast.AbstractASTTransformation
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import griffon.util.*
import javax.persistence.PersistenceException

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class TransactionTransformation extends AbstractASTTransformation {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionTransformation.class)

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {

        AnnotationNode annotation = astNodes[0]
        AnnotatedNode node = astNodes[1]

        LOG.debug "Starting AST Transformation for ${node}..."

        if (getPolicy(annotation)==Transaction.Policy.SKIP) return

        if (node instanceof ClassNode) {

            LOG.debug "It is a class: ${node.name}"
            ClassNode annotationClass = new ClassNode(Transaction.class)

            LOG.debug "Fields are ${node.fields}"
            node.fields?.each { FieldNode field ->
                if (field.initialExpression instanceof ClosureExpression &&
                    field.getAnnotations(annotationClass).isEmpty()) {
                        LOG.debug "Processing field $field.name..."
                        wrapStatements(field.initialExpression, node, annotation)
                }
            }

            LOG.debug "Methods are ${node.methods}"
            node.methods?.each { MethodNode method ->
                if (!isValidMethod(methodDescriptorFor(method)) || !method.getAnnotations(annotationClass).isEmpty()) {
                    LOG.warn "Not transforming method: ${method.name}"
                } else {
                    if (method.getAnnotations(annotationClass).isEmpty()) {
                        LOG.debug "Processing method $method.name..."
                        wrapStatements(method, node, annotation)
                    }
                }
            }

        } else if (node instanceof FieldNode) {

            LOG.debug "It is a field: ${node.name}"
            if (node.initialExpression instanceof ClosureExpression) {
                LOG.debug "Processing field $node.name..."
                wrapStatements(node.initialExpression, node, annotation)
            }

        } else if (node instanceof MethodNode) {

            LOG.debug "It is a method: ${node.name}"
            if (!isValidMethod(methodDescriptorFor(node))) {
                LOG.warn "Not transforming method: ${node.name}"
            } else {
                LOG.debug "Processing method $node.name..."
                wrapStatements(node, node, annotation)
            }

        }
    }

    public static boolean isValidMethod(GriffonClassUtils.MethodDescriptor method) {
        return GriffonClassUtils.isInstanceMethod(method) &&
            !GriffonClassUtils.isBasicMethod(method) &&
            !GriffonClassUtils.isGroovyInjectedMethod(method) &&
            !GriffonClassUtils.isThreadingMethod(method) &&
            !GriffonClassUtils.isArtifactMethod(method) &&
            !GriffonClassUtils.isMvcMethod(method) &&
            !GriffonClassUtils.isServiceMethod(method) &&
            !GriffonClassUtils.isEventPublisherMethod(method) &&
            !GriffonClassUtils.isObservableMethod(method) &&
            !GriffonClassUtils.isResourceHandlerMethod(method) &&
            !GriffonClassUtils.isContributionMethod(method);
    }

    private static Transaction.Policy getPolicy(AnnotationNode annotation) {
        PropertyExpression value = (PropertyExpression) annotation.getMember("value")
        value? Transaction.Policy.valueOf(value.getPropertyAsString()): Transaction.Policy.NORMAL
    }

    private static boolean isResume(AnnotationNode annotation) {
        if (getPolicy(annotation)==Transaction.Policy.SKIP_PROPAGATION) {
            return false
        } else {
            return true
        }
    }

    private static boolean isNewSession(AnnotationNode annotation) {
        ConstantExpression value = (ConstantExpression) annotation.getMember("newSession")
        value?.getValue()?:false
    }

    private static GriffonClassUtils.MethodDescriptor methodDescriptorFor(MethodNode method) {
        if (method==null) return null
        String[] parameterTypes = method.getParameters().collect { it.getType().getPlainNodeReference().getName() }
        new GriffonClassUtils.MethodDescriptor(method.name, parameterTypes, method.modifiers)
    }

    private static void wrapStatements(MethodNode method, AnnotatedNode node, AnnotationNode annotation) {
        LOG.debug "Transforming method..."
        Statement code = method.getCode()
        Statement wrappedCode = wrapStatements(code, node, annotation)
        if (code!=wrappedCode) {
            LOG.debug "Set a new code to method..."
            method.setCode(wrappedCode)
        }
    }

    private static void wrapStatements(ClosureExpression closure, AnnotatedNode node, AnnotationNode annotation) {
        LOG.debug "Transforming closure..."
        Statement code = closure.getCode()
        Statement wrappedCode = wrapStatements(code, node, annotation)
        if (code!=wrappedCode) {
            LOG.debug "Set a new code to closure..."
            closure.setCode(wrappedCode)
        }
    }

    private static Statement wrapStatements(Statement code, AnnotatedNode node, AnnotationNode annotation) {
        if (!(code instanceof BlockStatement)) return code

        BlockStatement originalBlock = (BlockStatement) code
        BlockStatement newBlock = new BlockStatement()

        BlockStatement catchPersistenceBlock = new BlockStatement()
        catchPersistenceBlock.addStatement(new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
            try {
                __transactionError__ = true
                rollbackTransaction()
            } finally {
                throw ex
            }
        }[0])

        BlockStatement catchReturnFailedBlock = new BlockStatement()
        catchReturnFailedBlock.addStatement(new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
            rollbackTransaction()
        })

        BlockStatement catchGenericBlock = new BlockStatement()
        catchGenericBlock.addStatement(new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
           try {
               __transactionError__ = true
               rollbackTransaction()
           } finally {
               throw ex
           }
        }[0])

        BlockStatement finallyBlock = new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
            if (!__transactionError__) {
                commitTransaction()
            }
        }[0]

        TryCatchStatement tryCatchStatement = new TryCatchStatement(originalBlock, finallyBlock)
        tryCatchStatement.addCatch(new CatchStatement(new Parameter(new ClassNode(PersistenceException.class), "ex"),
            catchPersistenceBlock))
        tryCatchStatement.addCatch(new CatchStatement(new Parameter(new ClassNode(ReturnFailedSignal.class), "ex"),
            catchReturnFailedBlock))
        tryCatchStatement.addCatch(new CatchStatement(new Parameter(new ClassNode(Exception.class), "ex"),
            catchGenericBlock))

        List beginTransactionParams = []
        beginTransactionParams << new ConstantExpression(isResume(annotation))
        beginTransactionParams << new ConstantExpression(isNewSession(annotation))

        newBlock.addStatement(new ExpressionStatement(new DeclarationExpression(
            new VariableExpression("__transactionError__", new ClassNode(Boolean)), Token.newSymbol("=", 1,1),
            new ConstantExpression(Boolean.FALSE))));
        MethodCallExpression beginTransactionCall = new MethodCallExpression(new VariableExpression("this"),
                "beginTransaction", new ArgumentListExpression(beginTransactionParams))
        newBlock.addStatement(new ExpressionStatement(beginTransactionCall))
        newBlock.addStatement(tryCatchStatement)

        LOG.debug "New code for closure has been created"
        newBlock
    }

}
