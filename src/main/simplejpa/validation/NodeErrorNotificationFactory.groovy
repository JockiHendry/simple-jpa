package simplejpa.validation

import org.jdesktop.swingx.JXDatePicker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.swing.DateTimePicker
import simplejpa.swing.TagChooser

import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ItemEvent
import java.awt.event.KeyEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

public class NodeErrorNotificationFactory {

    static Logger LOG = LoggerFactory.getLogger(NodeErrorNotificationFactory)

    public static void addErrorNotification(JComponent node, ObservableMap errors, String errorPath) {
        try {
            BasicClearErrorTrigger."enhance${node.class.simpleName}"(node, errors, errorPath)
        } catch (MissingMethodException ex) {
            BasicClearErrorTrigger.enhanceJTextField(node, errors, errorPath)
        }
    }

}

public abstract class ErrorNotification implements PropertyChangeListener {

    JComponent node
    ObservableMap errors
    String errorPath

    protected ErrorNotification(JComponent node, ObservableMap errors, String errorPath) {
        this.node = node
        this.errors = errors
        this.errorPath = errorPath
    }

    @Override
    void propertyChange(PropertyChangeEvent evt) {
        performNotification()
    }

    abstract void performNotification()

}

public class NopErrorNotification extends ErrorNotification {

    protected NopErrorNotification(JComponent node, ObservableMap errors, String errorPath) {
        super(node, errors, errorPath)
    }

    @Override
    void performNotification() {}

}

public class BasicHighlightErrorNotification extends ErrorNotification {

    Color normalBackgroundColor
    Color errorBackgroundColor = Color.PINK

    static final Logger LOG = LoggerFactory.getLogger(BasicHighlightErrorNotification)

    public BasicHighlightErrorNotification(JComponent node, ObservableMap errors, String errorPath) {
        super(node, errors, errorPath)
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
    void performNotification() {
        def action = {
            if (errors.get(errorPath)?.length() > 0) {
                setBackground(errorBackgroundColor)
            } else {
                setBackground(normalBackgroundColor)
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

    static enhanceJButton(JButton component, ObservableMap errors, String errorPath) {
        component.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent e) {
                errors.remove(errorPath)
            }
        })
    }

}

