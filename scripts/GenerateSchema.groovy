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


import griffon.core.*
import griffon.util.*

import javax.persistence.Persistence

includeTargets << griffonScript("_GriffonBootstrap")

target(name: 'generateschema',
        description: "Generate database schema based on domain models",
        prehook: null, posthook: null) {
    depends(createConfig)

    def helpDescription = """
DESCRIPTION
    generate-schema

    Generate database schema based on current domain models mapping.

SYNTAX
    generate-schema -target=database -action=[action]
    generate-schema -target=database -action=[action] -data=[script.sql]
    generate-schema -target=script -action=[action] -dropTarget=[script.sql]
                    -createTarget=[script.sql]

ARGUMENTS
   target
       Can be one of 'database' or 'script'.  If target value is 'database',
       this script will create database objects in the database.  If target
       value is 'script', this script will generate SQL scripts that can
       be executed in the database later.

   action
       Valid values are none, create, drop-and-create, or drop.

   data
       If this argument exists, an SQL script will be executed after
       database objects are created.  The purpose of this script is to
       initialize database (for example, populating tables with initial data).

   dropTarget
       This only works if target is 'script'. This is the file that will be
       generated and contains DDL DROP scripts.

   createTarget
       This only works if target is 'script'. This is the file that will be
       generated and contains DDL CREATE scripts.

DETAILS
    This command can be used to generate database objects in database based
    on current domain models.  It also supports generating database objects
    as SQL scripts that can be executed later.

EXAMPLES
    griffon generate-schema -target=database -action=drop-and-create
    griffon generate-schema -target=script -action=drop-and-create
                            -dropTarget=drop.sql -createTarget=target.sql
"""

    if (argsMap['params']=='info') {
        println helpDescription
        return
    }

    if (argsMap['target']==null || argsMap['action']==null) {
        println '''

You didn't specify all required arguments.  Please see the following
description for more information.

'''
        println helpDescription
        return
    }

    jardir = ant.antProject.replaceProperties(buildConfig.griffon.jars.destDir)
    ant.copy(todir: jardir) { fileset(dir: "${griffonHome}/lib/", includes: "jline-*.jar") }

    bootstrap()
    //griffonApp.startup()

    Map props = [:]
    if (argsMap['target']=='database') {
        props['javax.persistence.schema-generation.database.action'] = argsMap['action']
        if (argsMap['data']) {
            props['javax.persistence.sql-load-script-source'] = argsMap['data']
        }
    } else if (argsMap['target']=='script') {
        props['javax.persistence.schema-generation.scripts.action'] = argsMap['action']
        props['javax.persistence.schema-generation.scripts.drop-target'] = argsMap['dropTarget']
        props['javax.persistence.schema-generation.scripts.create-target'] = argsMap['createTarget']
    }

    Persistence.generateSchema('default', props)

    println "Scheme generation is completed successfully!"

}

setDefaultTarget('generateschema')
