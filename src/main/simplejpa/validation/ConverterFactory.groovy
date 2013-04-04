package simplejpa.validation

import javax.swing.JLabel


class ConverterFactory extends AbstractFactory {

    public static enum TYPE { INTEGER, REVERSE_STRING }
    public TYPE type

    public ConverterFactory(TYPE type) {
        this.type = type
    }

    public Closure newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) {
        if (!FactoryBuilderSupport.checkValueIsType(value, name, String) && !name.equals("toReverseString")) {
            throw new IllegalArgumentException("In $name you must define a value of type String")
        }

        def errors = builder.model.errors

        switch (type) {
            case TYPE.INTEGER: return {
                errors.remove(value)
                if (!it?.isEmpty()) {
                    try {
                        return Integer.parseInt(it)
                    } catch (NumberFormatException) {
                        def errorMessage = builder.app.getMessage(
                            "simplejpa.converter.toInteger", "must be a number")
                        errors[value] = errorMessage
                        return null
                    }
                }
            }
            case TYPE.REVERSE_STRING: return {
                it==null?"":String.valueOf(it)
            }
        }
    }

    public boolean isLeaf() {
        return true
    }
}
