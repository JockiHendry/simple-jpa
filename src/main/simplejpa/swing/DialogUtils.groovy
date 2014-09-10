package simplejpa.swing

import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import java.awt.Dialog
import java.awt.Window
import griffon.core.GriffonView
import griffon.core.GriffonApplication
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent

class DialogUtils {

    public static Closure defaultContentDecorator = null

    public static JDialog lastDialog = null

    static def showMVCGroup(String mvcGroupName, Map args, GriffonApplication app, GriffonView view,
            Map dialogProperties = null, Closure onFinish = null, Closure contentDecorator = null) {

        def result = null
        if (args == null) args = [:]
        app.withMVCGroup(mvcGroupName, args) { m, v, c ->
            Window parent = lastDialog ?: SwingUtilities.getWindowAncestor(view.mainPanel)
            JDialog dialog = new JDialog(parent, Dialog.ModalityType.APPLICATION_MODAL)
            lastDialog = dialog
            if (contentDecorator) {
                dialog.contentPane = contentDecorator(v.mainPanel)
            } else if (DialogUtils.defaultContentDecorator) {
                dialog.contentPane = DialogUtils.defaultContentDecorator.call(v.mainPanel)
            } else {
                dialog.contentPane = v.mainPanel
            }

            // Bind ESC to close dialog action
            dialog.getRootPane().registerKeyboardAction({ ActionEvent ae ->
                dialog.setVisible(false)
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW)

            dialogProperties?.each { prop, value ->
                dialog."$prop" = value
            }
            dialog.pack()
            dialog.setLocationRelativeTo(parent)
            dialog.setVisible(true)
            result = onFinish?.call(m, v, c)
        }
        lastDialog = null
        result

    }
}
