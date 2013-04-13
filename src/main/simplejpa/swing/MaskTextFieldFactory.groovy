/*
 * Copyright 2013 Jocki Hendry.
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

package simplejpa.swing

import griffon.util.GriffonNameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.JFormattedTextField
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.MaskFormatter

class MaskTextFieldFactory extends AbstractFactory {

    private Logger LOG = LoggerFactory.getLogger(MaskTextFieldFactory)

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        if (!attributes.containsKey("bindTo")) {
            throw new IllegalArgumentException("In $name you must define a String value for bindTo")
        }
        String bindTo = attributes.remove("bindTo")

        JFormattedTextField instance = new JFormattedTextField()

        MaskFormatter maskFormatter = attributes.remove("maskFormatter")
        if (maskFormatter==null) {
            maskFormatter = new MaskFormatter(attributes.remove("mask") ?: "")
        }

        attributes.keySet().findAll { it.startsWith("mf") }.each { String key ->
            String propertyName = GriffonNameUtils.getPropertyName(key[2..-1])
            maskFormatter."$propertyName" = attributes.remove(key)
        }

        instance.setFormatterFactory(new DefaultFormatterFactory(maskFormatter))

        builder.withBuilder(builder, {
            bind(source: instance, sourceProperty: 'value', target: builder.model, targetProperty: bindTo, mutual: true)
        })

        return instance
    }

    @Override
    boolean isLeaf() {
        true
    }


}
