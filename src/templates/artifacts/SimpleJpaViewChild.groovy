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

        panel(constraints: CENTER) {
            borderLayout()
            panel(constraints: PAGE_START, layout: new FlowLayout(FlowLayout.LEADING)) {
                label(text: bind('searchMessage', source: model))
            }
            scrollPane(constraints: CENTER) {
                table(rowSelectionAllowed: true, id: 'table') {
                    eventTableModel(list: model.${domainClassAsProp}List,
                        columnNames: [<%
    out << fields.collect { field ->
        "'${natural(field.name as String)}'"
    }.join(", ")
%>],
                        columnValues: [<%
    out << fields.collect { field ->
        "'\${value.${field.name}}'"
    }.join(", ")%>])
                    table.selectionModel = model.${domainClassAsProp}Selection
                }
            }
        }

        panel(id: "form", layout: new MigLayout('', '[right][left][left,grow]',''), constraints: PAGE_END, focusCycleRoot: true) {
<%
    fields.each { field ->
        if (field.annotations?.containsAttribute('mappedBy')) return

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
        } else if (field.info=="DOMAIN_CLASS") {
            out << "\t\t\tcomboBox(model: model.${field.name}, renderer: templateRenderer(template: '\${value}'), errorPath: '${field.name}')\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            if (field.annotations.containsAnnotation("OneToMany")) {
                out << "\t\t\tbutton(id: '${field.name}', text: '${natural(field.name as String)}')\n"
            } else {
                out << "\t\t\ttagChooser(model: model.${field.name}, templateString: '\${value}', constraints: 'grow,push,span,wrap', errorPath: '${field.name}')\n"
            }
        } else if (field.info=="UNKNOWN") {
            out << "\t\t\t// ${field.name} isn't supported by generator. It must be coded manually!\n"
            out << "\t\t\ttextField(id: '${field.name}', columns: 20, text: bind('${field.name}', target: model, mutual: true), errorPath: '${field.name}')\n"
        }

        if (field.type.toString()=="List" && field.info!="UNKNOWN" && !field.annotations.containsAnnotation("OneToMany")) {
            out << "\t\t\terrorLabel(path: '${field.name}', constraints: 'skip 1,grow,span,wrap')\n"
        } else {
            out << "\t\t\terrorLabel(path: '${field.name}', constraints: 'wrap')\n"
        }
    }
%>

            panel(constraints: 'span, growx, wrap') {
                flowLayout(alignment: FlowLayout.LEADING)
                button(app.getMessage("simplejpa.dialog.save.button"), actionPerformed: {
                    if (!model.itemTransaksiSelection.selectionEmpty) {
                        if (JOptionPane.showConfirmDialog(mainPanel, app.getMessage("simplejpa.dialog.update.message"),
                            app.getMessage("simplejpa.dialog.update.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                                return
                        }
                    }
                    controller.save()
                    form.getFocusTraversalPolicy().getFirstComponent(form).requestFocusInWindow()
                })
                button(app.getMessage("simplejpa.dialog.cancel.button"), visible: bind (source: model.${domainClassAsProp}Selection,
                    sourceEvent: 'valueChanged', sourceValue: {!model.${domainClassAsProp}Selection.selectionEmpty}),actionPerformed: model.clear)
                button(app.getMessage("simplejpa.dialog.delete.button"), visible: bind (source: model.${domainClassAsProp}Selection,
                    sourceEvent: 'valueChanged', sourceValue: {!model.${domainClassAsProp}Selection.selectionEmpty}), actionPerformed: {
                        if (JOptionPane.showConfirmDialog(mainPanel, app.getMessage("simplejpa.dialog.delete.message"),
                            app.getMessage("simplejpa.dialog.delete.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                                controller.delete()
                        }
                })
                button(app.getMessage("simplejpa.dialog.close.button"), actionPerformed: {
                    SwingUtilities.getWindowAncestor(mainPanel)?.dispose()
                })
            }
        }
    }
}
