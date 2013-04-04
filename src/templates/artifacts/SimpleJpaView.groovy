package $packageName

import net.miginfocom.swing.MigLayout
import org.joda.time.*
import java.awt.*

application(title: '${GriffonUtil.getNaturalName(domainClass)}',
        preferredSize: [520, 340],
        pack: true,
        locationByPlatform: true,
        iconImage: imageIcon('/griffon-icon-48x48.png').image,
        iconImages: [imageIcon('/griffon-icon-48x48.png').image,
                imageIcon('/griffon-icon-32x32.png').image,
                imageIcon('/griffon-icon-16x16.png').image]) {

    panel(id: 'mainPanel') {
        borderLayout()

        panel(constraints: PAGE_START) {
            flowLayout(alignment: FlowLayout.LEADING)
            label("${GriffonUtil.getNaturalName(fields[0]?.name as String)}")
            textField(columns: 4, text: bind('${fields[0]?.name}Search', target: model))
            button(app.getMessage('simplejpa.search.label'), actionPerformed: controller.search)
            button(app.getMessage('simplejpa.search.all.label'), actionPerformed: controller.listAll)
        }

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
        "'${GriffonUtil.getNaturalName(field.name as String)}'"
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
        out << "\t\t\tlabel('${GriffonUtil.getNaturalName(field.name as String)}:')\n"
        if (field.info=="BASIC_TYPE" && ["Byte", "byte", "Short", "short", "Integer", "int", "Long", "long", "Float", "float", "Double", "double"].contains(field.type as String)) {
            out << "\t\t\ttextField(id: '${field.name}', columns: 20, text: bind('${field.name}', target: model, converter: toInteger('${field.name}'), reverseConverter: toReverseString(), mutual: true), errorPath: '${field.name}')\n"
        } else if (field.info=="BASIC_TYPE") {
            out << "\t\t\ttextField(id: '${field.name}', columns: 20, text: bind('${field.name}', target: model, mutual: true), errorPath: '${field.name}')\n"
        } else if (field.info=="DATE") {
            out << "\t\t\tjxdatePicker(id: '${field.name}', date: bind('${field.name}', target: model, converter: { it==null?null:new DateTime(it) },\n";
            out << "\t\t\t\treverseConverter: { it==null?null:((DateTime)it)?.toDate() }, mutual: true), errorPath: '${field.name}')\n"
        } else if (field.info=="DOMAIN_CLASS") {
            out << "\t\t\tcomboBox(model: model.${field.name}, renderer: templateRenderer(template: '\${value}'), errorPath: '${field.name}')\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            out << "\t\t\ttagChooser(model: model.${field.name}, templateString: '\${value}', constraints: 'grow,push,span,wrap', errorPath: '${field.name}')\n"
        }

        if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            out << "\t\t\terrorLabel(path: '${field.name}', constraints: 'skip 1,grow,span,wrap')\n"
        } else {
            out << "\t\t\terrorLabel(path: '${field.name}', constraints: 'wrap')\n"
        }
    }
%>

            panel(constraints: 'span, growx, wrap') {
                flowLayout(alignment: FlowLayout.LEADING)
                button(app.getMessage("simplejpa.dialog.save.button"), actionPerformed: {
                    controller.save()
                    form.getFocusTraversalPolicy().getFirstComponent(form).requestFocusInWindow()
                })
                button(app.getMessage("simplejpa.dialog.cancel.button"), visible: bind (source: model.${domainClassAsProp}Selection,
                        sourceEvent: 'valueChanged', sourceValue: {!model.${domainClassAsProp}Selection.selectionEmpty}),
                        actionPerformed: model.clear)
                button(app.getMessage("simplejpa.dialog.delete.button"), visible: bind (source: model.${domainClassAsProp}Selection,
                        sourceEvent: 'valueChanged', sourceValue: {!model.${domainClassAsProp}Selection.selectionEmpty}),
                        actionPerformed: controller.delete)
            }
        }
    }
}
