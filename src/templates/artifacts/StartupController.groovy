package $packageName

import javax.swing.*
import java.awt.*
import java.awt.event.*

class $className {

    def model
    def view
    def group

    def switchPage = { ActionEvent event ->
        group?.destroy()
        execInsideUISync { view.busyLabel.visible = true }

        String groupId = event.actionCommand
        group = app.mvcGroupManager.buildMVCGroup(groupId)
        execInsideUISync { view.mainPanel.add(group.view.mainPanel, groupId) }
        group.controller.listAll()

        execInsideUIAsync {
            view.cardLayout.show(view.mainPanel, groupId)
            view.busyLabel.visible = false
        }
    }

}