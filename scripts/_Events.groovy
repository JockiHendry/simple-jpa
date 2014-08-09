package scripts

import griffon.util.*

includeTargets << griffonScript('_GriffonCompile')
includeTargets << griffonScript('_GriffonClasspath')

precompileAST = {
    ant.echo "Start precompile AST classes"
    ant.mkdir dir: projectMainClassesDir
    def sources = []

    new File("${basedir}/src/main").eachDirRecurse {
        if (it.name.contains('ast')) {
            ant.echo "Add AST sources $it"
            sources << it
        }
    }

    if (sources.isEmpty()) {
        ant.echo "Didn't find AST packages"
    } else {
        compileProjectSources(projectMainClassesDir, 'griffon.compile.classpath') {
            sources.each { src(path: it) }
            include(name: '*.groovy')
            include(name: '*.java')
        }
    }
}

scaffolding = {
    def config = new ConfigSlurper().parse(configFile.toURL())
    boolean autoScaffolding = ConfigUtils.getConfigValueAsBoolean(config, 'griffon.simplejpa.scaffolding.auto', false)
    if (autoScaffolding) {
        ant.echo "Running auto-scaffolding"
        def scaffolding = Class.forName('simplejpa.scaffolding.Scaffolding').newInstance([config].toArray())
        scaffolding.generate()
    }
}

eventCompileSourcesStart = { evt ->
    classpath()

    if (System.getProperty('simplejpa.precompileAST', 'true')=='true') {
        precompileAST()
    }

    // scaffolding
    if (!isPluginProject) {
        scaffolding()
    }
}