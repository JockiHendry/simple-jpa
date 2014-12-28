package ${g.targetPackageName}

import javax.swing.*
import java.awt.*
import java.awt.event.*
import griffon.util.GriffonNameUtils

class ${g.scaffolding.startupGroupName}Controller {

	def model
	def view

	def switchPage = { ActionEvent event, Map arguments = [:] ->
		execInsideUISync {
			view.busyLabel.visible = true
			def groupId = event.actionCommand
			def caption = event.source.text
			view.mainTab.addMVCTab(groupId, arguments, caption)
			view.busyLabel.visible = false
		}
	}

}