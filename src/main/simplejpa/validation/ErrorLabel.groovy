package simplejpa.validation

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.JLabel
import java.awt.Color
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

class ErrorLabel extends JLabel {

    private static Logger LOG = LoggerFactory.getLogger(ErrorLabel)

    String path

    public ErrorLabel(String path, ObservableMap errors) {
        this.path = path
        setForeground(Color.RED)
        setVisible(false)
        errors.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            void propertyChange(PropertyChangeEvent evt) {
                setVisible(false)
                if (errors.get(path)?.length() > 0) {
                    setText(errors.get(path))
                    setVisible(true)
                }
            }
        })

    }
}
