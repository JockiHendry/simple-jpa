package simplejpa.scaffolding.generator.basic

import simplejpa.scaffolding.attribute.CollectionAttribute
import griffon.util.*

class CollectionAttributeGenerator extends BuiltInAttributeGenerator {

    String targetType
    String var_findResult
    String var_glazedTable
    boolean ignoreLazy = true

    CollectionAttributeGenerator(CollectionAttribute attribute) {
        super(attribute)
        targetType = attribute.targetType
        var_findResult = "${name}Result"
        var_glazedTable = GriffonNameUtils.getPropertyName(targetType) + "List"
    }

    @Override
    List<String> modelAttr() {
        if (attribute.oneToMany) {
            return ["List<$targetType> $name = []"]
        } else if (attribute.manyToMany) {
            return ["TagChooserModel $name = new TagChooserModel()"]
        }
    }

    @Override
    List<String> asColumn() {
        []
    }

    @Override
    List<String> asDataEntry() {
        String textForMappedBy = (!attribute.eager && !ignoreLazy)? "'// TODO: $name is a lazy field'": "bind {model.${name}.join(',')}"
        if (attribute.oneToMany) {
            if (attribute.mappedBy && !attribute.hasCascadeAndOrphanRemoval) {
                return ["label(id: '$name', text: $textForMappedBy, errorPath: '$name')"]
            } else {
                return [
                    "mvcPopupButton(id: '$name', text: '$buttonName', errorPath: '$name', mvcGroup: '${attribute.target.nameAsProperty}AsChild',",
                    "\targs: {[parentList: model.$name]}, dialogProperties: [title: '$buttonName'], onFinish: { m, v, c -> ",
                    "\t\tmodel.${name}.clear()",
                    "\t\tmodel.${name}.addAll(m.$var_glazedTable)",
                    "\t}",
                    ")"
                ]
            }
        } else if (attribute.manyToMany) {
            if (attribute.mappedBy && !attribute.hasCascadeAndOrphanRemoval) {
                return ["label(id: '$name', text: $textForMappedBy, errorPath: '$name')"]
            } else {
                return ["tagChooser(id: '$name', model: model.$name, constraints: 'grow,push,span,wrap', errorPath: '$name')"]
            }
        }
    }

    List<String> findList() {
        if (attribute.manyToMany) {
            return ["List $var_findResult = findAll${attribute.target.name}()"]
        }
        []
    }

    List<String> setList() {
        if (attribute.mappedBy) return []
        if (attribute.manyToMany) {
            return ["model.${name}.replaceValues($var_findResult)"]
        }
    }

    List<String> constructor() {
        if (attribute.oneToMany) {
            return ["$name: new ArrayList(model.$name)"]
        } else if (attribute.manyToMany) {
            return ["$name: model.${name}.selectedValues"]
        }
    }

    @Override
    List<String> update(String var) {
        if (attribute.mappedBy && !attribute.hasCascadeAndOrphanRemoval) {
            return ["// TODO: $name will not be updated because it is an inverse that doesn't have CascadeType.ALL"]
        }
        def result = []
        result << "${var}.${name}.clear()"
        if (attribute.oneToMany) {
            result << "${var}.${name}.addAll(model.$name)"
        } else if (attribute.manyToMany) {
            result << "${var}.${name}.addAll(model.${name}.selectedValues)"
        }
        result
    }

    @Override
    List<String> clear() {
        if (attribute.oneToMany) {
            return ["model.${name}.clear()"]
        } else if (attribute.manyToMany) {
            return ["model.${name}.clearSelectedValues()"]
        }
    }

    @Override
    List<String> selected() {
        if (!attribute.eager && !ignoreLazy) {
            return ["// TODO: $name is a lazy field. We can't load it outside of transaction."]
        }
        if (attribute.oneToMany) {
            return [
                "model.${name}.clear()",
                "model.${name}.addAll(selected.$name)"
            ]
        } else if (attribute.manyToMany) {
            return ["model.${name}.replaceSelectedValues(selected.$name)"]
        }
    }

    @Override
    List<String> pair_init(String var) {
        if (attribute.oneToMany) {
            return [
                "if (${var}.$name) {",
                "\tmodel.${name}.clear()",
                "\tmodel.${name}.addAll(${var}.$name)",
                "}"
            ]
        } else if (attribute.manyToMany) {
            return [
                "if (${var}.$name) {",
                "\tmodel.${name}.replaceSelectedValues(${var}.$name)",
                "}"
            ]
        }
    }


}
