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

import simplejpa.swing.glazed.GlazedColumn
import simplejpa.swing.glazed.renderer.DefaultTableHeaderRenderer
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class GlazedColumnFactory extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        GlazedColumn result
        if (FactoryBuilderSupport.checkValueIsTypeNotString(value, name, GlazedColumn)) {
            result = value
        } else {
            result = new GlazedColumn()
        }
        if (attributes.width) {
            if (attributes.width instanceof Collection) {
                def (min, pref, max) = attributes.width
                if (min) result.minWidth = min
                if (pref) result.preferredWidth = pref
                if (max) result.maxWidth = max
            } else if (attributes.width instanceof Number) {
                result.minWidth = attributes.width
                result.preferredWidth = attributes.width
                result.maxWidth = attributes.width
            }
            attributes.remove('width')
        }
        result
    }

    @Override
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        GlazedColumn glazedColumn = (GlazedColumn) parent
        if (child instanceof DefaultTableHeaderRenderer) {
            glazedColumn.headerRenderer = child
        } else if (child instanceof TableCellRenderer) {
            glazedColumn.cellRenderer = child
        } else if (child instanceof TableCellEditor) {
            glazedColumn.cellEditor = child
        }
    }
}
