package simplejpa.scaffolding.generator.ddd

import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.EntityAttribute
import simplejpa.scaffolding.generator.basic.EntityAttributeGenerator

class DDDEntityAttributeGenerator extends EntityAttributeGenerator {

    DDDGenerator generator

    DDDEntityAttributeGenerator(EntityAttribute attribute, Scaffolding scaffolding) {
        super(attribute, scaffolding)
        generator = scaffolding.generator
    }

    List<String> findList() {
        if (attribute.manyToOne) {
            return ["List $var_findResult = ${generator.repositoryVar}.findAll${attribute.target.name}()"]
        }
        []
    }

    public List<String> repo_update() {
        List<String> result = []
        if (attribute.oneToOne && !attribute.hasCascadeAndOrphanRemoval) {
            result << "// TODO: The following code may not work because this attribute mapping doesn't enable cascading."
        }
        result << "${name} = ${scaffolding.generator.domainClassNameAsProperty}.${name} == null? null: merge(${scaffolding.generator.domainClassNameAsProperty}.${name})"
        result
    }

}

