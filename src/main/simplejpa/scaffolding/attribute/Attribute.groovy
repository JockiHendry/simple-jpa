package simplejpa.scaffolding.attribute

import simplejpa.scaffolding.generator.AttributeGenerator

abstract class Attribute {

    String name
    String type
    AttributeGenerator generator

    public Attribute(String name, String type) {
        this.name = name
        this.type = type
    }

}
