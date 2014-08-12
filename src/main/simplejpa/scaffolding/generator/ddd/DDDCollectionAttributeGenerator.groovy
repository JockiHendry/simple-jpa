package simplejpa.scaffolding.generator.ddd

import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.CollectionAttribute
import simplejpa.scaffolding.generator.basic.CollectionAttributeGenerator

class DDDCollectionAttributeGenerator extends CollectionAttributeGenerator {

    DDDGenerator generator

    DDDCollectionAttributeGenerator(CollectionAttribute attribute, Scaffolding scaffolding) {
        super(attribute, scaffolding)
        generator = scaffolding.generator
    }

    List<String> findList() {
        if (attribute.manyToMany) {
            return ["List $var_findResult = ${generator.repositoryVar}.findAll${attribute.target.name}()"]
        }
        []
    }

    public List<String> repo_update() {
        List<String> result = []
        if (attribute.embeddedCollection) {
            result << "${name}.clear()"
            result << "${name}.addAll(${scaffolding.generator.domainClassNameAsProperty}.${name})"
        } else {
            if (!attribute.hasCascadeAndOrphanRemoval) {
                result << "// TODO: The following code may not work because mapping for ${name} doesn't enable cascading."
            }
            result << "${name}.clear()"
            result << "${scaffolding.generator.domainClassNameAsProperty}.${name}.each {"
            if (attribute.bidirectional) {
                result << "\tit.${attribute.mappedBy} = merged${scaffolding.generator.domainClass.name}"
            }
            result << "\t${name} << merge(it)"
            result << "}"
        }
        result
    }

}
