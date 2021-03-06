package ${g.targetPackageName}

${g.imports()}
import ${g.targetPackageName}.*
import simplejpa.swing.DialogUtils
import simplejpa.transaction.Transaction
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.validation.groups.Default

class ${g.customClassName}Controller {

	${g.customClassName}Model model
	def view
    ${g.repositoryType} ${g.repositoryVar}

	void mvcGroupInit(Map args) {
        model.${g.domainClassGlazedListVariable}.addAll(args.'parentList'?:[])
		listAll()
	}

	void mvcGroupDestroy() {
	}

	def listAll = {
<%
    out << g.sub_listAll_clear(2)
    out << g.sub_listAll_find(2)
    out << g.sub_listAll_set(2)
%>    }

	def save = {
        if (!view.table.selectionModel.selectionEmpty &&
            !DialogUtils.confirm(view.mainPanel, app.getMessage("simplejpa.dialog.update.message"), app.getMessage("simplejpa.dialog.update.title"), JOptionPane.WARNING_MESSAGE)) {
                return
        }

		${g.domainClassName} ${g.domainClassNameAsProperty} = ${g.domainClassConstructor()}
<%
out << g.saveOneToManyInverse(g.domainClass,2)
out << g.saveManyToManyInverse(g.domainClass,2)
%>
		if (!${g.repositoryVar}.validate(${g.domainClassNameAsProperty}, Default, model)) return

		if (view.table.selectionModel.selectionEmpty) {
			// Insert operation
			execInsideUISync {
				model.${g.domainClassGlazedListVariable} << ${g.domainClassNameAsProperty}
				view.table.changeSelection(model.${g.domainClassGlazedListVariable}.size()-1, 0, false, false)
			}
		} else {
			// Update operation
			${g.domainClassName} selected${g.domainClassName} = view.table.selectionModel.selected[0]
${g.update(3)}
		}
		execInsideUISync {
            clear()
            view.form.getFocusTraversalPolicy().getFirstComponent(view.form).requestFocusInWindow()
        }
	}

	def delete = {
        if (!DialogUtils.confirm(view.mainPanel, app.getMessage("simplejpa.dialog.delete.message"), app.getMessage("simplejpa.dialog.delete.title"), JOptionPane.WARNING_MESSAGE)) {
            return
        }
		${g.domainClassName} ${g.domainClassNameAsProperty} = view.table.selectionModel.selected[0]
		execInsideUISync {
			model.${g.domainClassGlazedListVariable}.remove(${g.domainClassNameAsProperty})
			clear()
		}
	}
${g.popups(1)}
	def clear = {
		execInsideUISync {
			model.id = null
${g.clear(3, false)}
			model.errors.clear()
			view.table.selectionModel.clearSelection()
		}
	}

	def tableSelectionChanged = { ListSelectionEvent event ->
		execInsideUISync {
			if (view.table.selectionModel.isSelectionEmpty()) {
				clear()
			} else {
				${g.domainClassName} selected = view.table.selectionModel.selected[0]
				model.errors.clear()
				${g.hasId()? "model.id = selected.id": ''}
${g.selected(4, false)}
			}
		}
	}

    def close = {
    	execInsideUISync {
        	SwingUtilities.getWindowAncestor(view.mainPanel)?.dispose()
		}
    }

}