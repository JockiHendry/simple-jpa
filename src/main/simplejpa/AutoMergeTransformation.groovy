package simplejpa

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.griffon.ast.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.ast.AstUtils

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class AutoMergeTransformation extends AbstractASTTransformation {

    private static Logger log = LoggerFactory.getLogger(AutoMergeTransformation)

    static final String EXCLUDED_METHODS = ['toString', 'equals', 'hashCode']

    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        checkNodesForAnnotationAndType(astNodes[0], astNodes[1])
        AnnotationNode annotation = astNodes[0]
        ClassNode node = astNodes[1]

        log.debug "Starting @AutoMerge transformation for ${node.name}..."

        List<String> excludes = AstUtils.getMemberList(annotation, 'excludes')

        node.methods.each { MethodNode methodNode ->
            log.debug "Processing method: ${methodNode.name}..."
            if (!AstUtils.isValidMethod(methodNode)) {
                log.warn "Not transforming method: ${methodNode.name}"
                return
            }
            if (EXCLUDED_METHODS.contains(methodNode.name) || excludes.contains(methodNode.name)) {
                log.warn "Skipping method: ${methodNode.name}"
                return
            }
            wrapStatements(methodNode)
        }
    }

    private static void wrapStatements(MethodNode methodNode) {
        BlockStatement originalBlock = (BlockStatement) methodNode.code

        BlockStatement autoMergeCode = new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
            if (simplejpa.SimpleJpaUtil.instance.handler.getEntityManager() &&
                id!=null &&
                !simplejpa.SimpleJpaUtil.instance.handler.getEntityManager().contains(this)) {
                    simplejpa.SimpleJpaUtil.instance.handler.merge(this)
            }
        }[0]

        TryCatchStatement tryCatchStatement = new TryCatchStatement(originalBlock, autoMergeCode)
        BlockStatement result = new BlockStatement()
        result.addStatement(tryCatchStatement)

        methodNode.code = result
    }

}
