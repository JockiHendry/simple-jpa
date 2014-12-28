/*
 * Copyright 2014 Jocki Hendry.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        if (attribute.embedded) {
            result << "${name} = ${scaffolding.generator.domainClassNameAsProperty}.${name}"
        } else {
            if (attribute.oneToOne && !attribute.hasCascadeAndOrphanRemoval) {
                result << "// TODO: The following code may not work because this attribute mapping doesn't enable cascading."
            }
            result << "${name} = ${scaffolding.generator.domainClassNameAsProperty}.${name} == null? null: merge(${scaffolding.generator.domainClassNameAsProperty}.${name})"
        }
        result
    }

}

