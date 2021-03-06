package simplejpa.scaffolding.attribute

class BasicAttribute extends Attribute {

    public static final def TYPES = ['Boolean', 'boolean', 'Character', 'char', 'Byte', 'byte', 'Short', 'short',
         'Integer', 'int', 'Long', 'long', 'Float', 'float', 'Double', 'double', 'String', 'BigInteger', 'BigDecimal']

    public static final def NOT_SUPPORTED = ['char', 'byte', 'short', 'int', 'long', 'float', 'double']

    public BasicAttribute(Map information) {
        super(information.'name', information.'type')
    }

    public boolean isNumber() {
        ['Byte', 'byte', 'Short', 'short', 'Integer', 'int', 'Long', 'long', 'Float', 'float',
         'Double', 'double', 'BigInteger'].contains(type)
    }

    public boolean isBigDecimal() {
        type == 'BigDecimal'
    }

    public boolean isBoolean() {
        ['Boolean', 'boolean'].contains(type)
    }
    
    public boolean isNotSupported() {
        NOT_SUPPORTED.contains(type)
    }

    public static boolean isInstanceOf(Map information) {
        TYPES.contains(information.'type')
    }

}
