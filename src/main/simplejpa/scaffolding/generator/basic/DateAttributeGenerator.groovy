package simplejpa.scaffolding.generator.basic

import simplejpa.scaffolding.attribute.DateAttribute
import griffon.util.*

class DateAttributeGenerator extends BuiltInAttributeGenerator {

    DateAttributeGenerator(DateAttribute attribute) {
        super(attribute)
    }

    @Override
    List<String> modelAttr() {
        ["@Bindable $type $name"]
    }

    @Override
    List<String> asColumn() {
        ["glazedColumn(name: '$columnCaption', property: '$name')"]
    }

    @Override
    List<String> asDataEntry() {
        ["dateTimePicker(id: '$name', ${GriffonNameUtils.getPropertyName(type)}: bind('$name', target: model, mutual: true), " +
         "errorPath: '$name', dateVisible: ${attribute.includeDate()?'true':'false'}, timeVisible: ${attribute.includeTime()?'true':'false'})"]
    }

    List<String> constructor() {
        ["$name: model.$name"]
    }

    @Override
    List<String> update(String var) {
        ["${var}.$name = model.$name"]
    }

    @Override
    List<String> clear() {
        ["model.${name} = null"]
    }

    @Override
    List<String> selected() {
        ["model.${name} = selected.$name"]
    }

    @Override
    List<String> pair_init(String var) {
        return ["model.$name = ${var}.$name"]
    }

    @Override
    List<String> action() {
        []
    }

    @Override
    List<String> popup() {
        []
    }

}
