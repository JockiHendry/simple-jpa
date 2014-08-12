package simplejpa.scaffolding.generator.ddd

import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.BasicAttribute
import simplejpa.scaffolding.generator.basic.BasicAttributeGenerator

class DDDBasicAttributeGenerator extends BasicAttributeGenerator {

    DDDBasicAttributeGenerator(BasicAttribute attribute, Scaffolding scaffolding) {
        super(attribute, scaffolding)
    }

    public List<String> repo_update() {
        ["${name} = ${scaffolding.generator.domainClassNameAsProperty}.${name}"]
    }

}
