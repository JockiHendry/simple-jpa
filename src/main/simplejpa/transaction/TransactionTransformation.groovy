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
import simplejpa.SimpleJpaUtil
import simplejpa.ast.AstUtils
import griffon.util.*

import javax.persistence.PersistenceException

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class TransactionTransformation extends AbstractASTTransformation {

    private static final Logger log = LoggerFactory.getLogger(TransactionTransformation.class)

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {

        AnnotationNode annotation = astNodes[0]
        AnnotatedNode node = astNodes[1]

        log.debug "Starting @Transaction transformation for ${node.text}..."

        if (getPolicy(annotation)==Transaction.Policy.SKIP) return

        ClassNode annotationClass = new ClassNode(Transaction.class)

        if (node instanceof ClassNode) {

            log.debug "@Transaction AST on class: ${node.name}"
            log.debug "@Transaction AST on fields: ${node.fields.each {it.name}.join(',')}"

            node.fields?.each { FieldNode field ->
                if (field.initialExpression instanceof ClosureExpression &&
                    field.getAnnotations(annotationClass).isEmpty()) {
                        log.debug "Processing field $field.name..."
                        wrapStatements(field.initialExpression, node, annotation)
                }
            }

            log.debug "Methods are ${node.methods}"
            node.methods?.each { MethodNode method ->
                if (!AstUtils.isValidMethod(method) || !method.getAnnotations(annotationClass).isEmpty()) {
                    log.warn "Not transforming method: ${method.name}"
                } else {
                    if (method.getAnnotations(annotationClass).isEmpty()) {
                        log.debug "Processing method $method.name..."
                        wrapStatements(method, node, annotation)
                    }
                }
            }

        } else if (node instanceof FieldNode) {

            log.debug "It is a field: ${node.name}"
            if (node.initialExpression instanceof ClosureExpression) {
                log.debug "Processing field $node.name..."
                wrapStatements(node.initialExpression, node, annotation)
            }

        } else if (node instanceof MethodNode) {

            log.debug "It is a method: ${node.name}"
            if (!AstUtils.isValidMethod(node)) {
                log.warn "Not transforming method: ${node.name}"
            } else {
                log.debug "Processing method $node.name..."
                wrapStatements(node, node, annotation)
            }

        }

        // Entry to write
        List entries = []
        for (ClassNode c: sourceUnit.AST.classes) {
            if (!c.getAnnotations(annotationClass).isEmpty()) {
                entries << c.name
            }
        }

        // Check if files already registered in output file
        File outputFile = BuildSettingsHolder.settings.baseDir.toPath().
            resolve("griffon-app").resolve("resources").resolve(SimpleJpaUtil.FILE_ANNOTATED).toFile()
        if (outputFile.exists()) {
            outputFile.eachLine { line ->
                for (String entry : entries.toArray()) {
                    if (line.equals(entry)) entries.remove(entry)
                }
            }
        }
        if (entries.isEmpty()) return

        // Add this class to output file
        def out = new BufferedOutputStream(new FileOutputStream(outputFile, true))
        for (String entry: entries) {
            out.write("${entry}\n".bytes)
        }
        out.close()
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

    private static void wrapStatements(MethodNode method, AnnotatedNode node, AnnotationNode annotation) {
        log.debug "Transforming method..."
        Statement code = method.getCode()
        Statement wrappedCode = wrapStatements(code, node, annotation)
        if (code!=wrappedCode) {
            log.debug "Set a new code to method..."
            method.setCode(wrappedCode)
        }
    }

    private static void wrapStatements(ClosureExpression closure, AnnotatedNode node, AnnotationNode annotation) {
        log.debug "Transforming closure..."
        Statement code = closure.getCode()
        Statement wrappedCode = wrapStatements(code, node, annotation)
        if (code!=wrappedCode) {
            log.debug "Set a new code to closure..."
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

        log.debug "New code for closure has been created"
        newBlock
    }

}
