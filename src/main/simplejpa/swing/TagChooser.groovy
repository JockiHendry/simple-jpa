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

package simplejpa.swing

import groovy.swing.SwingBuilder
import org.jdesktop.swingx.JXPanel
import org.jdesktop.swingx.ScrollableSizeHint
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

class TagChooser extends JPanel {

    private static final Logger LOG = LoggerFactory.getLogger(TagChooser)

    TagChooserModel model
    def templateRenderer

    Closure selectedValueChanged  // will be called when model.selectedValues changed

    private static final ImageIcon addIcon, addRolloverIcon, removeIcon, removeRolloverIcon, addAllIcon, addAllRolloverIcon
    static {
        addIcon = new ImageIcon(TagChooser.getResource("/simplejpa/swing/icons/add.png"))
        addRolloverIcon = new ImageIcon(TagChooser.getResource("/simplejpa/swing/icons/add_rollover.png"))
        removeIcon = new ImageIcon(TagChooser.getResource("/simplejpa/swing/icons/remove.png"))
        removeRolloverIcon = new ImageIcon(TagChooser.getResource("/simplejpa/swing/icons/remove_rollover.png"))
        addAllIcon = new ImageIcon(TagChooser.getResource("/simplejpa/swing/icons/addall.png"))
        addAllRolloverIcon = new ImageIcon(TagChooser.getResource("/simplejpa/swing/icons/addall_rollover.png"))
    }

    private JComboBox cboInput
    private JButton btnAdd, btnAddAll
    private PanelSelectedItems panelSelectedItems
    private ComboBoxTemplateRenderer comboBoxTemplateRenderer

    public TagChooser() {

        setModel(new TagChooserModel())

        comboBoxTemplateRenderer = new ComboBoxTemplateRenderer()

        JPanel panelTop = new JPanel()
        panelTop.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0))

        cboInput = new JComboBox()
        cboInput.setPrototypeDisplayValue("This is a prototype display")
        def size = cboInput.preferredSize
        AutoCompleteDecorator.decorate(cboInput, new ObjectToStringConverter() {
            @Override
            String getPreferredStringForItem(Object o) {
                model.render(o)
            }
        })
        cboInput.preferredSize = size
        cboInput.actionPerformed = { ActionEvent e ->
            if (e.actionCommand=="comboBoxEdited") {
                addSelectedItem()
            }
        }
        panelTop.add(cboInput)

        JPanel pnlButtons = new JPanel()
        pnlButtons.setLayout(new FlowLayout(FlowLayout.LEADING))

        btnAdd = new JButton(addIcon)
        btnAdd.border = BorderFactory.createEmptyBorder()
        btnAdd.addFocusListener(new BorderFocusListener())
        btnAdd.rolloverIcon = addRolloverIcon
        btnAdd.contentAreaFilled = false
        btnAdd.actionPerformed = { addSelectedItem() }
        pnlButtons.add(btnAdd)

        btnAddAll = new JButton(addAllIcon)
        btnAddAll.border = BorderFactory.createEmptyBorder()
        btnAddAll.addFocusListener(new BorderFocusListener())
        btnAddAll.rolloverIcon = addAllRolloverIcon
        btnAddAll.contentAreaFilled = false
        btnAddAll.actionPerformed = { addAllItem() }
        pnlButtons.add(btnAddAll)

        panelTop.add(pnlButtons)

        panelSelectedItems = new PanelSelectedItems()

        setLayout(new BorderLayout())
        add(panelTop, BorderLayout.PAGE_START)
