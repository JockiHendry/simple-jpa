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
<% out << fields.collect { field ->
    if (["BASIC_TYPE", "DATE"].contains(field.info)) {
        return "\t@Bindable ${field.type} ${field.name}"
    } else {
        return ""
    }
}.join("\n") %>

    @Bindable String ${fields[0].name}Search
    @Bindable String searchMessage

<%
    fields.collect { field ->
        if (field.info=="DOMAIN_CLASS") {
            out << "\tBasicEventList<${field.type}> ${field.name}List = new BasicEventList<>()\n"
            out << "\t@Bindable DefaultEventComboBoxModel<${field.type}> ${field.name} =\n"
            out << "\t\tGlazedListsSwing.eventComboBoxModelWithThreadProxyList(${field.name}List)\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
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
    out << fields.collect { field ->
        if (["BASIC_TYPE", "DATE"].contains(field.info)) {
            return "\t\t\t\t${field.name} = selected.${field.name}"
        } else if (field.info=="DOMAIN_CLASS") {
            return "\t\t\t\t${field.name}.selectedItem = selected.${field.name}"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            return "\t\t\t\t${field.name}.replaceSelectedValues(selected.${field.name})"
        } else {
            return ""
        }
    }.join("\n")
%>
            }
        }
    }

    def clear = {
        id = null
<% out << fields.collect { field ->
        if (["BASIC_TYPE", "DATE"].contains(field.info)) {
            return "\t\t${field.name} = null"
        } else if (field.info=="DOMAIN_CLASS") {
            return "\t\t${field.name}.selectedItem = null"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            return "\t\t${field.name}.clearSelectedValues()"
        }
   }.join("\n")
%>
        errors.clear()
        ${domainClassAsProp}Selection.clearSelection()
    }
}