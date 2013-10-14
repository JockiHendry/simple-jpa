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

package simplejpa.swing.glazed.ast

import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import javax.swing.*
import java.awt.*
import java.util.List

@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
class ConditionSupportASTTransformation extends AbstractASTTransformation {

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if (astNodes.length != 2 || !(astNodes[0] instanceof AnnotationNode) || !(astNodes[1] instanceof AnnotatedNode)) {
            throw new RuntimeException("Internal error: expecting [AnnotationNode, AnnotatedNode] but got: " + Arrays.asList(astNodes))
        }
        if (((AnnotatedNode)astNodes[1]) instanceof ClassNode) {
            ClassNode parent = (ClassNode) astNodes[1]

            // Add the following field:
            //
            // List<Condition> conditions = []
            //
            FieldNode conditionsField = new FieldNode("conditions", ACC_PUBLIC, new ClassNode(List.class), parent,
                new ListExpression())
            parent.addField(conditionsField)

            //
            // Condition checking code
            //
            List<Statement> conditionCheckingStatements = new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
                def paramsDelegate = ['table': table, 'value': value, 'isSelected': isSelected, 'hasFocus': hasFocus,
                    'row': row, 'column': column, 'component': component]
                for (int i=0; i<conditions.size(); i++) {
                    def condition = conditions.get(i)
                    condition.if_.resolveStrategy = Closure.DELEGATE_FIRST
                    condition.if_.delegate = paramsDelegate
                    condition.then_?.resolveStrategy = Closure.DELEGATE_FIRST
                    condition.then_?.delegate = paramsDelegate
                    condition.else_?.resolveStrategy = Closure.DELEGATE_FIRST
                    condition.else_?.delegate = paramsDelegate

                    if (condition.if_.call(value)) {
                        if (condition.then_) {
                            condition.then_.call(component)
                        } else if (condition.then_property_ && condition.is_) {
                            component.metaClass.setProperty(component, condition.then_property_, condition.is_)
                        } else {
                            throw new IllegalArgumentException("Didn't find proper action for 'if then' condition!")
                        }
                    } else {
                        if (condition.else_) {
                            condition.else_.call(component)
                        } else if (condition.else_is_) {
                            component.metaClass.setProperty(component, condition.then_property_, condition.else_is_)
                        } else if (condition.else_property_ && condition.else_is_) {
                            component.metaClass.setProperty(component, condition.else_property_, condition.else_is_)
                        }
                    }
                }
                return component
            }

            List declareds = parent.getDeclaredMethods("getTableCellRendererComponent")
            if (declareds.isEmpty()) {
                //
                // Override:
                // public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
                //
                BlockStatement newBlock = new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
                    java.awt.Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                }[0]
                newBlock.addStatements(conditionCheckingStatements)
                MethodNode newMethod = new MethodNode("getTableCellRendererComponent", ACC_PUBLIC, new ClassNode(Component),
                    [new Parameter(new ClassNode(JTable), "table"), new Parameter(new ClassNode(Object), "value"),
                     new Parameter(ClassHelper.boolean_TYPE, "isSelected"), new Parameter(ClassHelper.boolean_TYPE, "hasFocus"),
                     new Parameter(ClassHelper.int_TYPE, "row"), new Parameter(ClassHelper.int_TYPE, "column")] as Parameter[],
                    null, newBlock)
                parent.addMethod(newMethod)

            } else {

                MethodNode currentMethod = declareds[0]
                Statement originalStatement = currentMethod.getCode()

                //
                // Create new method
                // private Component _getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
                //
                MethodNode hiddenMethod = new MethodNode("_getTableCellRendererComponent", ACC_PRIVATE, new ClassNode(Component),
                   [new Parameter(new ClassNode(JTable), "table"), new Parameter(new ClassNode(Object), "value"),
                    new Parameter(ClassHelper.boolean_TYPE, "isSelected"), new Parameter(ClassHelper.boolean_TYPE, "hasFocus"),
                    new Parameter(ClassHelper.int_TYPE, "row"), new Parameter(ClassHelper.int_TYPE, "column")] as Parameter[],
                    null, originalStatement)
                parent.addMethod(hiddenMethod)

                //
                // Change current getTableCellRendererComponent() implementation
                //
                BlockStatement newBlock = new AstBuilder().buildFromCode(CompilePhase.SEMANTIC_ANALYSIS, true) {
                    java.awt.Component component = _getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                }[0]
                newBlock.addStatements(conditionCheckingStatements)
                currentMethod.setCode(newBlock)

            }
        }

    }

}
