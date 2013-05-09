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

import groovy.swing.factory.BeanFactory
import simplejpa.SimpleJpaHandler
import simplejpa.swing.MaskTextFieldFactory
import simplejpa.swing.DateTimePicker
import simplejpa.swing.EventTableModelFactory
import simplejpa.swing.ListCellRendererFactory
import simplejpa.swing.NumberTextFieldFactory
import simplejpa.swing.TagChooser
import simplejpa.validation.ConverterFactory
import simplejpa.validation.ErrorLabelFactory
import simplejpa.validation.NodeErrorNotificationFactory

import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence
import javax.validation.Validation
import javax.validation.Validator
import java.util.concurrent.ConcurrentHashMap

class SimpleJpaGriffonAddon {

    void addonPostInit(GriffonApplication app) {
        def types = app.config.griffon?.simplejpa?.injectInto ?: ['controller']

        final EntityManagerFactory emf = Persistence.createEntityManagerFactory("default")
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

        types.each {
            app.artifactManager.getClassesOfType(it).each { GriffonClass gc ->
                // Creating new handler
                SimpleJpaHandler simpleJpaHandler = new SimpleJpaHandler(emf, validator,
                        app.config.griffon?.simplejpa?.method?.prefix ?: '',
                        app.config.griffon?.simplejpa?.model?.package ?: 'domain',
                        (app.config.griffon?.simplejpa?.finder?.alwaysExcludeSoftDeleted ?: false) as boolean
                )
                gc.metaClass.methodMissing =  simpleJpaHandler.methodMissingHandler
            }
        }

        // For validation-handling
        app.artifactManager.getClassesOfType("model").each { GriffonClass gc ->
            gc.metaClass."errors" = new ObservableMap(new ConcurrentHashMap())
            gc.metaClass."hasError" = {
                boolean errorFound = false
                errors.each { k, v -> if (v?.length()>0) errorFound = true}
                return errorFound
            }
        }
    }

    Map factories = [
        errorLabel: new ErrorLabelFactory(),
        toInteger: new ConverterFactory(ConverterFactory.TYPE.INTEGER),
        toReverseString: new ConverterFactory(ConverterFactory.TYPE.REVERSE_STRING),
        templateRenderer: new ListCellRendererFactory(),
        eventTableModel: new EventTableModelFactory(),

        tagChooser: new BeanFactory(TagChooser, false),
        dateTimePicker: new BeanFactory(DateTimePicker, false),
        numberTextField: new NumberTextFieldFactory(),
        maskTextField: new MaskTextFieldFactory(),
    ]

    List attributeDelegates = [
            {builder, node, attributes ->
                if (attributes.get('errorPath')!=null) {
                    ObservableMap errors = builder.model.errors
                    String errorPath = attributes.remove('errorPath')
                    NodeErrorNotificationFactory.addErrorNotification(node, errors, errorPath)
                }
            }
    ]



}
