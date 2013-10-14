package scripts

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

eventCompileStart = { evt ->

    classpath()

    if (System.getProperty('simplejpa.precompileAST', 'true')=='true') {
        precompileAST()
    }

}


