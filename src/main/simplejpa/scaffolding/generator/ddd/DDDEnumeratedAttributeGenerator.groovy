package simplejpa.scaffolding.generator.ddd

import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.EnumeratedAttribute
import simplejpa.scaffolding.generator.basic.EnumeratedAttributeGenerator

class DDDEnumeratedAttributeGenerator extends EnumeratedAttributeGenerator {

    DDDEnumeratedAttributeGenerator(EnumeratedAttribute attribute, Scaffolding scaffolding) {
        super(attribute, scaffolding)
    }

    public List<String> repo_update() {
        ["${name} = ${scaffolding.generator.domainClassNameAsProperty}.${name}"]
    }

}
