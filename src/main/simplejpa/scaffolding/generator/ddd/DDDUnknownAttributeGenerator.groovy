package simplejpa.scaffolding.generator.ddd

import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.UnknownAttribute
import simplejpa.scaffolding.generator.basic.UnknownAttributeGenerator

class DDDUnknownAttributeGenerator extends UnknownAttributeGenerator {

    DDDUnknownAttributeGenerator(UnknownAttribute attribute, Scaffolding scaffolding) {
        super(attribute, scaffolding)
    }

    public List<String> repo_update() {
        ["${name} = ${scaffolding.generator.domainClassNameAsProperty}.${name}"]
    }

}
