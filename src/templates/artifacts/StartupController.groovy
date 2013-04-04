package $packageName

import javax.swing.*
import java.awt.*
import java.awt.event.*

class $className {

    def model
    def view

    def switchPage = { ActionEvent event ->
        edt { view.busyLabel.visible = true }
        String groupId = event.actionCommand
        def group = app.mvcGroupManager.findGroup(groupId)
        if (group==null) {
            group = app.mvcGroupManager.buildMVCGroup(groupId)
            edt { view.mainPanel.add(group.view.mainPanel, groupId) }
        }
        group.controller.listAll()
        edt {
            view.cardLayout.show(view.mainPanel, groupId)
            view.busyLabel.visible = false
        }
    }

}