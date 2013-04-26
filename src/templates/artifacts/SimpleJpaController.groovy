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
        if (isManyToOne(field)) {
            out << "\t\t\tmodel.${field.name}List.clear()\n"
        }
    }
%>        }

        List ${domainClassAsProp}Result = findAll${domainClass}()
<%
    fields.each { field ->
        if (isManyToOne(field)) {
            out << "\t\tList ${field.name}Result = findAll${field.type}()\n"
        } else if (isManyToMany(field)) {
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
        if (isManyToOne(field)) {
            out << "\t\t\tmodel.${field.name}List.addAll(${field.name}Result)\n"
        } else if (isManyToMany(field)) {
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
    out << fields.findAll{ !(isOneToOne(it) && isMappedBy(it)) }.collect { field ->
        if (isManyToOne(field)) {
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
<%
    def processOneToManyInSave(List fields, String currentClass, String currentAttribute = null, int numOfTab) {
        if (!currentAttribute) currentAttribute = currentClass
        def printTab = { n -> n.times { out << "\t" } }
        fields.findAll{ isOneToMany(it) }.each { field ->
            printTab(numOfTab)
            out << "${currentClass}.${field.name}.each { ${field.info} ${prop(field.info)} ->\n"
            printTab(numOfTab+1)
            out << "${prop(field.info)}.${currentAttribute} = ${currentClass}\n"
            processOneToManyInSave(getField(field.info), prop(field.info), numOfTab+1)
            printTab(numOfTab)
            out << "}\n"
        }
    }
    processOneToManyInSave(fields, domainClassAsProp, 2)
%>
        if (!validate(${domainClassAsProp})) return_failed()

        if (model.id == null) {
            // Insert operation
            if (find${domainClass}By${cls(firstField)}(${domainClassAsProp}.${firstField})?.size() > 0) {
                model.errors['${firstField}'] = app.getMessage("simplejpa.error.alreadyExist.message")
                return_failed()
            }
            ${domainClassAsProp} = merge(${domainClassAsProp})
            execInsideUIAsync { model.${domainClassAsProp}List << ${domainClassAsProp} }
        } else {
            // Update operation
            ${domainClass} selected${domainClass} = model.${domainClassAsProp}Selection.selected[0]
<%
    fields.each { field ->
        if (isManyToOne(field)) {
            out << "\t\t\tselected${domainClass}.${field.name} = model.${field.name}.selectedItem\n"
        } else if (isOneToMany(field)) {
            if (!isCascaded(field)) {
                out << "\t\t\t// You may need to add code here because it seems that you haven't included cascade=CascadeType.ALL and orphanRemoval=true in your domain class's field\n"
            }
            out << "\t\t\tselected${domainClass}.${field.name}.clear()\n"
            out << "\t\t\tselected${domainClass}.${field.name}.addAll(model.${field.name})\n"
            processOneToManyInSave(fields, "selected${domainClass}", domainClassAsProp, 3)
        } else if (isManyToMany(field)) {
            if (!field.annotations?.containsAttribute('cascade')) {
                out << "\t\t\t// You may need to add code here because it seems that you haven't included cascade=CascadeType.ALL\n"
            }
            out << "\t\t\tselected${domainClass}.${field.name}.clear()\n"
            out << "\t\t\tselected${domainClass}.${field.name}.addAll(model.${field.name}.selectedValues)\n"
        } else {
            if (isOneToOne(field) && !isCascaded(field)) {
                out << "\t\t\t// You may need to add code here because it seems that you haven't included cascade=CascadeType.ALL and orphanRemoval=true in your domain class's field\n"
            }
            out << "\t\t\tselected${domainClass}.${field.name} = model.${field.name}\n"

        }
    }
%>
            execInsideUIAsync { model.${domainClassAsProp}Selection.selected[0] = merge(selected${domainClass}) }
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