/*
 * Copyright 2015 Jocki Hendry.
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

package simplejpa.swing.glazed.factory

import simplejpa.swing.template.TemplateTableCellRenderer

class TemplateRendererFactory extends ConditionRendererFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        def result
        def template
        def keys

        if (FactoryBuilderSupport.checkValueIsType(value, name, String)) {
            template = value
        } else if (FactoryBuilderSupport.checkValueIsType(value, name, TemplateTableCellRenderer)) {
            return value
        } else {
            keys = ['templateString', 'templateExpression', 'exp']
            for (String s: keys) {
                template = attributes.remove(s)
            }
        }

        if (template) {
            result = new TemplateTableCellRenderer(template)
        } else {
            throw new IllegalArgumentException("In $name you must define a value for one of $keys.  List of attributes: $attributes")
        }

        result
    }

}
