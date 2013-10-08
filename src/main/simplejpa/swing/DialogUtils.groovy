package simplejpa.swing

import javax.swing.JDialog
import javax.swing.SwingUtilities
import java.awt.Dialog
import java.awt.Window
import griffon.core.GriffonView
import griffon.core.GriffonApplication

class DialogUtils {

    static showMVCGroup(String mvcGroupName, Map args, GriffonApplication app, GriffonView view,
            Map dialogProperties = null, Closure onFinish = null, Closure contentDecorator = null) {
        app.withMVCGroup(mvcGroupName, args) { m, v, c ->
            Window thisWindow = SwingUtilities.getWindowAncestor(view.mainPanel)
            JDialog dialog = new JDialog(thisWindow, Dialog.ModalityType.APPLICATION_MODAL)
            if (contentDecorator) {
                dialog.contentPane = contentDecorator(v.mainPanel)
            } else {
                dialog.contentPane = v.mainPanel
            }
            dialog.pack()
            dialogProperties?.each { prop, value ->
                dialog."$prop" = value
            }
            dialog.setLocationRelativeTo(thisWindow)
            dialog.setVisible(true)

            onFinish?.call(m, v, c)
        }
    }
}
