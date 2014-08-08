package simplejpa.scaffolding.attribute

import griffon.util.*

class DateAttribute extends Attribute {

    public static final def TYPES = ["DateTime", "LocalDateTime", "LocalDate", "LocalTime"]

    public DateAttribute(Map information) {
        super(information.'name', information.'type')
    }

    public boolean includeTime() {
        ["DateTime", "LocalDateTime", "LocalTime"].contains(type)
    }

    public boolean includeDate() {
        ["DateTime", "LocalDateTime", "LocalDate"].contains(type)
    }

    public static boolean isInstanceOf(Map information) {
        TYPES.contains(information.'type')
    }

}
