griffon.project.dependency.resolution = {
    // implicit variables
    // pluginName:     plugin's name
    // pluginVersion:  plugin's version
    // pluginDirPath:  plugin's install path
    // griffonVersion: current Griffon version
    // groovyVersion:  bundled groovy
    // springVersion:  bundled Spring
    // antVertsion:    bundled Ant
    // slf4jVersion:   bundled Slf4j

    // inherit Griffon' default dependencies
    inherits("global") {
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        griffonHome()

        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        mavenLocal()
        mavenCentral()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"

        // pluginDirPath is only available when installed
        // String basePath = pluginDirPath? "${pluginDirPath}/" : ''
        // flatDir name: "${pluginName}LibDir", dirs: ["${basePath}lib"]
    }
    dependencies {
        // specify dependencies here under either 'scripts.build', 'compile', 'runtime' or 'test' scopes eg.

        compile 'org.hibernate.javax.persistence:hibernate-jpa-2.0-api:1.0.1.Final'
        compile 'javax.validation:validation-api:1.0.0.GA'
        compile 'org.jadira.usertype:usertype.jodatime:2.0.1'
        compile 'joda-time:joda-time:2.1'
        runtime 'org.hibernate:hibernate-entitymanager:4.1.9.Final'
        runtime 'org.hibernate:hibernate-validator:4.3.0.Final'

        // automatically depends on these libraries for quick start, but user may not want them.
        runtime 'mysql:mysql-connector-java:5.1.20'
        compile 'net.java.dev.glazedlists:glazedlists_java15:1.9.0'
    }
}

griffon {
    doc {
        logo = '<a href="http://griffon-framework.org" target="_blank"><img alt="The Griffon Framework" src="../img/griffon.png" border="0"/></a>'
        sponsorLogo = "<br/>"
        footer = "<br/><br/>Made with Griffon (@griffon.version@)"
    }
}

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '%d [%t] %-5p %c - %m%n')
    }
    error 'org.codehaus.griffon',
          'org.springframework',
          'org.apache.karaf',
          'groovyx.net',
          'org.hibernate'
    warn  'griffon'
}