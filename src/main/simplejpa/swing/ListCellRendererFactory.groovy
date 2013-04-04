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

package simplejpa.swing

import simplejpa.validation.ErrorLabel

import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.ListCellRenderer
import javax.swing.plaf.basic.BasicComboBoxRenderer


class ListCellRendererFactory extends AbstractFactory {

    public ListCellRenderer newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        if (FactoryBuilderSupport.checkValueIsTypeNotString(value, name, ListCellRenderer)) {
            return value
        }
        if (!attributes.containsKey('template')) {
            throw new IllegalArgumentException("In $name you must define a value for template of type String")
        }
        String template = attributes.remove('template')
        return new TemplateListCellRenderer(template)
    }

    public boolean isLeaf() {
        return true
    }

}
