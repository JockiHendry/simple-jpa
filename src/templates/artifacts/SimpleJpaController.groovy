package $packageName

import ${domainPackage}.*
import simplejpa.transaction.SimpleJpaTransaction
import javax.swing.*

@SimpleJpaTransaction
class $className {

    def model
    def view

    void mvcGroupInit(Map args) {
        listAll()
    }

    def listAll = {
        execInsideUIAsync {
            model.${domainClassAsProp}List.clear()
<%
    fields.collect { field ->
        if (field.info=="DOMAIN_CLASS") {
            out << "\t\t\tmodel.${field.name}List.clear()\n"
        }
    }
%>
        }
        List ${domainClassAsProp}Result = findAll${domainClass}()
<%
    fields.collect { field ->
        if (field.info=="DOMAIN_CLASS") {
            out << "\t\tList ${field.name}Result = findAll${field.type}()\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            out << "\t\tList ${field.name}Result = findAll${field.info}()\n"
        } else if (field.info=="UNKNOWN") {
            out << "\t\t// ${field.name} isn't supported! It must be coded manually!\n"
        }
    }
%>
        execInsideUIAsync {
            model.${domainClassAsProp}List.addAll(${domainClassAsProp}Result)
            model.searchMessage = app.getMessage("simplejpa.search.all.message")
<%
    fields.collect { field ->
        if (field.info=="DOMAIN_CLASS") {
            out << "\t\t\tmodel.${field.name}List.addAll(${field.name}Result)\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            out << "\t\t\tmodel.${field.name}.replaceValues(${field.name}Result)\n"
        }
    }
%>
        }
    }

    def search = {
        if (model.${firstField}Search?.length() > 0) {
            execInsideUIAsync { model.${domainClassAsProp}List.clear() }
            List result = find${domainClass}By${firstFieldUppercase}(model.${firstField}Search)
            execInsideUIAsync {
                model.${domainClassAsProp}List.addAll(result)
                model.searchMessage = app.getMessage("simplejpa.search.result.message", ['${firstFieldNatural}', model.${firstField}Search])
            }
        }
    }

    def save = {
        ${domainClass} ${domainClassAsProp} = new ${domainClass}(<%
    out << fields.collect { field ->
        if (field.info=="DOMAIN_CLASS") {
            return "model.${field.name}.selectedItem"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            return "model.${field.name}.selectedValues"
        } else {
            return "model.${field.name}"
        }
    }.join(", ")
%>)
        if (!validate(${domainClassAsProp})) return_failed()

        if (model.id == null) {
            // Insert operation
            if (find${domainClass}By${firstFieldUppercase}(${domainClassAsProp}.${firstField})?.size() > 0) {
                model.errors['${firstField}'] = app.getMessage("simplejpa.error.alreadyExist.message")
                return_failed()
            }
            persist(${domainClassAsProp})
            execInsideUIAsync { model.${domainClassAsProp}List << ${domainClassAsProp} }
        } else {
            // Update operation
            ${domainClass} selected${domainClass} = model.${domainClassAsProp}Selection.selected[0]
<%
    out << fields.collect { field ->
        if (field.info=="DOMAIN_CLASS") {
            return "\t\t\tselected${domainClass}.${field.name} = model.${field.name}.selectedItem"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            return "\t\t\tselected${domainClass}.${field.name}.clear()\n" +
                   "\t\t\tselected${domainClass}.${field.name}.addAll(model.${field.name}.selectedValues)"
        } else {
            return "\t\t\tselected${domainClass}.${field.name} = model.${field.name}"
        }
    }.join("\n")
%>
            merge(selected${domainClass})
        }
        execInsideUIAsync { model.clear() }
    }

    def delete = {
        ${domainClass} ${domainClassAsProp} = model.${domainClassAsProp}Selection.selected[0]

<% if (softDelete) {
        out << "\t\tsoftDelete${domainClass}(${domainClassAsProp}.id)\n"
   } else {
        out << "\t\tdef ${domainClassAsProp}Persist = merge(${domainClassAsProp})\n"
        out << "\t\tremove(${domainClassAsProp}Persist)"  } %>
        execInsideUIAsync { model.${domainClassAsProp}List.remove(${domainClassAsProp}) }
    }

}