package simplejpa.validation

import org.apache.commons.logging.LogFactory
import org.jdesktop.swingx.JXDatePicker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.swing.TagChooser;

import javax.swing.*
import java.awt.Color
import java.awt.Component
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class NodeErrorNotificationFactory {

    public static void addErrorNotification(JComponent node, ObservableMap errors, String errorPath) {
        errors.addPropertyChangeListener(new BasicHighlightErrorNotification(node, errors, errorPath))
        try {
            BasicClearErrorTrigger."enhance${node.class.simpleName}"(node, errors, errorPath)
        } catch (MissingMethodException ex) {
            BasicClearErrorTrigger.enhanceJTextField(node, errors, errorPath)
        }
    }
}

class BasicHighlightErrorNotification implements PropertyChangeListener {

    Color normalBackgroundColor
    ObservableMap errors
    String errorPath
    JComponent node
    String backgroundPropertyName

    public BasicHighlightErrorNotification(JComponent node, ObservableMap errors, String errorPath) {
        this.node = node
        this.errors = errors
        this.errorPath = errorPath
        this.normalBackgroundColor = node.getBackground()
    }

    void normalBackground() {
        if (node instanceof JXDatePicker) {
            node.editor.background = normalBackgroundColor
        } else {
            node.background = normalBackgroundColor
        }
    }

    @Override
    void propertyChange(PropertyChangeEvent evt) {

        normalBackground()

        if (errors.get(errorPath)?.length() > 0) {
            if (node instanceof JXDatePicker) {
                node.editor.background = Color.PINK
            } else {
                node.background = Color.PINK
            }
        }
    }

}

class BasicClearErrorTrigger {

    static enhanceJXDatePicker(JXDatePicker component, ObservableMap errors, String errorPath) {
        component.propertyChange = { PropertyChangeEvent e ->
            if ("date".equals(e.propertyName)) errors.remove(errorPath)
        }
    }

    static enhanceJComboBox(JComboBox component, ObservableMap errors, String errorPath) {
        component.itemStateChanged = { ItemEvent e ->
            errors.remove(errorPath)
        }
    }

    static enhanceJTextField(JTextField component, ObservableMap errors, String errorPath) {
        component.keyTyped = { KeyEvent e ->
            errors.remove(errorPath)
        }
    }

    static enhanceTagChooser(TagChooser component, ObservableMap errors, String errorPath) {
        component.selectedValueChanged = {
            errors.remove(errorPath)
        }
    }

}

