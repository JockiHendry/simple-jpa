package simplejpa.swing

import griffon.swing.SwingGriffonApplication
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import java.awt.Dialog
import java.awt.Window
import griffon.core.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent

class DialogUtils {

    public static Closure defaultContentDecorator = null

    /**
     *  This method will not destroy <code>MVCGroup</code> passed to it.
     */
    static def showMVCGroup(MVCGroup mvcGroup, GriffonApplication app, GriffonView view,
            Map dialogProperties = null, Closure onFinish = null, Closure contentDecorator = null) {

        def result

        JDialog dialog
        Window parent = SwingUtilities.getWindowAncestor(view.mainPanel)
        if (!parent) {
            parent = (app as SwingGriffonApplication).windowManager.getStartingWindow()
        }
        dialog = new JDialog(parent, Dialog.ModalityType.APPLICATION_MODAL)
        if (contentDecorator) {
            dialog.contentPane = contentDecorator(mvcGroup.view.mainPanel)
        } else if (DialogUtils.defaultContentDecorator) {
            dialog.contentPane = DialogUtils.defaultContentDecorator.call(mvcGroup.view.mainPanel)
        } else {
            dialog.contentPane = mvcGroup.view.mainPanel
        }
        if (mvcGroup.view.builder?.hasVariable('defaultButton') && mvcGroup.view.defaultButton) {
            dialog.getRootPane().defaultButton = mvcGroup.view.defaultButton
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

        result = onFinish?.call(mvcGroup.model, mvcGroup.view, mvcGroup.controller)

        dialog.dispose()
        dialog = null
        result

    }

    static def showMVCGroup(String mvcGroupName, Map args, GriffonApplication app, GriffonView view,
            Map dialogProperties = null, Closure onFinish = null, Closure contentDecorator = null) {

        if (args == null) args = [:]
        MVCGroup mvcGroup = app.buildMVCGroup(mvcGroupName, args)
        def result = showMVCGroup(mvcGroup, app, view, dialogProperties, onFinish, contentDecorator)

        mvcGroup.destroy()
        result

    }

    static def showAndReuseMVCGroup(String mvcGroupName, Map args, GriffonApplication app, GriffonView view,
            Map dialogProperties = null, Closure onFinish = null, Closure contentDecorator = null) {

        MVCGroup mvcGroup = app.mvcGroupManager.getAt(mvcGroupName)
        if (!mvcGroup) {
            mvcGroup = app.buildMVCGroup(mvcGroupName, mvcGroupName, args)
        }
        showMVCGroup(mvcGroup, app, view, dialogProperties, onFinish, contentDecorator)

    }

}
