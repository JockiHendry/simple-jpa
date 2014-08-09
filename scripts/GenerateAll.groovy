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

import simplejpa.scaffolding.Scaffolding
import griffon.util.*

/**
 * Gant script that creates a new MVC Group with view, controller and model that performs CRUD operation for
 * specified domain class.
 *
 */

includeTargets << griffonScript("_GriffonCreateArtifacts")
includeTargets << griffonScript('_GriffonBootstrap')

target(name: 'generateAll', description: "Create CRUD scaffolding for specified domain class", prehook: null, posthook: null) {
    depends(setupApp)
    def config = new ConfigSlurper().parse(configFile.toURL())
    String domainPackageName = ConfigUtils.getConfigValueAsString(config, 'griffon.simplejpa.model.package', 'domain')
    boolean alwaysExcludeSoftDeleted = ConfigUtils.getConfigValueAsBoolean(config, 'griffon.simplejpa.finders.alwaysExcludeSoftDeleted', false)

    def helpDescription = """
DESCRIPTION
    generate-all

    Generate an MVCGroup with scaffolding code.

SYNTAX
    griffon generate-all * [-generatedPackage=value] [-forceOverwrite]
        [-setStartup] [-skipExcel] [-startupGroup=value] [-generator=generator]
    griffon generate-all [domainClassName] [-generatedPackage=value]
        [-forceOverwrite] [-setStartup] [-skipExcel] [-startupGroup=value]
        [-generator=generator]
    griffon generate-all [domainClassName] [domainClassName] ...
        [-generatedPackage=value] [-forceOverwrite] [-setStartup]
        [-skipExcel] [-startupGroup=value] [-generator=generator]

ARGUMENTS
    *
        This command will process all domain classes.

    domainClassName
        This is the name of domain class the scaffolding result will based on.

    generatedPackage (optional)
        By default, generate-all will place the generated files in package
        'project'.  You can set the generated package name by using this
        argument.

    forceOverwrite (optional)
        If this argument is present, generate-all will replace all existing
        files without any notifications.

    setStartup (optional)
        Set the generated MVCGroup as startup group (the MVCGroup that will
        be launched when program starts).  If this argument is present when
        using generating more than one MVCGroup, then the last MVCGroup will
        be set as startup group.

    skipExcel (optional)
        If this argument is present, generate-all will not create Microsoft
        Excel file for integration testing (DbUnit).

    startupGroup (optional)
        Generate a distinct MVCGroup that serves as startup group.  This
        MVCGroup will act as a container for the other domain classes' based
        MVCGroups.
        If this argument is present together with setStartup argument, then
        the setStartup argument will have no effect.

    generator (optional)
        The generator class that will be used to generate files.  Default
        generator is simplejpa.scaffolding.generator.basic.BasicGenerator.


DETAILS
    This command will generate scaffolding MVC based on a domain class. It
    will also generate a startup MVCGroup that act as container for the
    domain class based MVCGroup.

    generate-all will find domain classes in the package specified by
    griffon.simplejpa.model.package in Config.groovy.  The default value for
    package is 'domain'.

    The value of griffon.simplejpa.finders.alwaysExcludeSoftDeleted will have
    impact to the generated controller classes.  If you change this
    configuration value after generating domain classes, than you will need
    to alter the generated controllers manually.

    If you want to change the default template used by this command, you can
    execute griffon install-templates command and alter the generated
    template files.

EXAMPLES
    griffon generate-all *
    griffon generate-all * -forceOverwrite -setStartup
    griffon generate-all * -forceOverwrite -generator=com.my.CustomGenerator
    griffon generate-all Student Teacher Classroom
    griffon generate-all Student -startupGroup=MainGroup
    griffon generate-all -startupGroup=MainGroup

CONFIGURATIONS
    griffon.simplejpa.model.package = $domainPackageName
    griffon.simplejpa.finders.alwaysExcludeSoftDeleted = $alwaysExcludeSoftDeleted
"""
    if (argsMap['info']) {
        println helpDescription
        return
    }

    if (argsMap?.params?.isEmpty() && !(argsMap['startup-group'] || argsMap['startupGroup'])) {
        println '''

You didn't specify all required arguments.  Please see the following
description for more information.

'''
        println helpDescription
        return
    }

    Scaffolding scaffolding = new Scaffolding(config)
    if (argsMap['generator']) {
        scaffolding.generatorClass = argsMap['generator']
    }
    if (argsMap['generatedPackage']) {
        scaffolding.generatedPackage = argsMap['generatedPackage']
    }
    if (argsMap['startupGroup']) {
        scaffolding.startupGroupName = argsMap['startupGroup']
    }
    if (argsMap['ignoreLazy']) {
        scaffolding.ignoreLazy = argsMap.containsKey('ignoreLazy')
    }
    if (argsMap['forceOverwrite']) {
        scaffolding.forceOverwrite = argsMap.containsKey('forceOverwrite')
    }
    if (argsMap['skipExcel']) {
        scaffolding.skipExcel = argsMap.containsKey('skipExcel')
    }
    if (argsMap['setStartup']) {
        scaffolding.setStartup = argsMap['setStartup']
    }
    scaffolding.domainClassesToGenerate = argsMap.params

    scaffolding.generate()

    println "\nConfiguring additional files...\n"

    File validationFile = new File("${basedir}/griffon-app/i18n/messages.properties")
    ["simplejpa.dialog.save.button": "Save",
     "simplejpa.dialog.cancel.button": "Cancel",
     "simplejpa.dialog.delete.button": "Delete",
     "simplejpa.dialog.update.button": "Update",
     "simplejpa.dialog.close.button": "Close",
     "simplejpa.search.all.message": "Display all data",
     "simplejpa.search.result.message": "Display {0} search result for {1}",
     "simplejpa.error.alreadyExist.message": "already registered!",
     "simplejpa.dialog.delete.message": "Do you really want to delete this?",
     "simplejpa.dialog.delete.title": "Delete Confirmation",
     "simplejpa.dialog.update.message": "Do you really want to update this?",
     "simplejpa.dialog.update.title": "Update Confirmation",
     "simplejpa.search.label": "Search",
     "simplejpa.search.all.label": "Display All"].each { k, v ->
        if (!validationFile.text.contains(k)) {
            println "Adding $k to message.properties..."
            validationFile << "\n$k = $v"
        }
    }

    File eventsFile = new File("${basedir}/griffon-app/conf/Events.groovy")
    if (eventsFile.exists() && !scaffolding.forceOverwrite) {
        println "Didn't change $eventsFile."
    } else {
        if (!eventsFile.exists()) {
            println "Creating file $eventsFile..."
            eventsFile.createNewFile()
        }
        if (!eventsFile.text.contains("onUncaughtExceptionThrown")) {
            eventsFile << """\n
onUncaughtExceptionThrown = { Exception e ->
    if (e instanceof org.codehaus.groovy.runtime.InvokerInvocationException) e = e.cause
    javax.swing.JOptionPane.showMessageDialog(null, e.message, "Error", javax.swing.JOptionPane.ERROR_MESSAGE)
}
"""
            println "$eventsFile has been modified."
        }
    }

    println ""
}

setDefaultTarget(generateAll)