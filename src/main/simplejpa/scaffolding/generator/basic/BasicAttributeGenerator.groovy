package simplejpa.scaffolding.generator.basic

import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.BasicAttribute

class BasicAttributeGenerator extends BuiltInAttributeGenerator {

    BasicAttributeGenerator(BasicAttribute attribute, Scaffolding scaffolding) {
        super(attribute, scaffolding)
    }

    @Override
    List<String> modelAttr() {
        List<String> result = []
        if (attribute.notSupported) {
            result << "// TODO: Native ${attribute.type} is not supported by scaffolding generator.  Please convert to object wrapper."
        }
        result << "@Bindable $type $name"
        result
    }

    @Override
    List<String> asColumn() {
        ["glazedColumn(name: '$columnCaption', property: '$name')"]
    }

    @Override
    List<String> asDataEntry() {
        if (attribute.notSupported) {
            return ["label(text: '// TODO: Native ${attribute.type} is not supported by scaffolding generator.  Please convert to object wrapper.')"]
        } else if (attribute.number) {
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
        if (attribute.notSupported) {
            return ["// TODO: Native ${attribute.type} is not supported by scaffolding generator.  Please convert to object wrapper."]
        } else if (attribute.boolean) {
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

    @Override
    List<String> action() {
        []
    }

    @Override
    List<String> popup() {
        []
    }

}
