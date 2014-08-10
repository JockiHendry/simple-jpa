package simplejpa.scaffolding.generator.basic

import simplejpa.scaffolding.attribute.Attribute
import simplejpa.scaffolding.generator.AttributeGenerator
import griffon.util.*

abstract class BuiltInAttributeGenerator implements AttributeGenerator {

    Attribute attribute
    String buttonName
    String columnCaption
    String name
    String type
    String typeAsProperty

    public BuiltInAttributeGenerator(Attribute attribute) {
        this.attribute = attribute
        name = attribute.name
        type = attribute.type
        columnCaption = GriffonNameUtils.getNaturalName(attribute.name)
        buttonName = GriffonNameUtils.getNaturalName(attribute.name)
        typeAsProperty = GriffonNameUtils.getPropertyName(attribute.type)
    }

    abstract public List<String> modelAttr()

    abstract public List<String> asColumn()

    abstract public List<String> asDataEntry()

    abstract public List<String> constructor()

    abstract public List<String> update(String var)

    abstract public List<String> clear()

    abstract public List<String> selected()

    abstract public List<String> pair_init(String var)

    abstract public List<String> action()

    abstract public List<String> popup()

}
