package $packageName

import ${domainPackage}.*
import simplejpa.transaction.SimpleJpaTransaction
import javax.swing.*

@SimpleJpaTransaction
class $className {

    def model
    def view

    void mvcGroupInit(Map args) {
        model.${domainClassAsProp} = args.'pair'
<%
        fields.each { field ->
            if (isOneToOne(field) && isMappedBy(field)) return

            if (["BASIC_TYPE", "DATE"].contains(field.info)) {
                out << "\t\tmodel.${field.name} = args.'pair'?.${field.name}\n"
            } else if (isOneToOne(field)) {
                out << "\t\tmodel.${field.name} = args.'pair'?.${field.name}\n"
            } else if (isManyToOne(field)) {
                out << "\t\tmodel.${field.name}.selectedItem = args.'pair'?.${field.name}\n"
            } else if (isOneToMany(field)) {
                out << "\t\tmodel.${field.name}.clear()\n"
                out << "\t\tmodel.${field.name}.addAll(args.'pair'?.${field.name})\n"
            } else if (isManyToMany(field)) {
                out << "\t\tmodel.${field.name}.replaceSelectedValues(args.'pair'?.${field.name})\n"
            } else if (field.info=="UNKNOWN") {
                out << "\t\t// ${field.name} is not supported by generator.  You will need to code it manually.\n"
                out << "\t\tmodel.${field.name} = args.'pair'?.${field.name}\n"
            }
        }
%>
        listAll()
    }

    def listAll = {
<%
    fields.each { field ->
        if ((isManyToOne(field))) {
            out << "\t\t\texecInsideUIAsync {model.${field.name}List.clear() }\n"
        }
    }

    fields.each { field ->
        if (isManyToOne(field)) {
            out << "\t\tList ${field.name}Result = findAll${field.type}()\n"
        } else if (isManyToMany(field)) {
            out << "\t\tList ${field.name}Result = findAll${field.info}()\n"
        } else if (field.info=="UNKNOWN") {
            out << "\t\t// ${field.name} isn't supported! It must be coded manually!\n"
        }
    }

    fields.each { field ->
        if (isManyToOne(field)) {
            out << "\t\t\texecInsideUIAsync{ model.${field.name}List.addAll(${field.name}Result) }\n"
        } else if (isManyToMany(field)) {
            out << "\t\t\texecInsideUIAsync{ model.${field.name}.replaceValues(${field.name}Result) }\n"
        }
    }
%>    }

    def close = {
        execInsideUIAsync { SwingUtilities.getWindowAncestor(view.mainPanel)?.dispose() }
    }

    def save = {
        ${domainClass} ${domainClassAsProp} = new ${domainClass}(<%
    out << fields.findAll{ !(isOneToOne(it) && isMappedBy(it)) }.collect { field ->
        if (isOneToOne(field)) {
            return "'${field.name}': model.${field.name}"
        } else if (isManyToOne(field)) {
            return "'${field.name}': model.${field.name}.selectedItem"
        } else if (isManyToMany(field)) {
            return "'${field.name}': model.${field.name}.selectedValues"
        } else {
            return "'${field.name}': model.${field.name}"
        }
    }.join(", ")
%>)
        if (!validate(${domainClassAsProp})) return_failed()

        if (model.${domainClassAsProp}==null) {
            // Insert operation
            model.${domainClassAsProp}= ${domainClassAsProp}
        } else {
            // Update operation
<%
    fields.each { field ->

        if (field.info=="DOMAIN_CLASS" && field.annotations?.containsAttribute('mappedBy')) return ''

        if (field.type.toString()=="List" && field.info!="UNKNOWN" && field.annotations?.get("OneToMany")==null) {
            out << "\t\t\tmodel.${domainClassAsProp}.${field.name}.clear()\n"
            out << "\t\t\tmodel.${domainClassAsProp}.${field.name}.addAll(${domainClassAsProp}.${field.name})\n"
        } else {
            out << "\t\t\tmodel.${domainClassAsProp}.${field.name} = ${domainClassAsProp}.${field.name}\n"
        }
    }
%>}
        close()
    }

    def delete = {
        model.${domainClassAsProp} = null
        close()
    }

}