package simplejpa.validation

import org.jdesktop.swingx.JXDatePicker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.swing.DateTimePicker
import simplejpa.swing.TagChooser

import javax.swing.*
import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.KeyEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

public class NodeErrorNotificationFactory {

    static Logger LOG = LoggerFactory.getLogger(NodeErrorNotificationFactory)

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
    static final Logger LOG = LoggerFactory.getLogger(BasicHighlightErrorNotification)

    public BasicHighlightErrorNotification(JComponent node, ObservableMap errors, String errorPath) {
        this.node = node
        this.errors = errors
        this.errorPath = errorPath
        this.normalBackgroundColor = getBackground()
    }

    void setBackground(Color color) {
        if (node instanceof DateTimePicker) {
            node.componentBackground = color
        } else {
            node.background = color
        }
    }

    Color getBackground() {
        if (node instanceof DateTimePicker) {
            node.componentBackground
        } else {
            node.background
        }
    }

    @Override
    void propertyChange(PropertyChangeEvent evt) {
        def action = {
            setBackground(normalBackgroundColor)
            if (errors.get(errorPath)?.length() > 0) {
                setBackground(Color.PINK)
            }
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                void run() {
                    action()
                }
            })
        } else {
            action()
        }
    }

}

class BasicClearErrorTrigger {

    static Logger LOG = LoggerFactory.getLogger(BasicClearErrorTrigger)

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

    static enhanceDateTimePicker(DateTimePicker component, ObservableMap errors, String errorPath) {
        component.selectedValueChanged = {
            errors.remove(errorPath)
        }
    }

}

