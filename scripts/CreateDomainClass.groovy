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

import groovy.text.GStringTemplateEngine
import groovy.xml.XmlUtil
import griffon.util.ConfigUtils

/**
 * Gant script that creates an empty domain class and register it in persistence.xml.
 * Using JPA in desktop application requires domain classes to be registered in persistence.xml.
 *
 */

includeTargets << griffonScript("_GriffonCreateArtifacts")

target(name: 'createDomainClass', description: "Create an empty domain class and register it in persistence file", prehook: null, posthook: null) {

    def config = new ConfigSlurper().parse(configFile.toURL())
    String packageName = ConfigUtils.getConfigValueAsString(config, 'griffon.simplejpa.model.package', 'domain')

    def helpDescription = """
DESCRIPTION
    create-domain-class

    Create a new domain class and register it in persistence file.

SYNTAX
    create-domain-class [domainClassName]
    create-domain-class [domainClassName] [domainClassName] ...
    create-domain-class [domainClassName],[domainClassName], ...

ARGUMENTS
    domainClassName
        This is the name of domain class.

DETAILS
    Before you can create a domain class, please make sure you have setup your
    project for using JPA.  This command requires the existence of
    persistence.xml in your project.You can setup your project to use JPA by
    invoking griffon create-simple-jpa command.

    Domain class will be generated in the package specified by
    griffon.simplejpa.model.package value in Config.groovy.  The default value
    for package is 'domain'.

    If you want to change the default template used by this command, you can
    execute griffon install-templates command and alter
    SimplaJpaDomainClass.groovy manually.

EXAMPLES
    griffon create-domain-class Student
    griffon create-domain-class Teacher Student
    griffon create-domain-class Teacher,Student

CONFIGURATIONS
    griffon.simplejpa.model.package.value = $packageName
"""

    if (argsMap?.params?.isEmpty() || argsMap['info']) {
        println helpDescription
        return
    }

    String persistenceXml = "${basedir}/griffon-app/conf/metainf/persistence.xml"
    File persistenceFile = new File(persistenceXml)
    if (!persistenceFile.exists()) {
        println """

Before you can create domain classes, you must prepare your project to use JPA.
The following file is required by JPA project, but we can't find it:
$persistenceXml.
You can setup your project to use JPA by executing this command:
griffon create-simple-jpa

$helpDescription
"""
        return
    }

    List domainModelName = []
    argsMap.params.each { String param ->
        String[] temp = [param];
        if (param.contains(",")) {
            temp = param.split(",")
        } else if (param.contains(";")) {
            temp = param.split(";")
        }
        temp.each {
            domainModelName << it.replaceAll("[^A-Za-z0-9\$_]", ' ').trim()
        }
    }

    def persistenceRoot = new XmlSlurper(false, false).parse(persistenceFile)



    def templateFile = resolveTemplate("SimpleJpaDomainClass", ".groovy")
    if (!templateFile?.exists()) {
        println "Can't find SimpleJpaDomainClass template."
        return
    }

    domainModelName.each { param ->
        String domainClassFileName = "${basedir}/src/main/${packageName?.replace('.','/')}/${param}.groovy"
        File domainClassFile = new File(domainClassFileName)
        if (domainClassFile.exists()) {
            fail("File $domainClassFileName already exists")
        }
        print "Creating file $domainClassFileName... "

        domainClassFile.getParentFile().mkdirs()
        domainClassFile.createNewFile()

        def template = new GStringTemplateEngine().createTemplate(templateFile.file)
        def binding = ['packageName': packageName, 'className': param]
        String result = template.make(binding)
        domainClassFile.write(result)

        println "OK"

        String fullClassName = "${packageName}.${param}"
        if (!persistenceRoot."persistence-unit".'class'.find{it=="$fullClassName" }.isEmpty()) {
            println "$fullClassName already listed in persistence file!"
        } else {
            persistenceRoot."persistence-unit"."provider" + {
                'class'("${packageName}.${param}")
            }
        }
    }

    // Save persistence.xml
    XmlUtil.serialize(persistenceRoot, new FileOutputStream(persistenceFile))
    println "${persistenceXml} has been updated."
}

setDefaultTarget(createDomainClass)
