package simplejpa.validation

import javax.swing.*

class ErrorLabelFactory extends AbstractFactory {

    public ErrorLabel newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        if (FactoryBuilderSupport.checkValueIsTypeNotString(value, name, JLabel)) {
            return value
        }
        if (!attributes.containsKey('path')) {
            throw new IllegalArgumentException("In $name you must define a value for path of type String")
        }
        String path = attributes.remove('path')
        return new ErrorLabel(path, builder.model.errors)
    }

    public boolean isLeaf() {
        return true
    }

}
