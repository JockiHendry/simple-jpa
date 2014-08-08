package simplejpa.scaffolding.generator.basic

import simplejpa.scaffolding.attribute.BasicAttribute

class BasicAttributeGenerator extends BuiltInAttributeGenerator {

    BasicAttributeGenerator(BasicAttribute attribute) {
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
        if (attribute.number) {
            return ["numberTextField(id: '$name', columns: 20, bindTo: '$name', errorPath: '$name')"]
        } else if (attribute.bigDecimal) {
            return ["decimalTextField(id: '$name', columns: 20, bindTo: '$name', errorPath: '$name')"]
        } else if (attribute.boolean) {
            return ["checkBox(id: '$name', selected: bind('$name', target: model, mutual: true), errorPath: '$name')"]
        } else {
            return ["textField(id: '$name', columns: 50, text: bind('$name', target: model, mutual: true), errorPath: '$name')"]
        }
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
        if (attribute.boolean) {
            return ["model.$name = false"]
        } else {
            return ["model.$name = null"]
        }
    }

    @Override
    List<String> selected() {
        ["model.${name} = selected.$name"]
    }

    @Override
    List<String> pair_init(String var) {
        return ["model.$name = ${var}.$name"]
    }

}
