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
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

/**
 * Gant script that creates an empty domain class and register it in persistence.xml.
 * Using JPA in desktop application requires domain classes to be registered in persistence.xml.
 *
 */

includeTargets << griffonScript("_GriffonCreateArtifacts")

target(name: 'createDomainClass', description: "Create an empty domain class and register it in persistence file", prehook: null, posthook: null) {

    if (argsMap?.params?.isEmpty()) {
        println '''
Usage: griffon create-domain-class domainClassName
       griffon create-domain-class domainClassName1 domainClassName2 domainClassName3 ...
       griffon create-domain-class domainClassName1,domainClassName2,domainClassName3,...

Parameter: domainClassName is domain class name to be generated.

Example: griffon create-domain-class Student
         griffon create-domain-class Teacher Student
         griffon create-domain-class Teacher,Student
'''
        println "Can't execute create-domain-class"
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

    String persistenceXml = "${basedir}/griffon-app/conf/metainf/persistence.xml"
    File persistenceFile = new File(persistenceXml)
    if (!persistenceFile.exists()) {
        fail "$persistenceXml doesn't exists! You can generate it by executing create-persistence-file command."
    }
    def persistenceRoot = new XmlSlurper(false, false).parse(persistenceFile)

    def config = new ConfigSlurper().parse(configFile.toURL())
    String packageName = config.griffon?.simplejpa?.model?.package ?: 'domain'

    def templateFile = resolveTemplate("SimpleJpaDomainClass", ".groovy")
    if (!templateFile?.exists()) {
        println "Can't find SimpleJpaDomainClass template."
        return
    }

    domainModelName.each { param ->
        String domainClassFileName = "${basedir}/src/main/${packageName?.replace('.','/')}/${param}.groovy"
        File domainClassFile = new File(domainClassFileName)
        if (domainClassFile .exists()) {
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
