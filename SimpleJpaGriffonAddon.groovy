/*
 * Copyright 2015 Jocki Hendry.
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







import griffon.swing.SwingApplication
import groovy.swing.factory.BeanFactory
import org.codehaus.griffon.runtime.core.AbstractGriffonClass
import org.jdesktop.swingx.JXLoginPane
import org.jdesktop.swingx.auth.LoginService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.SimpleJpaHandler
import simplejpa.SimpleJpaUtil
import simplejpa.transaction.WithTransactionHandler
import simplejpa.swing.glazed.ast.Condition
import simplejpa.swing.glazed.factory.CustomConditionalRenderer
import simplejpa.swing.glazed.factory.DefaultHeaderRendererFactory
import simplejpa.swing.glazed.factory.GlazedColumnFactory
import simplejpa.swing.glazed.factory.GlazedTableFactory
import simplejpa.swing.MVCPopupButtonFactory
import simplejpa.swing.MaskTextFieldFactory
import simplejpa.swing.DateTimePicker
import simplejpa.swing.NumberTextFieldFactory
import simplejpa.swing.TagChooser
import simplejpa.swing.template.TemplateListCellRenderer
import simplejpa.swing.glazed.factory.TemplateRendererFactory
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
import java.awt.Window
import java.beans.PropertyChangeListener
import java.util.concurrent.ConcurrentHashMap
import griffon.util.*

class SimpleJpaGriffonAddon {

    private static Logger LOG = LoggerFactory.getLogger(SimpleJpaHandler)

    private String generateMethodName(String prefix, String methodName) {
        if (!prefix || prefix?.trim()?.isEmpty()) {
            return methodName
        }
        return prefix + GriffonNameUtils.capitalize(methodName)
    }

    void addonPostInit(GriffonApplication app) {
        def types = ConfigUtils.getConfigValue(app.config, 'griffon.simplejpa.finders.injectInto', ['controller'])

        final EntityManagerFactory emf = Persistence.createEntityManagerFactory("default", SimpleJpaUtil.instance.getJpaConfig())
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator()

        SimpleJpaUtil util = SimpleJpaUtil.instance
        util.entityManagerFactory = emf

        // Classes that will be injected with simple-jpa methods
        def targets = new HashSet()
        for (String type: types) {
            app.artifactManager.getClassesOfType(type).each { targets << it }
        }

        // Also injects simple-jpa methods to domain classes annotated with @Transaction
        InputStream input = app.getResourceAsStream(SimpleJpaUtil.FILE_ANNOTATED)
        if (input) {
            input.eachLine { String line ->
                try {
                    Class c = Class.forName(line)
                    if (targets.find { (it instanceof AbstractGriffonClass) && (it.clazz == c) } == null) {
                        if (!GriffonArtifact.isAssignableFrom(c)) {
                            targets << c
                        }
                    }
                } catch (Exception ex) {
                    LOG.error "Can't load [$line]", ex
                }
            }
        }

        SimpleJpaHandler simpleJpaHandler = new SimpleJpaHandler(emf, validator)
        util.handler = simpleJpaHandler

        targets << WithTransactionHandler

        for (def target: targets) {
            LOG.debug "Add SimpleJpaHandler to ${target instanceof AbstractGriffonClass? target.clazz.name: target.name}"

            target.metaClass.methodMissing =  simpleJpaHandler.methodMissingHandler

            // Add these methods dynamically
            target.metaClass.beginTransaction = simpleJpaHandler.beginTransaction
            target.metaClass.commitTransaction = simpleJpaHandler.commitTransaction
            target.metaClass.rollbackTransaction = simpleJpaHandler.rollbackTransaction
            target.metaClass.return_failed = simpleJpaHandler.returnFailed
            target.metaClass.getEntityManager = simpleJpaHandler.getEntityManager
            target.metaClass.withTransaction = simpleJpaHandler.withTransaction

            String pre = simpleJpaHandler.prefix
            target.metaClass."${generateMethodName(pre, 'persist')}" = simpleJpaHandler.persist
            target.metaClass."${generateMethodName(pre, 'validate')}" = simpleJpaHandler.validate
            target.metaClass."${generateMethodName(pre, 'merge')}" = simpleJpaHandler.merge
            target.metaClass."${generateMethodName(pre, 'remove')}" = simpleJpaHandler.remove
            target.metaClass."${generateMethodName(pre, 'softDelete')}" = simpleJpaHandler.softDelete
            target.metaClass."${generateMethodName(pre, 'executeNamedQuery')}" = simpleJpaHandler.executeNamedQuery
            target.metaClass."${generateMethodName(pre, 'executeQuery')}" = simpleJpaHandler.executeQuery
            target.metaClass."${generateMethodName(pre, 'executeNativeQuery')}" = simpleJpaHandler.executeNativeQuery

            // meta-methods for finders
            target.metaClass."${generateMethodName(pre, 'findByDsl')}" = simpleJpaHandler.findByDsl
            target.metaClass."${generateMethodName(pre, 'findAllByDsl')}" = simpleJpaHandler.findAllByDsl
            target.metaClass."${generateMethodName(pre, 'findByAnd')}" = simpleJpaHandler.findByAnd
            target.metaClass."${generateMethodName(pre, 'findAllByAnd')}" = simpleJpaHandler.findAllByAnd
            target.metaClass."${generateMethodName(pre, 'findByOr')}" = simpleJpaHandler.findByOr
            target.metaClass."${generateMethodName(pre, 'findAllByOr')}" = simpleJpaHandler.findAllByOr
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
                } else if (v instanceof String) {
                    errorCleaners[k] = Class.forName(v).newInstance()
                }
            }
        }

        // Displaying login dialog if configured to do so
        String loginServiceName = ConfigUtils.getConfigValueAsString(app.config, 'griffon.simplejpa.auditing.loginService', null)
        if (loginServiceName && !SimpleJpaUtil.instance.bootFromScript) {
            try {
                LoginService loginService = app.serviceManager.findService(loginServiceName)
                if (loginService == null) {
                    LOG.error "Can't find the following login service: $loginServiceName"
                } else {
                    JXLoginPane loginPane = new JXLoginPane(loginService)
                    Window startingWindow = (app as SwingApplication).windowManager.getStartingWindow()
                    JXLoginPane.Status status = JXLoginPane.showLoginDialog(startingWindow, loginPane)
                    if (status != JXLoginPane.Status.SUCCEEDED) {
                        app.shutdown()
                    }
                }
            } catch (Exception ex) {
                LOG.error "Error while displaying login service.", ex
                app.shutdown()
            }
        }
    }

    Map factories = [
        errorLabel: new ErrorLabelFactory(),
        toInteger: new ConverterFactory(ConverterFactory.TYPE.INTEGER),
        toReverseString: new ConverterFactory(ConverterFactory.TYPE.REVERSE_STRING),
        mvcPopupButton: new MVCPopupButtonFactory(),

        glazedTable: new GlazedTableFactory(),
        glazedColumn: new GlazedColumnFactory(),
        templateRenderer: new TemplateRendererFactory(),
        defaultHeaderRenderer: new DefaultHeaderRendererFactory(),
        customConditionalRenderer: new CustomConditionalRenderer(),
        condition: new BeanFactory(Condition, true),

        tagChooser: new BeanFactory(TagChooser, false),
        dateTimePicker: new BeanFactory(DateTimePicker, false),
        numberTextField: new NumberTextFieldFactory(),
        decimalTextField: new NumberTextFieldFactory(true),
        maskTextField: new MaskTextFieldFactory(),
    ]

    Map errorCleaners = [
        'default': new JTextFieldErrorCleaner(),
        'javax.swing.JTextField' : new JTextFieldErrorCleaner(),
        'javax.swing.JComboBox': new JComboBoxErrorCleaner(),
        'javax.swing.JCheckBox': new JComboBoxErrorCleaner(),
        'javax.swing.JButton': new JButtonErrorCleaner(),
        'javax.swing.JRadioButton': new JButtonErrorCleaner(),

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
                    errorNotification = Class.forName(builder.app.config.griffon.simplejpa.validation.defaultErrorNotificationClass)
                        .newInstance([node, errors, errorPath].toArray())
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
                def template = attributes.remove('templateRenderer')
                if (node instanceof JComboBox) {
                    node.setRenderer(new TemplateListCellRenderer(template))
                } else if (node instanceof JList) {
                    node.setCellRenderer(new TemplateListCellRenderer(template))
                } else if (node instanceof TagChooser) {
                    node.setTemplateRenderer(template)
                } else {
                    throw new Exception("templateRenderer can't be applied to $node")
                }
            }
        }
    ]



}
