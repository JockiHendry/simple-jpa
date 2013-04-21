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
    fields.each { field ->
        if (field.info=="DOMAIN_CLASS" && !field.annotations?.containsAttribute('mappedBy')) {
            out << "\t\t\tmodel.${field.name}List.clear()\n"
        }
    }
%>        }

        List ${domainClassAsProp}Result = findAll${domainClass}()
<%
    fields.each { field ->
        if (field.info=="DOMAIN_CLASS" && field.annotations?.containsAttribute('mappedBy')) return

        if (field.info=="DOMAIN_CLASS") {
            out << "\t\tList ${field.name}Result = findAll${field.type}()\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN" && field.annotations?.get("OneToMany")==null) {
            out << "\t\tList ${field.name}Result = findAll${field.info}()\n"
        } else if (field.info=="UNKNOWN") {
            out << "\t\t// ${field.name} isn't supported! It must be coded manually!\n"
        }
    }  %>
        execInsideUIAsync {
            model.${domainClassAsProp}List.addAll(${domainClassAsProp}Result)
            model.searchMessage = app.getMessage("simplejpa.search.all.message")
<%
    fields.each { field ->
        if (field.info=="DOMAIN_CLASS" && field.annotations?.containsAttribute('mappedBy')) return

        if (field.info=="DOMAIN_CLASS") {
            out << "\t\t\tmodel.${field.name}List.addAll(${field.name}Result)\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN" && field.annotations?.get("OneToMany")==null) {
            out << "\t\t\tmodel.${field.name}.replaceValues(${field.name}Result)\n"
        }
    }
%>        }
    }

    def search = {
        if (model.${firstField}Search?.length() > 0) {
            execInsideUIAsync { model.${domainClassAsProp}List.clear() }
            List result = find${domainClass}By${cls(firstField)}(model.${firstField}Search)
            execInsideUIAsync {
                model.${domainClassAsProp}List.addAll(result)
                model.searchMessage = app.getMessage("simplejpa.search.result.message", ['${natural(firstField)}', model.${firstField}Search])
            }
        }
    }

    def save = {
        ${domainClass} ${domainClassAsProp} = new ${domainClass}(<%
    out << fields.collect { field ->
        if (field.info=="DOMAIN_CLASS" && field.annotations?.containsAttribute('mappedBy')) {
            return "'${field.name}': null"
        } else if (field.info=="DOMAIN_CLASS") {
            return "'${field.name}': model.${field.name}.selectedItem"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            if (field.annotations?.get("OneToMany")!=null) {
                return "'${field.name}': new ArrayList(model.${field.name})"
            } else {
                return "'${field.name}': model.${field.name}.selectedValues"
            }
        } else {
            return "'${field.name}': model.${field.name}"
        }
    }.join(", ")
%>)
        if (!validate(${domainClassAsProp})) return_failed()

        if (model.id == null) {
            // Insert operation
            if (find${domainClass}By${cls(firstField)}(${domainClassAsProp}.${firstField})?.size() > 0) {
                model.errors['${firstField}'] = app.getMessage("simplejpa.error.alreadyExist.message")
                return_failed()
            }
            persist(${domainClassAsProp})
            execInsideUIAsync { model.${domainClassAsProp}List << ${domainClassAsProp} }
        } else {
            // Update operation
            ${domainClass} selected${domainClass} = model.${domainClassAsProp}Selection.selected[0]
<%
    fields.each { field ->
        if (field.info=="DOMAIN_CLASS" && field.annotations?.containsAttribute('mappedBy')) return ''

        if (field.info=="DOMAIN_CLASS") {
            out << "\t\t\tselected${domainClass}.${field.name} = model.${field.name}.selectedItem\n"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            if (field.annotations?.get("OneToMany")!=null) {
                out << "\t\t\tselected${domainClass}.${field.name}.clear()\n"
                out << "\t\t\tselected${domainClass}.${field.name}.addAll(model.${field.name})\n"
            } else {
                out << "\t\t\tselected${domainClass}.${field.name}.clear()\n"
                out << "\t\t\tselected${domainClass}.${field.name}.addAll(model.${field.name}.selectedValues)\n"
            }
        } else {
            out << "\t\t\tselected${domainClass}.${field.name} = model.${field.name}\n"
        }
    }
%>
            model.${domainClassAsProp}Selection.selected[0] = merge(selected${domainClass})
        }
        execInsideUIAsync { model.clear() }
    }

    def delete = {
        ${domainClass} ${domainClassAsProp} = model.${domainClassAsProp}Selection.selected[0]
<% if (softDelete) {
        out << "\t\tsoftDelete${domainClass}(${domainClassAsProp}.id)\n"
   } else {
        out << "\t\tremove(${domainClassAsProp})"  } %>
        execInsideUIAsync { model.${domainClassAsProp}List.remove(${domainClassAsProp}) }
    }

}