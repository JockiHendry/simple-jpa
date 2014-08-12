package simplejpa.scaffolding.generator.basic

import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.EnumeratedAttribute

class EnumeratedAttributeGenerator extends BuiltInAttributeGenerator {

    EnumeratedAttributeGenerator(EnumeratedAttribute attribute, Scaffolding scaffolding) {
        super(attribute, scaffolding)
    }

    @Override
    List<String> modelAttr() {
        ["EnumComboBoxModel<$type> $name = new EnumComboBoxModel<$type>($type)"]
    }

    @Override
    List<String> asColumn() {
        ["glazedColumn(name: '$columnCaption', property: '$name')"]
    }

    @Override
    List<String> asDataEntry() {
        ["comboBox(id: '${name}', model: model.${name}, errorPath: '${name}')"]
    }

    List<String> constructor() {
        ["$name: model.${name}.selectedItem"]
    }

    @Override
    List<String> update(String var) {
        ["${var}.$name = model.$name"]
    }

    @Override
    List<String> clear() {
        ["model.${name}.selectedItem = null"]
    }

    @Override
    List<String> selected() {
        ["model.${name}.selectedItem = selected.$name"]
    }

    @Override
    List<String> pair_init(String var) {
        return ["model.${name}.selectedItem = ${var}.$name"]
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
