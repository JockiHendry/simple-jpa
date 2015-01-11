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



package simplejpa.swing.glazed

import ca.odell.glazedlists.EventList
import ca.odell.glazedlists.SortedList
import ca.odell.glazedlists.impl.gui.SortingStrategy
import ca.odell.glazedlists.swing.AdvancedTableModel
import ca.odell.glazedlists.swing.GlazedListsSwing
import ca.odell.glazedlists.swing.TableComparatorChooser
import groovy.beans.Bindable
import simplejpa.swing.glazed.renderer.DefaultTableHeaderRenderer
import javax.swing.*
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumnModel
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.text.MessageFormat
import griffon.util.*

class GlazedTable extends JTable implements PropertyChangeListener {

    static final MessageFormat printHeader, printFooter

    static {
        printHeader = new MessageFormat(ApplicationHolder.application.getMessage('simplejpa.table.print.header', ''))
        printFooter = new MessageFormat(ApplicationHolder.application.getMessage('simplejpa.table.print.footer', 'Page {0}'))
    }

    EventList eventList
    List<GlazedColumn> eventColumns = []
    List<GlazedColumn> visibleColumns = []
    SortingStrategy sortingStrategy
    TableComparatorChooser tableComparatorChooser
    Closure onValueChanged
    @Bindable Boolean isRowSelected
    @Bindable Boolean isNotRowSelected
    GlazedTableFormat tableFormat
    Action doubleClickAction
    Action enterKeyAction
    List<JMenuItem> popupMenuItems = []

    private static final Random random = new Random()

    public GlazedTable() {
        this(null)
    }

    public GlazedTable(EventList eventList) {
        super()
        this.eventList = eventList
        getTableHeader().setDefaultRenderer(new JTableHeader().getDefaultRenderer())
        setRowSorter(null)
        setAutoCreateRowSorter(false)
        setAutoCreateColumnsFromModel(false)
    }

    public void addEventColumn(GlazedColumn eventColumn) {
        if (!eventColumn.modelIndex) {
            eventColumn.modelIndex = eventColumns.size()
        }
        eventColumn.identifier = eventColumn.modelIndex
        if (!eventColumn.getPropertyChangeListeners().contains(this)) eventColumn.addPropertyChangeListener(this)
        eventColumns << eventColumn
    }

    public void build() {

        // Build table model
        eventColumns.sort { it.modelIndex }
        visibleColumns = eventColumns.findAll { it.visible }

        tableFormat = new GlazedTableFormat(visibleColumns)
        if (sortingStrategy && !(eventList instanceof SortedList)) {
            eventList = new SortedList(eventList, new Comparator() {
                @Override
                int compare(Object o1, Object o2) {
                    0
                }
            })
        }
        AdvancedTableModel tableModel = GlazedListsSwing.eventTableModelWithThreadProxyList(eventList, tableFormat)
        setModel(tableModel)

        // Remove and add columns
        TableColumnModel cm = getColumnModel()
        while (cm.getColumnCount() > 0) {
            cm.removeColumn(cm.getColumn(0))
        }
        int modelIndex = 0
        visibleColumns.findAll{ it.visible }.each{ GlazedColumn c ->
            if (!c.headerRenderer) c.headerRenderer = new DefaultTableHeaderRenderer()
            if (isInstanceOfNumber(c.columnClass)) {
                c.headerRenderer.addPropertyValue('horizontalAlignment', SwingConstants.RIGHT)
            } else {
                c.headerRenderer.addPropertyValue('horizontalAlignment', SwingConstants.LEFT)
            }
            c.modelIndex = modelIndex++
            addColumn(c)
        }

        // Add sorting
        if (sortingStrategy) {
            tableComparatorChooser = TableComparatorChooser.install(this, eventList, sortingStrategy)
        }

        // Event selection
        selectionModel = GlazedListsSwing.eventSelectionModelWithThreadProxyList(eventList)
        isRowSelected = false
        isNotRowSelected = true
        selectionModel.valueChanged = {
            isRowSelected = !selectionModel.isSelectionEmpty()
            isNotRowSelected = !isRowSelected
            firePropertyChange("isRowSelected", !isRowSelected, isRowSelected)
            firePropertyChange("isNotRowSelected", !isNotRowSelected, isNotRowSelected)
        }
        if (onValueChanged) {
            onValueChanged.delegate = this
            selectionModel.valueChanged = onValueChanged
        }

        // Key binding
        if (enterKeyAction) {
            def actionKey = "Action" + random.nextLong()
            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), actionKey)
            actionMap.put(actionKey, enterKeyAction)
        }
        if (doubleClickAction) {
            if (!getMouseListeners().contains(doubleClickHandler)) {
                addMouseListener(doubleClickHandler)
            }
        }

        // Begin adding popup menu
        JPopupMenu popupMenu = new JPopupMenu()

        // 'Copy Cell' menu
        Action copyCellAction = new AbstractAction('Copy Cell') {
            @Override
            void actionPerformed(ActionEvent e) {
                int r = getSelectedRow()
                int c = getSelectedColumn()
                if ((r >= 0) && (c >= 0)) {
                    String v = getValueAt(r, c)
                    Toolkit.defaultToolkit.systemClipboard.setContents(new StringSelection(v), null)
                }
            }
        }
        JMenuItem menuCopy = new JMenuItem(copyCellAction)
        getActionMap().put('copyCell', copyCellAction)
        popupMenu.add(menuCopy)

        // 'Print' menu
        Action printAction = new AbstractAction('Print') {
            @Override
            void actionPerformed(ActionEvent e) {
                print(JTable.PrintMode.FIT_WIDTH, printHeader, printFooter)
            }
        }
        JMenuItem menuPrint = new JMenuItem(printAction)
        popupMenu.add(menuPrint)

        if (!popupMenuItems.empty) {
            popupMenu.addSeparator()
            for (JMenuItem menuItem: popupMenuItems) {
                popupMenu.add(menuItem)
            }
        }
        setComponentPopupMenu(popupMenu)

        // Refresh
        UIManager.getLookAndFeelDefaults().put("Table.alternateRowColor", new Color(242,242,242))
        updateUI()
    }

    private boolean isInstanceOfNumber(Class aClass) {
        if (!aClass) return false
        Class superClass = aClass.superclass
        while (superClass.simpleName!="Object") {
            if (superClass.simpleName=="Number") return true
            superClass = aClass.superclass
        }
        false
    }

    @Override
    void propertyChange(PropertyChangeEvent evt) {
        if (evt.propertyName == 'visible') {
            build()
            super.invalidate()
            super.repaint()
        }
    }

    final def doubleClickHandler = new MouseAdapter() {
        @Override
        void mouseClicked(MouseEvent e) {
            if (doubleClickAction.enabled) {
                if (e.clickCount==2) {
                    doubleClickAction.actionPerformed(null)
                }
            }
        }
    }

    void addPopupMenuItem(JMenuItem menuItem) {
        popupMenuItems << menuItem
    }

}