package simplejpa.swing

import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import java.awt.Dialog
import java.awt.Window
import java.awt.event.ActionEvent
import static griffon.util.GriffonNameUtils.*


class MVCPopupButtonFactory extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {

        if (!attributes.containsKey('mvcGroup')) {
            throw new IllegalArgumentException("In $name you must define a value for mvcGroup of type String")
        }
        def mvcGroup = attributes.remove('mvcGroup')
        def arguments = ['popup': true]
        if (attributes['args'] instanceof Map) {
            arguments.putAll(attributes.remove('args'))
        } else if (attributes['args']) {
            arguments = attributes.remove('args')
        }
        Closure onFinish = attributes.remove('onFinish')
        Closure onBeforeDisplay = attributes.remove('onBeforeDisplay')
        def dialogProperties = attributes.remove('dialogProperties')
        JButton btnResult
        if (!value) {
            btnResult = new JButton()
        } else if (value instanceof JButton) {
            btnResult = value
        } else {
            btnResult = new JButton(value)
        }


        btnResult.actionPerformed = { ActionEvent actionEvent ->
            def args = arguments
            if (arguments instanceof Closure) {
                args = arguments.call()
            }
            onBeforeDisplay?.call(btnResult)
            DialogUtils.showMVCGroup(mvcGroup, args, builder.getVariable("app"),
                builder.getVariable("view"), dialogProperties, onFinish)
        }

        btnResult
    }

}
