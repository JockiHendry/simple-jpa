package $packageName

import ${domainPackage}.*
import simplejpa.transaction.SimpleJpaTransaction
import javax.swing.*

@SimpleJpaTransaction
class $className {

    def model
    def view

    void mvcGroupInit(Map args) {
        args.'parentList'.each { model.${domainClassAsProp}List << it }
        listAll()
    }

    def listAll = {
<%
    fields.each { field ->
        if (isManyToOne(field) && !isOwned(field)) {
            out << "\t\t\texecInsideUIAsync {model.${field.name}List.clear() }\n"
        }
    }

    fields.each { field ->
        if (isOwned(field)) return

        if (isManyToOne(field)) {
            out << "\t\tList ${field.name}Result = findAll${field.type}()\n"
        } else if (isManyToMany(field)) {
            out << "\t\tList ${field.name}Result = findAll${field.info}()\n"
        } else if (field.info=="UNKNOWN") {
            out << "\t\t// ${field.name} isn't supported! It must be coded manually!\n"
        }
    }

    fields.each { field ->
        if (isOwned(field)) return

        if (isManyToOne(field)) {
            out << "\t\t\texecInsideUIAsync{ model.${field.name}List.addAll(${field.name}Result) }\n"
        } else if (isManyToMany(field)) {
            out << "\t\t\texecInsideUIAsync{ model.${field.name}.replaceValues(${field.name}Result) }\n"
        }
    }
%>    }

    def save = {
        ${domainClass} ${domainClassAsProp} = new ${domainClass}(<%
    out << fields.collect { field ->
        if (isOwned(field)) {
            return "'${field.name}': null"
        } else if (isManyToOne(field)) {
            return "'${field.name}': model.${field.name}.selectedItem"
        } else if (isOneToMany(field)) {
            return "'${field.name}': new ArrayList(model.${field.name})"
        } else if (isManyToMany(field)) {
            return "'${field.name}': model.${field.name}.selectedValues"
        } else {
            return "'${field.name}': model.${field.name}"
        }
    }.join(", ")
%>)
        if (!validate(${domainClassAsProp})) return_failed()

        if (model.itemTransaksiSelection.selectionEmpty) {
            // Insert operation
            execInsideUIAsync { model.${domainClassAsProp}List << ${domainClassAsProp} }
        } else {
            // Update operation
            ${domainClass} selected${domainClass} = model.${domainClassAsProp}Selection.selected[0]
<%
    fields.each { field ->
        if (isOwned(field)) return

        if (isManyToOne(field)) {
            out << "\t\t\tselected${domainClass}.${field.name} = model.${field.name}.selectedItem\n"
        } else if (isOneToMany(field)) {
            out << "\t\t\tselected${domainClass}.${field.name}.clear()\n"
            out << "\t\t\tselected${domainClass}.${field.name}.addAll(model.${field.name})\n"
        } else if (isManyToMany(field)) {
            out << "\t\t\tselected${domainClass}.${field.name}.clear()\n"
            out << "\t\t\tselected${domainClass}.${field.name}.addAll(model.${field.name}.selectedValues)\n"
        } else {
            out << "\t\t\tselected${domainClass}.${field.name} = model.${field.name}\n"
        }
    }
%>
        }
        execInsideUIAsync { model.clear() }
    }

    def delete = {
        ${domainClass} ${domainClassAsProp} = model.${domainClassAsProp}Selection.selected[0]
        execInsideUIAsync { model.${domainClassAsProp}List.remove(${domainClassAsProp}) }
    }

}