package simplejpa.scaffolding.generator.ddd

import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.DateAttribute
import simplejpa.scaffolding.generator.basic.BuiltInAttributeGenerator
import simplejpa.scaffolding.generator.basic.DateAttributeGenerator

class DDDDateAttributeGenerator extends DateAttributeGenerator {

    DDDDateAttributeGenerator(DateAttribute attribute, Scaffolding scaffolding) {
        super(attribute, scaffolding)
    }

    public List<String> repo_update() {
        ["${name} = ${scaffolding.generator.domainClassNameAsProperty}.${name}"]
    }
}
