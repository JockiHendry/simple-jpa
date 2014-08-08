package ${g.targetPackageName}

${g.imports()}
import simplejpa.transaction.Transaction
import javax.swing.*
import javax.swing.event.ListSelectionEvent

@Transaction
class ${g.domainClassName}Controller {

	${g.domainClassName}Model model
	def view

	void mvcGroupInit(Map args) {
		listAll()
	}

	void mvcGroupDestroy() {
		destroyEntityManager()
	}

	@Transaction(newSession = true)
	def listAll = {
		execInsideUISync {
${g.listAll_clear(3)}
		}

${g.listAll_find(2)}

		execInsideUISync {
${g.listAll_set(3)}
		}
	}

	@Transaction(newSession = true)
	def search = {
		if (model.${g.firstAttrSearch}?.length() > 0) {
			execInsideUISync { model.${g.domainClassGlazedListVariable}.clear() }
			List result = findAll${g.domainClassName}By${g.firstAttrAsCapitalized}Like("%\${model.${g.firstAttrSearch}}%")
			execInsideUISync {
				model.${g.domainClassGlazedListVariable}.addAll(result)
				model.searchMessage = app.getMessage("simplejpa.search.result.message", ['${g.firstAttrAsNatural}', model.${g.firstAttrSearch}])
			}
		}
	}

	def save = {
		${g.domainClassName} ${g.domainClassNameAsProperty} = ${g.domainClassConstructor()}

		if (!validate(${g.domainClassNameAsProperty})) return

		if (model.id == null) {
			// Insert operation
<%
    out << g.saveOneToManyInverse(g.domainClass,3)
    out << g.saveManyToManyInverse(g.domainClass,3)
%>
			if (find${g.domainClassName}By${g.firstAttrAsCapitalized}(${g.domainClassNameAsProperty}.${g.firstAttr})) {
				model.errors['${g.firstAttr}'] = app.getMessage("simplejpa.error.alreadyExist.message")
				return_failed()
			}
			persist(${g.domainClassNameAsProperty})
			execInsideUISync {
				model.${g.domainClassGlazedListVariable} << ${g.domainClassNameAsProperty}
				view.table.changeSelection(model.${g.domainClassGlazedListVariable}.size()-1, 0, false, false)
			}
		} else {
			// Update operation
			${g.domainClassName} selected${g.domainClassName} = view.table.selectionModel.selected[0]
${g.update(3)}
<%
    out << g.saveOneToManyInverse(g.domainClass, 3, "selected${g.domainClassName}")
    out << g.saveManyToManyInverse(g.domainClass, 3, "selected${g.domainClassName}")
%>
			selected${g.domainClassName} = merge(selected${g.domainClassName})
			execInsideUISync { view.table.selectionModel.selected[0] = selected${g.domainClassName} }
		}
		execInsideUISync { clear() }
	}

	def delete = {
		${g.domainClassName} ${g.domainClassNameAsProperty} = view.table.selectionModel.selected[0]
${g.delete(2)}
		execInsideUISync {
			model.${g.domainClassGlazedListVariable}.remove(${g.domainClassNameAsProperty})
			clear()
		}
	}

	@Transaction(Transaction.Policy.SKIP)
	def clear = {
		execInsideUISync {
			model.id = null
${g.clear(3)}
			model.errors.clear()
			view.table.selectionModel.clearSelection()
		}
	}

	@Transaction(Transaction.Policy.SKIP)
	def tableSelectionChanged = { ListSelectionEvent event ->
		execInsideUISync {
			if (view.table.selectionModel.isSelectionEmpty()) {
				clear()
			} else {
				${g.domainClassName} selected = view.table.selectionModel.selected[0]
				model.errors.clear()
				model.id = selected.id
${g.selected(4)}
			}
		}
	}

}