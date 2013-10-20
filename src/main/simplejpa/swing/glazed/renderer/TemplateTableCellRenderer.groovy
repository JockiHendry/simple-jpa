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

package simplejpa.swing.glazed.renderer

import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.text.TemplateEngine
import simplejpa.swing.TemplateRenderer
import simplejpa.swing.glazed.ast.ConditionSupport

import javax.swing.table.DefaultTableCellRenderer

@ConditionSupport
class TemplateTableCellRenderer extends DefaultTableCellRenderer {

    Template template
    Closure expression

    public TemplateTableCellRenderer() {
        super()
    }

    public TemplateTableCellRenderer(String templateString) {
        TemplateEngine templateEngine = new SimpleTemplateEngine()
        template = templateEngine.createTemplate(templateString)
    }

    public TemplateTableCellRenderer(Closure templateExpression) {
        this.expression = templateExpression
    }

    @Override
    protected void setValue(Object value) {
        if (template) {
            super.setValue(TemplateRenderer.make(template, value))
        } else if (expression) {
            super.setValue(TemplateRenderer.make(expression, value))
        }
    }
}