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

package simplejpa.swing.glazed

import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.gui.AdvancedTableFormat

class GlazedTableFormat implements AdvancedTableFormat {

    List<GlazedColumn> eventColumns

    public GlazedTableFormat(List eventColumns) {
        this.eventColumns = eventColumns
    }

    @Override
    int getColumnCount() {
        eventColumns.size()
    }

    @Override
    String getColumnName(int i) {
        eventColumns[i].name
    }

    @Override
    Object getColumnValue(Object e, int i) {
        GlazedColumn eventColumn = eventColumns[i]
        def result = null
        if (eventColumn.expression) {
            result = eventColumn.expression.call(e)
        } else if (eventColumn.property) {
            result = e.metaClass.getProperty(e, eventColumn.property)
        }
        result
    }

    @Override
    Class getColumnClass(int i) {
        if (eventColumns[i].columnClass) {
            return eventColumns[i].columnClass
        } else {
            return Object
        }
    }

    @Override
    Comparator getColumnComparator(int i) {
        if (eventColumns[i].comparator) {
            return eventColumns[i].comparator
        }
        GlazedLists.comparableComparator()
    }
}
