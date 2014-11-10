package simplejpa.scaffolding.generator.basic

import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.EntityAttribute

class EntityAttributeGenerator extends BuiltInAttributeGenerator {

    String var_table
    String var_findResult

    EntityAttributeGenerator(EntityAttribute attribute, Scaffolding scaffolding) {
        super(attribute, scaffolding)
        var_table = "${name}List"
        var_findResult = "${name}Result"
    }

    @Override
    List<String> modelAttr() {
        if (attribute.oneToOne || attribute.embedded) {
            return ["@Bindable $type $name"]
        } else if (attribute.manyToOne) {
            return [
                "BasicEventList<$type> $var_table = new BasicEventList<>()",
                "@Bindable DefaultEventComboBoxModel<$type> $name = ",
                "\tGlazedListsSwing.eventComboBoxModelWithThreadProxyList($var_table)"
            ]
        }
    }

    List<String> clearList() {
        if (attribute.manyToOne) {
            return ["model.${var_table}.clear()"]
        }
        []
    }

    List<String> findList() {
        if (attribute.manyToOne) {
            return ["List $var_findResult = findAll${attribute.target.name}()"]
        }
        []
    }

    List<String> setList() {
        if (attribute.manyToOne) {
            return ["model.${var_table}.addAll($var_findResult)"]
        }
        []
    }

    List<String> constructor() {
        if (attribute.isInverse()) return []
        if (attribute.manyToOne) {
            return ["$name: model.${name}.selectedItem"]
        }
        ["$name: model.$name"]
    }

    @Override
    List<String> asColumn() {
        ["glazedColumn(name: '$columnCaption', property: '$name')"]
    }

    @Override
    List<String> asDataEntry() {
        if (attribute.oneToOne) {
            if (attribute.isInverse()) {
                return ["label(id: '$name', text: bind {model.$name}, errorPath: '$name')"]
            }
            return ["button(action: ${attribute.actionName}, errorPath: '$name')"]
        } else if (attribute.embedded) {
            return ["button(action: ${attribute.actionName}, errorPath: '$name')"]
        } else if (attribute.manyToOne) {
            if (attribute.isInverse()) {
                return ["label(id: '$name', text: '// TODO: $name is an inverse.', errorPath: '$name')"]
            }
            return ["comboBox(id: '$name', model: model.$name, errorPath: '$name')"]
        }
    }

    @Override
    List<String> update(String var) {
        if (attribute.isInverse()) {
            return ["// TODO: $name will not be updated because it is an inverse that doesn't have CascadeType.ALL or it is a many-to-one that should not be edited."]
        }
        def result = []
        if (attribute.oneToOne) {
            if (!attribute.hasCascadeAndOrphanRemoval) {
                result << "// TODO: You may need to add code here because it seems that you haven't included cascade=CascadeType.ALL and orphanRemoval=true for $name"
            }
            result << "${var}.$name = model.${name}"
        } else if (attribute.embedded) {
            result << "${var}.$name = model.${name}"
        } else if (attribute.manyToOne) {
            result << "${var}.$name = model.${name}.selectedItem"
        }
        result
    }

    @Override
    List<String> clear() {
        if (attribute.oneToOne || attribute.embedded) {
            return ["model.$name = null"]
        } else if (attribute.manyToOne) {
            return ["model.${name}.selectedItem = null"]
        }
    }

    @Override
    List<String> selected() {
        if (attribute.oneToOne || attribute.embedded) {
            return ["model.$name = selected.$name"]
        } else {
            return ["model.${name}.selectedItem = selected.$name"]
        }
    }

    @Override
    List<String> pair_init(String var) {
        if (attribute.oneToOne || attribute.embedded) {
            return ["model.$name = ${var}.$name"]
        } else if (attribute.manyToOne) {
            return ["model.${name}.selectedItem = ${var}.$name"]
        }
    }

    @Override
    List<String> action() {
        if ((attribute.oneToOne && !attribute.inverse) || attribute.embedded) {
            return ["action(id: '${attribute.actionName}', name: '$buttonName', closure: controller.${attribute.actionName})"]
        }
        []
    }

    @Override
    List<String> popup() {
        if ((attribute.oneToOne && !attribute.inverse) || attribute.embedded) {
            return [
                "@Transaction(Transaction.Policy.SKIP)",
                "def ${attribute.actionName} = {",
                "\texecInsideUISync {",
                "\t\tdef args = [pair: model.${name}]",
                "\t\tdef props = [title: '$buttonName']",
                "\t\tDialogUtils.showMVCGroup('${attribute.target.nameAsProperty}AsPair', args, view, props) { m, v, c ->",
                "\t\t\tmodel.${name} = m.${name}",
                "\t\t}",
                "\t}",
                "}"
            ]
        }
        []
    }

}

