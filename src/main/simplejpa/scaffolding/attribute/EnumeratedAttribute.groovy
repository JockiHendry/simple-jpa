package simplejpa.scaffolding.attribute

class EnumeratedAttribute extends Attribute {

    public EnumeratedAttribute(Map information) {
        super(information.'name', information.'type')
    }

    public static boolean isInstanceOf(Map information) {
        information['annotations']?.find { it.name == 'Enumerated' }
    }
}
