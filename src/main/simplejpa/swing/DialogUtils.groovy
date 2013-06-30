package simplejpa.swing

import javax.swing.JDialog
import javax.swing.SwingUtilities
import java.awt.Dialog
import java.awt.Window
import griffon.core.GriffonView
import griffon.core.GriffonApplication

class DialogUtils {

    static showMVCGroup(String mvcGroupName, Map args, String dialogTitle, GriffonApplication app, GriffonView view,
            Closure onFinish = null) {
        app.withMVCGroup(mvcGroupName, args) { m, v, c ->
            Window thisWindow = SwingUtilities.getWindowAncestor(view.mainPanel)
            new JDialog(thisWindow, dialogTitle, Dialog.ModalityType.APPLICATION_MODAL).with {
                contentPane = v.mainPanel
                pack()
                setLocationRelativeTo(thisWindow)
                setVisible(true)
            }
            onFinish?.call(m, v, c)
        }
    }
}
