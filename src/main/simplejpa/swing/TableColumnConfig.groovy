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

import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel
import java.awt.Component


class TableColumnConfig extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        if (!(builder.getCurrent() instanceof JTable)) {
            throw new IllegalArgumentException("$name must be declared inside JTable")
        }

        JTable table = (JTable) builder.getCurrent()
        TableColumnModel columnModel = table.getColumnModel()

        attributes.each { index, configs ->
            TableColumn column = columnModel.getColumn(index)
            configs.each { String method, v ->
                def m = method.tokenize('.')
                if (m[0]=='cellRenderer') {
                    if (!column.cellRenderer) {
                        column.cellRenderer = new DefaultTableCellRenderer()
                    }
                    column.cellRenderer."${m[1]}" = v
                } else if(m[0]=='headerRenderer') {
                    if (!column.headerRenderer) {
                        column.headerRenderer = new HeaderRenderer(table)
                    }
                    if (column.headerRenderer instanceof HeaderRenderer) {
                        column.headerRenderer.mapCustomize."${m[1]}" = v
                    } else {
                        column.headerRenderer."${m[1]}" = v
                    }
                } else {
                    column."$method" = v
                }
            }
        }
        attributes.clear()

        columnModel
    }

    class HeaderRenderer implements TableCellRenderer {

        DefaultTableCellRenderer renderer
        Map mapCustomize = [:]

        public HeaderRenderer(JTable table) {
            this.renderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()
        }

        @Override
        Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            mapCustomize.each { String k, def v ->
                label."$k" = v
            }
            label
        }
    }


}

