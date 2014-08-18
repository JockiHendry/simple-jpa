includeTargets << griffonScript('_GriffonCreateArtifacts')

target(name: 'createRepository', description: "Creates a new repository class", prehook: null, posthook: null) {
    ant.mkdir(dir: "${basedir}/griffon-app/repositories")

    String type = 'Repository'
    promptForName(type: type)

    def name = argsMap["params"][0]

    createArtifact(
            name:   name,
            suffix: type,
            type:   type,
            path:   'griffon-app/repositories')
    doCreateUnitTest(name: name, suffix: type)
}

setDefaultTarget(createRepository)