//        JScrollPane scrlPanelSelectedItems = new JScrollPane(panelSelectedItems)
//        int prefWidth = panelSelectedItems.preferredSize.width+scrlPanelSelectedItems.verticalScrollBar.preferredSize.width
//        int prefHeight = panelSelectedItems.preferredSize.height * 2
//        scrlPanelSelectedItems.setPreferredSize(new Dimension(prefWidth, prefHeight))
//        add(scrlPanelSelectedItems, BorderLayout.CENTER)
        add(panelSelectedItems, BorderLayout.CENTER)

    }

    private void addSelectedItem() {
        if (cboInput.selectedItem==null) return
        if (!model.allowMultiple) {
            if (model.selectedValues.contains(cboInput.selectedItem)) return
        }

        model.addSelectedValue(cboInput.selectedItem)
        panelSelectedItems.refresh()
    }

    private void addAllItem() {
        model.addAllValues()
        panelSelectedItems.refresh()
    }

    public void setModel(TagChooserModel model) {
        model.addPropertyChangeListener(new PropertyChangeListener() {

            SwingBuilder swing = new SwingBuilder()

            @Override
            void propertyChange(PropertyChangeEvent evt) {

                switch (evt.propertyName) {
                    case "values":
                        swing.edt {
                            cboInput.setModel(model.comboBoxModel)
                            model.comboBoxModel.selectedItem = null
                            cboInput.setRenderer(comboBoxTemplateRenderer)
                            cboInput.repaint()
                        }
                        break
                    case "selectedValues":
                        selectedValueChanged?.call(model.selectedValues)
                        panelSelectedItems.refresh()
                        break
                    case "templateString":
                        swing.edt { model.refreshTemplateValues() }
                        break
                }
            }
        })
        this.model = model
    }

    public void setTemplateRenderer(def templateRenderer) {
        model.setTemplateRenderer(templateRenderer)
    }

    class PanelSelectedItems extends JXPanel {

        public PanelSelectedItems() {
            setScrollableHeightHint(ScrollableSizeHint.PREFERRED_STRETCH)
            setLayout(new WrapLayout(WrapLayout.LEADING))
        }

        public void refresh() {
            removeAll()
            model.selectedValues.each { value ->
                add(new SelectedItem(value))
            }

            def tagChooserInstance = TagChooser.this
            griffon.core.UIThreadManager.instance.executeSync {
                tagChooserInstance.validate()
                repaint()
            }
        }
    }

    class SelectedItem extends JPanel {

        private JLabel lblData
        private JButton btnRemove
        private Color backgroundColor
        private Color hoverColor
        private Object data

        public SelectedItem(Object data) {

            setOpaque(true)

            setLayout(new FlowLayout())

            this.data = data
            lblData = new JLabel(model.render(data))
            backgroundColor = getBackground()
            hoverColor = getBackground().darker()
            add(lblData)

            btnRemove = new JButton(removeIcon)
            btnRemove.setRolloverIcon(removeRolloverIcon)
            btnRemove.setBorder(BorderFactory.createEmptyBorder())
            btnRemove.addFocusListener(new BorderFocusListener())
            btnRemove.setContentAreaFilled(false)
            btnRemove.actionPerformed = {
                model.removeSelectedValue(data)
                panelSelectedItems.refresh()
            }
            add(btnRemove)

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(hoverColor)
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(backgroundColor)
                }
            })

            setBorder(BorderFactory.createLineBorder(hoverColor))
        }

    }


    class ComboBoxTemplateRenderer extends JLabel implements ListCellRenderer {

        public ComboBoxTemplateRenderer() {
            setOpaque(true)
            setBorder(BorderFactory.createEmptyBorder(0,3,0,3))
        }

        @Override
        Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground())
                setForeground(list.getSelectionForeground())
            } else {
                setBackground(list.getBackground())
                setForeground(list.getForeground())
            }
            setText(model.render(value))
            return this
        }
    }

    class BorderFocusListener implements FocusListener {

        @Override
        void focusGained(FocusEvent e) {
            JComponent c = e.getComponent()
            c.setBorder(BorderFactory.createDashedBorder(Color.GRAY))
            c.repaint()
        }

        @Override
        void focusLost(FocusEvent e) {
            JComponent c = e.getComponent()
            c.setBorder(BorderFactory.createEmptyBorder())
            c.repaint()
        }
    }

}
