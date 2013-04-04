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

package scripts


import groovy.text.GStringTemplateEngine
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.codehaus.griffon.artifacts.model.Plugin

/**
 * Gant script that install simple-jpa templates to user projects.
 *
 */

includeTargets << griffonScript("_GriffonCreateArtifacts")

target(name: 'installTemplates', description: "Install templates into user projects", prehook: null, posthook: null) {

    String artifactInstallPath = "${basedir}/src/templates/artifacts"
    ant.mkdir(dir: artifactInstallPath)

    resolveResources("file:${artifactSettings.artifactBase(Plugin.TYPE)}/*/src/templates/artifacts/SimpleJpa*").each { resource ->
        println "Copying file ${resource.file} to ${artifactInstallPath}..."
        ant.copy(file: resource.file, todir: artifactInstallPath)
    }

    resolveResources("file:${artifactSettings.artifactBase(Plugin.TYPE)}/*/src/templates/artifacts/Startup*").each { resource ->
        println "Copying file ${resource.file} to ${artifactInstallPath}..."
        ant.copy(file: resource.file, todir: artifactInstallPath)
    }

    println "Simple JPA templates installed to project."

}

setDefaultTarget(installTemplates)
