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
        } else if (isOneToOne(field) && !isMappedBy(field)) {
            out << "\t@Bindable ${field.type} ${field.name}\n"
        } else if (isManyToOne(field) && !field.type.toString().equals(parentDomainClass)) {
            out << "\tBasicEventList<${field.type}> ${field.name}List = new BasicEventList<>()\n"
            out << "\t@Bindable DefaultEventComboBoxModel<${field.type}> ${field.name} =\n"
            out << "\t\tGlazedListsSwing.eventComboBoxModelWithThreadProxyList(${field.name}List)\n"
        } else if (isOneToMany(field)) {
            out << "\tList<${field.info}> ${field.name} = []\n"
        } else if (isManyToMany(field)) {
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
        if (isOneToOne(field) && isMappedBy(field)) return
        if (isManyToOne(field) && field.type.toString().equals(parentDomainClass)) return

        if (["BASIC_TYPE", "DATE"].contains(field.info)) {
            out << "\t\t\t\t${field.name} = selected.${field.name}\n"
        } else if (isOneToOne(field)) {
            out << "\t\t\t\t${field.name} = selected.${field.name}\n"
        } else if (isOneToMany(field)) {
            out << "\t\t\t\t${field.name}.clear()\n"
            out << "\t\t\t\t${field.name}.addAll(selected.${field.name})\n"
        } else if (isManyToMany(field)) {
            out << "\t\t\t\t${field.name}.replaceSelectedValues(selected.${field.name})\n"
        } else if (isManyToOne(field)) {
            out << "\t\t\t\t${field.name}.selectedItem = selected.${field.name}\n"
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
        if (isOneToOne(field) && isMappedBy(field)) return
        if (isManyToOne(field) && field.type.toString().equals(parentDomainClass)) return

        if (["BASIC_TYPE", "DATE"].contains(field.info)) {
            if (["Boolean", "boolean"].contains(field.type as String)) {
                out << "\t\t${field.name} = false\n"
            } else {
                out << "\t\t${field.name} = null\n"
            }
        } else if (isOneToOne(field)) {
            out << "\t\t${field.name} = null\n"
        } else if (isOneToMany(field)) {
            out << "\t\t${field.name}.clear()\n"
        } else if (isManyToMany(field)) {
            out << "\t\t${field.name}.clearSelectedValues()\n"
        } else if (isManyToOne(field)) {
            out << "\t\t${field.name}.selectedItem = null\n"
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