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
        if (field.info=="DOMAIN_CLASS" && !field.annotations?.containsAttribute('mappedBy')) {
            out << "\t\t\texecInsideUIAsync {model.${field.name}List.clear() }\n"
        }
    }

    fields.each { field ->
        if (field.info=="DOMAIN_CLASS" && field.annotations?.containsAttribute('mappedBy')) return

        if (field.info=="DOMAIN_CLASS") {
            out << "\t\tList ${field.name}Result = findAll${field.type}()\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN" && field.annotations?.containsAttribute('mappedBy')) {
            out << "\t\tList ${field.name}Result = findAll${field.info}()\n"
        } else if (field.info=="UNKNOWN") {
            out << "\t\t// ${field.name} isn't supported! It must be coded manually!\n"
        }
    }

    fields.each { field ->
        if (field.info=="DOMAIN_CLASS" && field.annotations?.containsAttribute('mappedBy')) return

        if (field.info=="DOMAIN_CLASS") {
            out << "\t\t\texecInsideUIAsync{ model.${field.name}List.addAll(${field.name}Result) }\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN" && field.annotations?.containsAttribute('mappedBy') && field.annotations?.get("OneToMany")==null) {
            out << "\t\t\texecInsideUIAsync{ model.${field.name}.replaceValues(${field.name}Result) }\n"
        }
    }
%>    }

    def save = {
        ${domainClass} ${domainClassAsProp} = new ${domainClass}(<%
    out << fields.collect { field ->
        if (field.info=="DOMAIN_CLASS" && field.annotations?.containsAttribute('mappedBy')) {
            return "'${field.name}': null"
        } else if (field.info=="DOMAIN_CLASS") {
            return "'${field.name}': model.${field.name}.selectedItem"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN" && field.annotations?.get("OneToMany")==null) {
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
        if (field.info=="DOMAIN_CLASS" && field.annotations?.containsAttribute('mappedBy')) return ''

        if (field.info=="DOMAIN_CLASS") {
            out << "\t\t\tselected${domainClass}.${field.name} = model.${field.name}.selectedItem\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN" && field.annotations?.get("OneToMany")==null) {
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