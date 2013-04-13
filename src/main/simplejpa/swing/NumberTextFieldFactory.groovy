package simplejpa.swing

import griffon.util.GriffonNameUtils
import org.jdesktop.swingx.text.NumberFormatExt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.JFormattedTextField
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter
import java.text.DecimalFormat
import java.text.NumberFormat


class NumberTextFieldFactory extends AbstractFactory {

    private Logger LOG = LoggerFactory.getLogger(NumberTextFieldFactory)

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        if (!attributes.containsKey("bindTo")) {
            throw new IllegalArgumentException("In $name you must define a String value for bindTo")
        }
        String bindTo = attributes.remove("bindTo")

        JFormattedTextField instance = new JFormattedTextField()

        DecimalFormat decimalFormat = attributes.remove("decimalFormat")
        if (decimalFormat==null) {
            String type = attributes.remove("type") ?: "default"
            switch (type) {
                case "currency":
                    decimalFormat = DecimalFormat.getCurrencyInstance()
                    break
                case "percent":
                    decimalFormat = DecimalFormat.getPercentInstance()
                    break
                case "integer":
                    decimalFormat = DecimalFormat.getIntegerInstance()
                    break
                default:
                    decimalFormat = DecimalFormat.getNumberInstance()
            }
        }

        NumberFormat editFormat = NumberFormat.getInstance()
        attributes.keySet().findAll { it.startsWith("nf") }.each { String key ->
            String propertyName = GriffonNameUtils.getPropertyName(key[2..-1])
            def v = attributes.remove(key)
            decimalFormat."$propertyName" = v
            editFormat."$propertyName" = v
        }

        NumberFormatter displayFormatter = new NumberFormatter(new NumberFormatExt(decimalFormat))
        NumberFormatter editFormatter = new NumberFormatter(new NumberFormatExt(editFormat))
        instance.setFormatterFactory(new DefaultFormatterFactory(displayFormatter, displayFormatter, editFormatter))

        builder.withBuilder(builder, {
            bind(source: instance, sourceProperty: 'value', target: builder.model, targetProperty: bindTo, mutual: true)
        })

        return instance
    }

    @Override
    boolean isLeaf() {
        true
    }

}
