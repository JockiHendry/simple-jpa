package simplejpa

import org.codehaus.griffon.ast.AbstractASTTransformation
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class DomainModelTransformation extends AbstractASTTransformation {

    private static final Logger LOG = LoggerFactory.getLogger(DomainModelTransformation.class)

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        checkNodesForAnnotationAndType(astNodes[0], astNodes[1])

        ClassNode classNode = astNodes[1]
        LOG.info "AST Transformation for domain class $classNode is being executed..."

        // If superclass is also a domain class, than do not inject any fields.
        if (classNode.superClass.annotations.find { it.classNode.typeClass == DomainModel.class }) {
            LOG.info "Didn't inject attributes to $classNode because its superclass is a domain class!"
            return
        }

        // Add attribute for soft delete
        classNode.addField("deleted", ACC_PUBLIC, ClassHelper.STRING_TYPE, new ConstantExpression("N"))

        // Add attribute for surrogate primary key
        AnnotationNode idAnnotation = new AnnotationNode(ClassHelper.make(Id.class))
        AnnotationNode generatedValueAnnotation = new AnnotationNode(ClassHelper.make(GeneratedValue.class))
        generatedValueAnnotation.addMember("strategy", new PropertyExpression(
            new ClassExpression(ClassHelper.make(GenerationType.class)), "TABLE"))
        classNode.addField("id", ACC_PUBLIC, ClassHelper.Long_TYPE, null).addAnnotations([
            idAnnotation, generatedValueAnnotation])

        // Add attribute for auditing
        AnnotationNode typeAnnotation = new AnnotationNode(ClassHelper.make(Type.class))
        typeAnnotation.addMember("type", new ConstantExpression("org.jadira.usertype.dateandtime.joda.PersistentDateTime"))
        classNode.addField("createdDate", ACC_PUBLIC, ClassHelper.make(DateTime.class), null)
            .addAnnotation(typeAnnotation)
        classNode.addField("modifiedDate", ACC_PUBLIC, ClassHelper.make(DateTime.class), null)
            .addAnnotation(typeAnnotation)
    }

}
