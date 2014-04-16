package simplejpa.swing

import javax.swing.JDialog
import javax.swing.SwingUtilities
import java.awt.Dialog
import java.awt.Window
import griffon.core.GriffonView
import griffon.core.GriffonApplication

class DialogUtils {

    public static Closure defaultContentDecorator = null

    static def showMVCGroup(String mvcGroupName, Map args, GriffonApplication app, GriffonView view,
            Map dialogProperties = null, Closure onFinish = null, Closure contentDecorator = null) {
        def result = null
        if (args == null) args = [:]
        app.withMVCGroup(mvcGroupName, args) { m, v, c ->
            Window thisWindow = SwingUtilities.getWindowAncestor(view.mainPanel)
            JDialog dialog = new JDialog(thisWindow, Dialog.ModalityType.APPLICATION_MODAL)
            if (contentDecorator) {
                dialog.contentPane = contentDecorator(v.mainPanel)
            } else if (DialogUtils.defaultContentDecorator) {
                dialog.contentPane = DialogUtils.defaultContentDecorator.call(v.mainPanel)
            } else {
                dialog.contentPane = v.mainPanel
            }
            dialog.pack()
            dialogProperties?.each { prop, value ->
                dialog."$prop" = value
            }
            dialog.setLocationRelativeTo(thisWindow)
            dialog.setVisible(true)

            result = onFinish?.call(m, v, c)
        }
        result
    }
}
