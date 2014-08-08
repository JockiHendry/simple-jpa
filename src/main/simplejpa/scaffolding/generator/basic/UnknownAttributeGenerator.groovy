package simplejpa.scaffolding.generator.basic

import simplejpa.scaffolding.attribute.Attribute
import simplejpa.scaffolding.attribute.UnknownAttribute

class UnknownAttributeGenerator extends BuiltInAttributeGenerator {

    UnknownAttributeGenerator(UnknownAttribute attribute) {
        super(attribute)
    }

    @Override
    List<String> modelAttr() {
        [
            "// TODO: Please the declaration for $name if necessary.",
            "@Bindable $type $name"
        ]
    }

    @Override
    List<String> asColumn() {
        ["glazedColumn(name: '$columnCaption', property: '$name')"]
    }

    @Override
    List<String> asDataEntry() {
        ["textField(id: '$name', columns: 20, text: bind('$name', target: model, mutual: true), errorPath: '$name')"]

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
        [
            "// TODO: $name is not supported by generator.  You will need to code it manually.",
            "model.${name} = null"
        ]
    }

    @Override
    List<String> selected() {
        [
            "// TODO: $name is not supported by generator. You will need to code it manually.",
            "model.${name} = selected.$name"
        ]
    }

    @Override
    List<String> pair_init(String var) {
        return [
            "// TODO: $name is not supported by generator. You will need to code it manually.",
            "model.$name = ${var}.$name"
        ]
    }

}
