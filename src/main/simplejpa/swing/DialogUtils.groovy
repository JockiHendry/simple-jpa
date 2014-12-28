/*
 * Copyright 2014 Jocki Hendry.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package simplejpa.swing

import griffon.swing.SwingGriffonApplication
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JLayer
import javax.swing.JOptionPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.plaf.LayerUI
import java.awt.Component
import java.awt.Dialog
import java.awt.Window
import griffon.core.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import griffon.util.*

class DialogUtils {

    public static LayerUI defaultLayerUI = null

    /**
     *  This method will not destroy <code>MVCGroup</code> passed to it.
     */
    static def showMVCGroup(MVCGroup mvcGroup, GriffonView view, Map dialogProperties = [:], LayerUI layerUI = null, Closure onFinish = null) {
        SwingGriffonApplication app = ApplicationHolder.application
        def result = null
        app.execInsideUISync {
            Window parent = SwingUtilities.getWindowAncestor(view.mainPanel)
            if (!parent) {
                parent = app.windowManager.getStartingWindow()
            }
            JDialog dialog = new JDialog(parent, Dialog.ModalityType.APPLICATION_MODAL)
            JLayer layer
            if (layerUI) {
                layer = new JLayer(mvcGroup.view.mainPanel, layerUI)
                dialog.contentPane = layer
            } else if (defaultLayerUI) {
                layer = new JLayer(mvcGroup.view.mainPanel, defaultLayerUI)
                dialog.contentPane = layer
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

            dialogProperties.each { prop, value ->
                dialog."$prop" = value
            }
            dialog.pack()
            dialog.setLocationRelativeTo(parent)
            dialog.setVisible(true)

            result = onFinish?.call(mvcGroup.model, mvcGroup.view, mvcGroup.controller)

            if (layer) {
                layer.getUI().uninstallUI(layer)
            }
            dialog.dispose()
            dialog = null
        }
        result
    }

    static def showMVCGroup(String mvcGroupName, Map args = [:], GriffonView view, Map dialogProperties = [:], LayerUI layerUI, Closure onFinish = null) {
        MVCGroup mvcGroup = ApplicationHolder.application.buildMVCGroup(mvcGroupName, args)
        def result = showMVCGroup(mvcGroup, view, dialogProperties, layerUI, onFinish)
        mvcGroup.destroy()
        result
    }

    static def showMVCGroup(String mvcGroupName, Map args = [:], GriffonView view, Map dialogProperties = [:], Closure onFinish = null) {
        showMVCGroup(mvcGroupName, args, view, dialogProperties, null, onFinish)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    static def showAndReuseMVCGroup(String mvcGroupName, Map args = [:], GriffonView view, Map dialogProperties = [:], LayerUI layerUI = null, Closure onFinish = null) {
        def app = ApplicationHolder.application
        MVCGroup mvcGroup = app.mvcGroupManager.getAt(mvcGroupName)
        if (!mvcGroup) {
            mvcGroup = app.buildMVCGroup(mvcGroupName, mvcGroupName, args)
        }
        showMVCGroup(mvcGroup, view, dialogProperties, layerUI, onFinish)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    static boolean confirm(Component parent, String message, String title, int messageType = JOptionPane.QUESTION_MESSAGE) {
        int result
        ApplicationHolder.application.execInsideUISync {
            result = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION, messageType)
        }
        return (result == JOptionPane.YES_OPTION)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    static void message(Component parent, String message, String title, int messageType = JOptionPane.INFORMATION_MESSAGE) {
        ApplicationHolder.application.execInsideUISync {
            JOptionPane.showMessageDialog(parent, message, title, messageType)
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    static String input(Component parent, String message, String title, int messageType = JOptionPane.QUESTION_MESSAGE) {
        def result
        ApplicationHolder.application.execInsideUISync {
            result = JOptionPane.showInputDialog(parent, message, title, messageType)
        }
        return result
    }

}
