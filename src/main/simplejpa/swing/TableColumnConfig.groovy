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

import ca.odell.glazedlists.swing.DefaultEventSelectionModel
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter

@Deprecated
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
                if (method.contains('.')) {
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
                    }
                } else if (method=='linkRenderer') {
                    column.cellRenderer = new LinkRenderer()
                    table.addMouseMotionListener(new MouseMotionAdapter() {
                        @Override
                        void mouseMoved(MouseEvent e) {
                            if (table.columnAtPoint(e.point)==index && table.getValueAt(table.rowAtPoint(e.point), table.columnAtPoint(e.point)) != '') {
                                table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                            } else {
                                table.setCursor(Cursor.getDefaultCursor())
                            }
                        }
                    })
                    table.addMouseListener(new MouseAdapter() {
                        @Override
                        void mouseClicked(MouseEvent e) {
                            if (table.columnAtPoint(e.point)==index && table.getValueAt(table.selectedRow, table.selectedColumn) != '') {
                                def selectedValue
                                if (table.selectionModel instanceof DefaultEventSelectionModel) {
                                    selectedValue = table.selectionModel.selected[0]
                                } else {
                                    selectedValue = table.getValueAt(table.selectedRow, table.selectedColumn)
                                }
                                if (v instanceof String) {
                                    DialogUtils.showMVCGroup(v, ['value': selectedValue], 'Popup', builder.getVariable('app'),
                                        builder.getVariable('view'))
                                } else if (v instanceof Closure) {
                                    v.call(selectedValue)
                                }
                            }
                        }
                    })
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

    class LinkRenderer extends DefaultTableCellRenderer {

        @Override
        Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            def original = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            if (!isSelected && original && (original instanceof JLabel)) {
                original.setText("<html><a href>${original.getText()}</a></html>")
            }
            return original
        }

    }


}

