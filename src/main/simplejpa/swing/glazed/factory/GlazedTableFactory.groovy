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

package simplejpa.swing.glazed.factory

import ca.odell.glazedlists.impl.gui.SortingStrategy
import simplejpa.swing.glazed.GlazedColumn
import simplejpa.swing.glazed.GlazedTable

class GlazedTableFactory extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        GlazedTable result = new GlazedTable()
        if (FactoryBuilderSupport.checkValueIsTypeNotString(value, name, GlazedTable)) {
            result = value
        }
        if (!result && !attributes.containsKey('list')) {
            throw new IllegalArgumentException("In $name you must define a value for list of type EventList")
        }
        if (attributes.containsKey('list')) {
            def list = attributes.remove('list')
            result.eventList = list
        }
        if (attributes.containsKey('sortingStrategy')) {
            Object strategy = attributes.remove('sortingStrategy')
            if (!(strategy instanceof SortingStrategy)) {
                throw new IllegalArgumentException("sortingStrategy in $name must be an implementation of SortingStrategy")
            }
            result.sortingStrategy = strategy
        }
        result
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        GlazedTable table = (GlazedTable) parent
        if (child instanceof GlazedColumn) {
            if (!child.expression && !child.property) {
                throw new IllegalArgumentException("In eventColumn you must define a value for then_property_ or expression")
            }
            table.addEventColumn(child)
        }
    }

    @Override
    void onNodeCompleted(FactoryBuilderSupport builder, Object parent, Object node) {
        ((GlazedTable) node).build()
    }
}
