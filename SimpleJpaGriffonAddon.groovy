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
import simplejpa.swing.NumberTextFieldFactory
import simplejpa.swing.TagChooser
import simplejpa.swing.TemplateListCellRenderer
import simplejpa.validation.BasicHighlightErrorNotification
import simplejpa.validation.ConverterFactory
import simplejpa.validation.DateTimePickerErrorCleaner
import simplejpa.validation.ErrorLabelFactory
import simplejpa.validation.ErrorNotification
import simplejpa.validation.JButtonErrorCleaner
import simplejpa.validation.JComboBoxErrorCleaner
import simplejpa.validation.JTextFieldErrorCleaner
import simplejpa.validation.JXDatePickerErrorCleaner
import simplejpa.validation.TagChooserErrorCleaner

import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence
import javax.swing.JComboBox
import javax.swing.JList
import javax.validation.Validation
import javax.validation.Validator
import java.beans.PropertyChangeListener
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

        // read error cleaners from configuration
        if (app.config.griffon.simplejpa.validation.containsKey("errorCleaners") &&
            !app.config.griffon.simplejpa.validation.errorCleaners.isEmpty()) {

            Map values = app.config.griffon.simplejpa.validation.errorCleaners
            values.each { k, v ->
                if (v instanceof Class) {
                    errorCleaners[k] = v.newInstance()
                } else {
                    errorCleaners[k] = v
                }
            }
        }
    }

    Map factories = [
        errorLabel: new ErrorLabelFactory(),
        toInteger: new ConverterFactory(ConverterFactory.TYPE.INTEGER),
        toReverseString: new ConverterFactory(ConverterFactory.TYPE.REVERSE_STRING),
        eventTableModel: new EventTableModelFactory(),

        tagChooser: new BeanFactory(TagChooser, false),
        dateTimePicker: new BeanFactory(DateTimePicker, false),
        numberTextField: new NumberTextFieldFactory(),
        maskTextField: new MaskTextFieldFactory(),
    ]

    Map errorCleaners = [
        'default': new JTextFieldErrorCleaner(),
        'javax.swing.JTextField' : new JTextFieldErrorCleaner(),
        'javax.swing.JComboBox': new JComboBoxErrorCleaner(),
        'javax.swing.JButton': new JButtonErrorCleaner(),

        'org.jdesktop.swingx.JXDatePicker': new JXDatePickerErrorCleaner(),
        'simplejpa.swing.TagChooser': new TagChooserErrorCleaner(),
        'simplejpa.swing.DateTimePicker': new DateTimePickerErrorCleaner(),
    ]

    List attributeDelegates = [
        {builder, node, attributes ->
            if (attributes.get('errorPath')!=null) {

                ObservableMap errors = builder.model.errors
                ErrorNotification errorNotification

                String errorPath = attributes.remove('errorPath')
                def errorNotificationAttr = attributes.remove('errorNotification')

                if (errorNotificationAttr) {
                    errorNotification = errorNotificationAttr.newInstance([node, errors, errorPath].toArray())
                } else  if (builder.app.config.griffon.simplejpa.validation.containsKey('defaultErrorNotificationClass')) {
                    errorNotification = builder.app.config.griffon.simplejpa.validation.defaultErrorNotificationClass.newInstance([node, errors, errorPath].toArray())
                } else {
                    errorNotification = new BasicHighlightErrorNotification(node, errors, errorPath)
                }

                // Remove existing PropertyChangeListener
                errors.getPropertyChangeListeners().findAll { PropertyChangeListener pcl ->
                    (pcl instanceof ErrorNotification) &&
                    ((ErrorNotification) pcl).errorPath == errorNotification.errorPath
                }.each { PropertyChangeListener pcl ->
                    errors.removePropertyChangeListener(pcl)
                }

                errors.addPropertyChangeListener(errorNotification)

                if (errorCleaners.containsKey('*')) {
                    errorCleaners.'*'.addErrorCleaning(node, errors, errorPath)
                } else if (errorCleaners.containsKey(node.class.canonicalName)) {
                    errorCleaners[node.class.canonicalName].addErrorCleaning(node, errors, errorPath)
                } else {
                    errorCleaners['default']?.addErrorCleaning(node, errors, errorPath)
                }

            }
            if (attributes.get('templateRenderer')!=null) {
                String templateString = attributes.remove('templateRenderer')
                if (node instanceof JComboBox) {
                    node.setRenderer(new TemplateListCellRenderer(templateString))
                } else if (node instanceof JList) {
                    node.setCellRenderer(new TemplateListCellRenderer(templateString))
                } else {
                    throw new Exception("templateRenderer can't be applied to $node")
                }
            }
        }
    ]



}
