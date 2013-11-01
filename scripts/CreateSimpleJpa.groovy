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

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

/**
 * Gant script that creates a simple persistence.xml to be configured later.
 *
 */
final Map JPA_PROVIDERS = [
    'hibernate':  'org.hibernate:hibernate-entitymanager:4.2.0.Final'
]

final Map JDBC_DRIVERS = [
    'mysql': 'mysql:mysql-connector-java:5.1.20'
]

final List COMMON_DEPENDENCIES = [
    'org.hibernate:hibernate-validator:4.3.0.Final'
]

target(name: 'createSimpleJpa', description: "Creates persistence.xml, orm.xml and validation messages if not present", prehook: null, posthook: null) {

    def helpDescription = """
DESCRIPTION
    create-simple-jpa

    Prepare this project to use Java Persistence API (JPA).

SYNTAX
    create-simple-jpa -user=[databaseUser] -password=[databasePassword]
        -database=[databaseName] -rootPassword=[databaseRootPassword]
        -provider=[JPAProvider] -jdbc=[databaseType]
    create-simple-jpa -user=[databaseUser] -password=[databasePassword]
        -database=[databaseName] -provider=[JPAProvider]
        -jdbc=[databaseType] -skipDatabase

ARGUMENTS
    user
        This is the name of database user.  This project will establish a
        connection to database by using the specified user name.
        MySQL Only:
            If user name doesn't exists, it will be created automatically.

    password
        This is the password used when establishing connection to the
        database.
        MySQL Only:
            User with this password will be create if it doesn't exists.

    database
        This is the database name or schema name.
        MySQL Only:
            If this database doesn't exists, it will be created automatically.
            Specified user will also be granted privilleges to use
            this database.

    rootPassword
        MySQL Only:
            To create user & database and grants privilleges, this command
            will require password for MySQL root user.  While user name and
            password is saved in persistence.xml for establishing connection,
            root password will never be stored in project files.

    provider
        Specifies JPA provider that will be used.  The default value for
        this parameter is 'hibernate'.
        Available values:
            hibernate - Use Hibernate JPA.

    databaseType
        Specifies JDBC driver that will be used.  The default value for
        this parameter is 'mysql'.
        Available values:
            mysql - Use MySQL JDBC.

    skipDatabase
        Don't create user and database automatically.  This command will
        only write to persistence.xml and assume required database objects
        for establishing database connection are available.

DETAILS
    This command is usually the first command that you will need to invoke
    before working with Java Persistence API (JPA) in your project.
    It will create persistence.xml and orm.xml in current project.  It will
    also create some resource files that are commonly required when working
    with JPA.

    This command assume you're using MySQL Server database.  If you're using
    another database, you should always add -skipDatabase argument when
    invoking this command.  You will need to modify persistence.xml manually.
    You will also need to add required JDBC driver for your database.

    *NOTE*:  In the future, this command will allow user to select database
    type, JPA version and JPA implementation!

EXAMPLES
    griffon create-simple-jpa -user=steven -password=12345 -database=sample
        -rootPassword=secret

    griffon create-simple-jpa -user=scott -password=tiger -database=ha
        -skip-database
"""

    if (argsMap['info']) {
        println helpDescription
        return
    }

    if ((argsMap['user']==null || argsMap['password']==null || argsMap['database']==null) ||
        ((argsMap['root-password']==null && argsMap['rootPassword']==null) &&
         (argsMap['skip-database']==null && argsMap['skipDatabase']==null))) {
        println '''

You didn't specify all required arguments.  Please see the following
description for more information.

'''
        println helpDescription
        return
    }

    String databaseName = argsMap.database ?: "database"
    String user = argsMap.user ?: "user"
    String password = argsMap.password ?: "password"
    String rootPassword = argsMap['root-password'] ?: (argsMap['rootPassword'] ?: '')
    String jpaProvider = argsMap.provider ?: 'hibernate'
    String databaseType = argsMap.jdbc ?: 'mysql'
    boolean skipDatabase = argsMap['skip-database']==true || argsMap['skip-database']=="true" ? true:
        (argsMap['skip-database']==true || argsMap['skipDatabase']=="true" ? true : false)

    String persistenceXml = "${basedir}/griffon-app/conf/metainf/persistence.xml"
    File file = new File(persistenceXml)
    if (file.exists()) {
        println "Will not create a new file because $persistenceXml already exists!"
    } else {

        if (!skipDatabase) {
            Connection cn
            ResultSet rs
            Statement stmt
            try {
                cn = DriverManager.getConnection("jdbc:mysql://localhost", "root", rootPassword)
                stmt = cn.createStatement()

                // Check database
                rs = cn.getMetaData().getCatalogs()
                boolean found = false
                while (rs.next()) {
                    if (rs.getString(1).equals(databaseName)) {
                        println "Database $databaseName already exists. Will not create a new database."
                        found = true
                        break
                    }
                }
                if (!found) {
                    stmt.execute("CREATE DATABASE ${databaseName}")
                    println "Database $databaseName created successfully!"
                }

                // Check if user already exists
                rs = stmt.executeQuery("SELECT user, host FROM mysql.user")
                found = false
                host = 'localhost'
                while (rs.next()) {
                    if (rs.getString(1).equals(user)) {
                        println "User $user already exists. Will not create a new user."
                        host = rs.getString(2)
                        found = true
                        break
                    }
                }
                if (!found) {
                    stmt.execute("CREATE USER `${user}`@'${host}' IDENTIFIED BY '${password}'")
                    println "User $user created successfully!"
                }

                // Grant privilleges
                println "Granting privileges..."
                stmt.execute("GRANT ALL ON ${databaseName}.* TO `$user`@'${host}'")
                println "Privileges on $databaseName granted to $user!"

            } catch (Exception ex) {
                ex.printStackTrace()
                fail "Can't create new database! ${ex.getMessage()}"
            } finally {
                rs?.close()
                stmt?.close()
                cn?.close()
            }
        }

        String content = """\
<?xml version="1.0" encoding="UTF-8" ?>
<persistence xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd"
    version="2.0" xmlns="http://java.sun.com/xml/ns/persistence">
    <persistence-unit name="default" transaction-type="RESOURCE_LOCAL">
        <provider>org.hibernate.ejb.HibernatePersistence</provider>
        <properties>
            <property name="javax.persistence.jdbc.driver"   value="com.mysql.jdbc.Driver" />
            <property name="javax.persistence.jdbc.url"      value="jdbc:mysql://localhost/@database.name@" />
            <property name="javax.persistence.jdbc.user"     value="@database.user@" />
            <property name="javax.persistence.jdbc.password" value="@database.password@" />
            <property name="hibernate.connection.autocommit" value="false" />
            <property name="hibernate.dialect" value="org.hibernate.dialect.MySQL5Dialect" />
            <property name="hibernate.hbm2ddl.auto" value="create-drop" />
            <property name="jadira.usertype.autoRegisterUserTypes" value="true"/>
        </properties>
    </persistence-unit>
</persistence>
"""
        content = content.replaceAll("@database.name@", databaseName).replaceAll("@database.user@", user)
                    .replaceAll("@database.password@", password)
        file.withWriter {
            it.write content.bytes
        }
        println "File $persistenceXml created succesfully! Please modify this file according to your environments."
    }

    String ormXml = "${basedir}/griffon-app/conf/metainf/orm.xml"
    file = new File(ormXml)
    if (file.exists()) {
        println "Will not create a new file because $ormXml already exists!"
    } else {
        content = """\
<?xml version="1.0" encoding="UTF-8"?>
<entity-mappings version="2.0" xmlns="http://java.sun.com/xml/ns/persistence/orm"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://java.sun.com/xml/ns/persistence/orm orm_2_0.xsd">

<persistence-unit-metadata>
    <persistence-unit-defaults>
        <entity-listeners>
            <entity-listener class="simplejpa.AuditingEntityListener" />
        </entity-listeners>
    </persistence-unit-defaults>
</persistence-unit-metadata>

</entity-mappings>
"""
        file.withWriter {
            it.write content.bytes
        }
        println "File $ormXml created succesfully! Please modify this file according to your environments."
    }

    String validationMessages = "${basedir}/griffon-app/i18n/ValidationMessages.properties"
    file = new File(validationMessages)
    if (file.exists()) {
        println "Will not create a new file because $validationMessages already exists!"
    } else {
        content = """\

javax.validation.constraints.AssertFalse.message = must be false
javax.validation.constraints.AssertTrue.message  = must be true
javax.validation.constraints.DecimalMax.message  = must be less than or equal to {value}
javax.validation.constraints.DecimalMin.message  = must be greater than or equal to {value}
javax.validation.constraints.Digits.message      = numeric value out of bounds (<{integer} digits>.<{fraction} digits> expected)
javax.validation.constraints.Future.message      = must be in the future
javax.validation.constraints.Max.message         = must be less than or equal to {value}
javax.validation.constraints.Min.message         = must be greater than or equal to {value}
javax.validation.constraints.NotNull.message     = may not be null
javax.validation.constraints.Null.message        = must be null
javax.validation.constraints.Past.message        = must be in the past
javax.validation.constraints.Pattern.message     = must match "{regexp}"
javax.validation.constraints.Size.message        = size must be between {min} and {max}

org.hibernate.validator.constraints.CreditCardNumber.message = invalid credit card number
org.hibernate.validator.constraints.Email.message            = not a well-formed email address
org.hibernate.validator.constraints.Length.message           = length must be between {min} and {max}
org.hibernate.validator.constraints.NotBlank.message         = may not be empty
org.hibernate.validator.constraints.NotEmpty.message         = may not be empty
org.hibernate.validator.constraints.Range.message            = must be between {min} and {max}
org.hibernate.validator.constraints.SafeHtml.message         = may have unsafe html content
org.hibernate.validator.constraints.ScriptAssert.message     = script expression "{script}" didn't evaluate to true
org.hibernate.validator.constraints.URL.message              = must be a valid URL
org.hibernate.validator.constraints.br.CNPJ.message          = invalid Brazilian corporate taxpayer registry number (CNPJ)
org.hibernate.validator.constraints.br.CPF.message           = invalid Brazilian individual taxpayer registry number (CPF)
org.hibernate.validator.constraints.br.TituloEleitor.message = invalid Brazilian Voter ID card number

simplejpa.converter.toInteger = must be a number

"""
        file.withWriter {
            it.write content.bytes
        }
        println "File $validationMessages created succesfully!"
    }

    def config = new ConfigSlurper().parse(configFile.toURL())
    def basenames = config?.resources?.basenames ?: []
    if (basenames.contains('ValidationMessages')) {
        println "Will not add new entry to $configFile because it is already exists!"
    } else {
        basenames << "messages"
        basenames << "ValidationMessages"
        def configText = configFile.text.split('\n').grep { !it.contains("i18n.basenames") }.join('\n')
        configText += "\ni18n.basenames = ['${basenames.join("','")}']"
        configFile.withWriter { writer ->
            writer.write configText
        }
        println "File $configFile successfully updated!"
    }

    //
    // Add dependencies to project
    //

    def buildConfigFile = new File("${basedir}/griffon-app/conf/BuildConfig.groovy")
    def buildConfigText = buildConfigFile.text

    def dependencies = []
    dependencies.addAll(COMMON_DEPENDENCIES)
    if (!JPA_PROVIDERS[jpaProvider]) {
        println "JPA Provider: $jpaProvider is not supported.  You will need to add dependency to this provider manually by editing $buildConfigFile."
    } else {
        dependencies << JPA_PROVIDERS[jpaProvider.toLowerCase()]
    }
    if (!JDBC_DRIVERS[databaseType]) {
        println "JDBC Driver: $databaseType is not supported.  You will need to add dependency to this JDBC driver manually by editing $buildConfigFile."
    } else {
        dependencies << JDBC_DRIVERS[databaseType.toLowerCase()]
    }

    dependencies.each { String dependency ->
        if (buildConfigText =~ /(?s)\s*$dependency\s*/)  {
            println "The following dependency wasn't added because it is already exists: $dependency"
        } else {
            println "Add new dependency: $dependency"
            buildConfigText = buildConfigText.replaceAll(/\s*dependencies\s*\{/, """
    dependencies {
        runtime '$dependency'""")
        }
    }

    buildConfigFile.withWriter {
        it.write buildConfigText
    }

    println "File $buildConfigFile successfully updated!"

}

setDefaultTarget(createSimpleJpa)