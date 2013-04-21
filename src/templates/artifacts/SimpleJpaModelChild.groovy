package $packageName

import ${domainPackage}.*
import ca.odell.glazedlists.*
import ca.odell.glazedlists.swing.*
import groovy.beans.Bindable
import org.joda.time.*
import javax.swing.event.*
import simplejpa.swing.*

class $className {

    @Bindable Long id
<%
    fields.each { field ->
        if (["BASIC_TYPE", "DATE"].contains(field.info)) {
            out << "\t@Bindable ${field.type} ${field.name}\n"
        } else if ("UNKNOWN".equals(field.info)){
            out << "\t// ${field.name} is not supported by generator.  You will need to code it manually.\n"
            out << "\t@Bindable ${field.type} ${field.name}\n"
        } else if (field.info=="DOMAIN_CLASS" && !field.annotations?.containsAttribute('mappedBy')) {
            out << "\tBasicEventList<${field.type}> ${field.name}List = new BasicEventList<>()\n"
            out << "\t@Bindable DefaultEventComboBoxModel<${field.type}> ${field.name} =\n"
            out << "\t\tGlazedListsSwing.eventComboBoxModelWithThreadProxyList(${field.name}List)\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN" && field.annotations?.get("OneToMany")==null) {
            out << "\tTagChooserModel ${field.name} = new TagChooserModel()\n"
        }
    }
%>
    BasicEventList<${domainClass}> ${domainClassAsProp}List = new BasicEventList<>()
    DefaultEventSelectionModel<${domainClass}> ${domainClassAsProp}Selection =
        GlazedListsSwing.eventSelectionModelWithThreadProxyList(${domainClassAsProp}List)

    public ${className}() {
        ${domainClassAsProp}Selection.valueChanged = { ListSelectionEvent event ->
            if (${domainClassAsProp}Selection.isSelectionEmpty()) {
                clear()
            } else {

                ${domainClass} selected = ${domainClassAsProp}Selection.selected[0]
                errors.clear()
                id = selected.id
<%
    fields.each { field ->
        if (["BASIC_TYPE", "DATE"].contains(field.info)) {
            out << "\t\t\t\t${field.name} = selected.${field.name}\n"
        } else if (field.info=="DOMAIN_CLASS" && !field.annotations?.containsAttribute('mappedBy')) {
            out << "\t\t\t\t${field.name}.selectedItem = selected.${field.name}\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN" && field.annotations?.get("OneToMany")==null) {
            out << "\t\t\t\t${field.name}.replaceSelectedValues(selected.${field.name})\n"
        } else if (field.info=="UNKNOWN") {
            out << "\t\t\t\t// ${field.name} is not supported by generator.  You will need to code it manually.\n"
            out << "\t\t\t\t${field.name} = selected.${field.name}\n"
        }
    }
%>
            }
        }
    }

    def clear = {
        id = null
<% fields.each { field ->
        if (["BASIC_TYPE", "DATE"].contains(field.info)) {
            out << "\t\t${field.name} = null\n"
        } else if (field.info=="DOMAIN_CLASS" && !field.annotations?.containsAttribute('mappedBy')) {
            out << "\t\t${field.name}.selectedItem = null\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN" && field.annotations?.get("OneToMany")==null) {
            out << "\t\t${field.name}.clearSelectedValues()\n"
        } else if (field.info=="UNKNOWN") {
            out << "\t\t// ${field.name} is not supported by generator.  You will need to code it manually.\n"
            out << "\t\t${field.name} = null\n"
        }
   }
%>
        errors.clear()
        ${domainClassAsProp}Selection.clearSelection()
    }
}