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

import ca.odell.glazedlists.swing.GlazedListsSwing
import javax.swing.table.TableModel

class EventTableModelFactory extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        if (FactoryBuilderSupport.checkValueIsTypeNotString(value, name, TableModel)) {
            return value
        }
        if (!attributes.containsKey('list')) {
            throw new IllegalArgumentException("In $name you must define a value for list of type BasicEventList")
        }
        if (!attributes.containsKey('columnNames')) {
            throw new IllegalArgumentException("In $name you must defined a value for columnNames of type List<String>")
        }
        if (!attributes.containsKey('columnValues')) {
            throw new IllegalArgumentException("In $name you must defined a value for columnValues for type List<String>")
        }
        def list = attributes.remove('list')
        def columnNames = attributes.remove('columnNames')
        def columnValues = attributes.remove('columnValues')
        def columnClasses = attributes.remove('columnClasses')

        return GlazedListsSwing.eventTableModelWithThreadProxyList(list,
            new TemplateTableFormat(columnNames, columnValues, columnClasses))
    }
}
