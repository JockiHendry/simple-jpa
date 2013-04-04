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
        edt {
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
        }
    }
%>
        edt {
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
            edt { model.${domainClassAsProp}List.clear() }
            List result = find${domainClass}By${firstFieldUppercase}(model.${firstField}Search)
            edt {
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
            edt { model.${domainClassAsProp}List << ${domainClassAsProp} }
        } else {
            // Update operation
            if (JOptionPane.showConfirmDialog(view.mainPanel, app.getMessage("simplejpa.dialog.update.message"),
                    app.getMessage("simplejpa.dialog.update.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                ${domainClass} selected${domainClass} = model.${domainClassAsProp}Selection.selected[0]
<%
    out << fields.collect { field ->
        if (field.info=="DOMAIN_CLASS") {
            return "\t\t\t\tselected${domainClass}.${field.name} = model.${field.name}.selectedItem"
        } else if (field.type.toString()=="List" && field.info!="UNKNOWN") {
            return "\t\t\t\tselected${domainClass}.${field.name}.clear()\n" +
                   "\t\t\t\tselected${domainClass}.${field.name}.addAll(model.${field.name}.selectedValues)"
        } else {
            return "\t\t\t\tselected${domainClass}.${field.name} = model.${field.name}"
        }
    }.join("\n")
%>
                merge(selected${domainClass})
            }
        }
        edt { model.clear() }
    }

    def delete = {
        ${domainClass} ${domainClassAsProp} = model.${domainClassAsProp}Selection.selected[0]

        if (JOptionPane.showConfirmDialog(view.mainPanel, app.getMessage("simplejpa.dialog.delete.message"),
            app.getMessage("simplejpa.dialog.delete.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
            == JOptionPane.YES_OPTION) {
<% if (softDelete) {
        out << "\t\t\t\tsoftDelete${domainClass}(${domainClassAsProp}.id)\n"
   } else {
        out << "\t\t\t\tdef ${domainClassAsProp}Persist = merge(${domainClassAsProp})\n"
        out << "\t\t\t\tremove(${domainClassAsProp}Persist)"  } %>
                edt { model.${domainClassAsProp}List.remove(${domainClassAsProp}) }
        }
    }

}