/*
 * Copyright 2013 Jocki Hendry.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simplejpa

import org.codehaus.griffon.ast.AbstractASTTransformation
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Temporal
import javax.persistence.TemporalType

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class DomainClassTransformation extends AbstractASTTransformation {

    private static final Logger LOG = LoggerFactory.getLogger(DomainClassTransformation.class)

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        checkNodesForAnnotationAndType(astNodes[0], astNodes[1])

        AnnotationNode annotation = astNodes[0]
        ClassNode classNode = astNodes[1]

        // If superclass is also a domain class, than do not inject any fields.
        if (classNode.superClass.annotations.find {
                    it.classNode.typeClass == DomainClass.class ||
                    it.classNode.typeClass == Entity.class
                }) {
            LOG.debug "Didn't inject attributes to $classNode because its superclass is a domain class!"
            return
        }

        // Add attribute for soft delete
        if (getBooleanProperty(annotation, 'excludeDeletedFlag')) {
            LOG.debug "Didn't inject 'deleted' attribute to $classNode"
        } else {
            LOG.debug "Creating 'deleted' for ${classNode.name}"
            classNode.addField("deleted", ACC_PUBLIC, ClassHelper.STRING_TYPE, new ConstantExpression("N"))
            LOG.debug "'deleted' added to ${classNode.name}"
        }

        // Add attribute for surrogate primary key
        if (getBooleanProperty(annotation, 'excludeId')) {
            LOG.debug "Didn't inject 'id' attribute to $classNode"
        } else {
            LOG.debug "Creating 'id' for ${classNode.name}"
            AnnotationNode idAnnotation = new AnnotationNode(ClassHelper.make(Id.class))
            AnnotationNode generatedValueAnnotation = new AnnotationNode(ClassHelper.make(GeneratedValue.class))
            Expression idGenerationStrategy = annotation.getMember('idGenerationStrategy')
            if (!idGenerationStrategy) {
                idGenerationStrategy = new PropertyExpression(new ClassExpression(ClassHelper.make(GenerationType)), 'TABLE')
            }
            generatedValueAnnotation.addMember("strategy", idGenerationStrategy)

            classNode.addField("id", ACC_PUBLIC, ClassHelper.Long_TYPE, null).addAnnotations([
                idAnnotation, generatedValueAnnotation])
            LOG.debug "'id' added to ${classNode.name}"
        }

        // Add attribute for auditing
        if (getBooleanProperty(annotation, 'excludeAuditing')) {
            LOG.debug "Didn't inject 'createdDate' and 'modifiedDate' attribute to $classNode"
        } else {
            LOG.debug "Creating auditing fields for ${classNode.name}"
            AnnotationNode temporalAnnotation = new AnnotationNode(ClassHelper.make(Temporal))
            temporalAnnotation.addMember('value', new PropertyExpression(
               new ClassExpression(ClassHelper.make(TemporalType)), 'TIMESTAMP'))
            classNode.addField("createdDate", ACC_PUBLIC, ClassHelper.make(Date.class), null)
                .addAnnotation(temporalAnnotation)
            classNode.addField("createdBy", ACC_PUBLIC, ClassHelper.make(String.class), null)
            classNode.addField("modifiedDate", ACC_PUBLIC, ClassHelper.make(Date.class), null)
                .addAnnotation(temporalAnnotation)
            classNode.addField("modifiedBy", ACC_PUBLIC, ClassHelper.make(String.class), null)
            LOG.debug "Auditing fields added to ${classNode.name}"
        }
    }

    private static boolean getBooleanProperty(AnnotationNode annotation, String propertyName, Boolean defaultValue = false) {
        ConstantExpression expression = (ConstantExpression) annotation.getMember(propertyName)
        expression?.trueExpression
    }

}
