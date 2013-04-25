package $packageName

import net.miginfocom.swing.MigLayout
import org.joda.time.*
import java.awt.*

application(title: '${natural(domainClass)}',
        preferredSize: [520, 340],
        pack: true,
        locationByPlatform: true,
        iconImage: imageIcon('/griffon-icon-48x48.png').image,
        iconImages: [imageIcon('/griffon-icon-48x48.png').image,
                imageIcon('/griffon-icon-32x32.png').image,
                imageIcon('/griffon-icon-16x16.png').image]) {

    panel(id: 'mainPanel') {
        borderLayout()

        panel(id: "form", layout: new MigLayout('', '[right][left][left,grow]',''), constraints: CENTER, focusCycleRoot: true) {
<%
    fields.findAll{ !(isOneToOne(it) && isMappedBy(it)) &&
                    !(isManyToOne(it) && it.type.toString().equals(parentDomainClass)) }.each { field ->
        out << "\t\t\tlabel('${natural(field.name as String)}:')\n"
        if (field.info=="BASIC_TYPE" && ["Byte", "byte", "Short", "short", "Integer", "int", "Long", "long", "Float", "float", "Double", "double", "BigInteger"].contains(field.type as String)) {
            out << "\t\t\tnumberTextField(id: '${field.name}', columns: 20, bindTo: '${field.name}', errorPath: '${field.name}')\n"
        } else if (field.info=="BASIC_TYPE" && ["BigDecimal"].contains(field.type as String)) {
            out << "\t\t\tnumberTextField(id: '${field.name}', columns: 20, bindTo: '${field.name}', nfParseBigDecimal: true, errorPath: '${field.name}')\n"
        } else if (field.info=="BASIC_TYPE") {
            out << "\t\t\ttextField(id: '${field.name}', columns: 20, text: bind('${field.name}', target: model, mutual: true), errorPath: '${field.name}')\n"
        } else if (field.info=="DATE") {
            out << "\t\t\tdateTimePicker(id: '${field.name}', ${prop(field.type.toString())}: bind('${field.name}', target: model, mutual: true), errorPath: '${field.name}'"
            if (field.type.toString().equals("LocalDate")) out << ", dateVisible: true, timeVisible: false"
            if (field.type.toString().equals("LocalTime")) out << ", dateVisible: false, timeVisible: true"
            out << ")\n"
        } else if (isManyToOne(field)) {
            out << "\t\t\tcomboBox(model: model.${field.name}, renderer: templateRenderer(template: '\${value}'), errorPath: '${field.name}')\n"
        } else if (isOneToOne(field)) {
            out << "\t\t\tbutton(id: '${field.name}', text: '${natural(field.name as String)}', errorPath: '${field.name}', actionPerformed: {\n"
            out << """\
                app.withMVCGroup("${prop(field.type as String)}AsPair", [pair: model.${field.name}]) { m, v, c ->
                    Window thisWindow = SwingUtilities.getWindowAncestor(mainPanel)
                    new JDialog(thisWindow, "${natural(field.name as String)}", Dialog.ModalityType.DOCUMENT_MODAL).with {
                        contentPane = v.mainPanel
                        pack()
                        setLocationRelativeTo(thisWindow)
                        setVisible(true)
                        model.${field.name} = m.${field.name}
                    }
                }
            })
"""
        } else if (isOneToMany(field)) {
            out << "\t\t\tbutton(id: '${field.name}', text: '${natural(field.name as String)}', errorPath: '${field.name}', actionPerformed: {\n"
            out << """\
                app.withMVCGroup("${prop(field.info)}AsChild", [parentList: model.${field.name}]) { m, v, c ->
                    Window thisWindow = SwingUtilities.getWindowAncestor(mainPanel)
                    new JDialog(thisWindow, "${natural(field.name as String)}", Dialog.ModalityType.DOCUMENT_MODAL).with {
                        contentPane = v.mainPanel
                        pack()
                        setLocationRelativeTo(thisWindow)
                        setVisible(true)

                        model.${field.name}.clear()
                        model.${field.name}.addAll(m.${prop(field.info)}List)
                    }
                }
            })
"""
        } else if (isManyToMany(field)) {
            out << "\t\t\ttagChooser(model: model.${field.name}, templateString: '\${value}', constraints: 'grow,push,span,wrap', errorPath: '${field.name}')\n"
        } else if (field.info=="UNKNOWN") {
            out << "\t\t\t// ${field.name} isn't supported by generator. It must be coded manually!\n"
            out << "\t\t\ttextField(id: '${field.name}', columns: 20, text: bind('${field.name}', target: model, mutual: true), errorPath: '${field.name}')\n"
        }

        if (isManyToMany(field)) {
            out << "\t\t\terrorLabel(path: '${field.name}', constraints: 'skip 1,grow,span,wrap')\n"
        } else {
            out << "\t\t\terrorLabel(path: '${field.name}', constraints: 'wrap')\n"
        }
    }
%>

            panel(constraints: 'span, growx, wrap') {
                flowLayout(alignment: FlowLayout.LEADING)
                button(app.getMessage("simplejpa.dialog.update.button"), actionPerformed: {
                    if (model.${domainClassAsProp}!=null) {
                        if (JOptionPane.showConfirmDialog(mainPanel, app.getMessage("simplejpa.dialog.update.message"),
                            app.getMessage("simplejpa.dialog.update.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                                return
                        }
                    }
                    controller.save()
                })
                button(app.getMessage("simplejpa.dialog.delete.button"), visible: bind {model.${domainClassAsProp}!=null},
                    actionPerformed: {
                        if (JOptionPane.showConfirmDialog(mainPanel, app.getMessage("simplejpa.dialog.delete.message"),
                            app.getMessage("simplejpa.dialog.delete.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                                controller.delete()
                        }
                })
                button(app.getMessage("simplejpa.dialog.close.button"), actionPerformed: { controller.close() })
            }
        }
    }
}
