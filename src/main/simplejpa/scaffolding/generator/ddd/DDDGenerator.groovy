package simplejpa.scaffolding.generator.ddd

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.scaffolding.DomainClass
import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.CollectionAttribute
import simplejpa.scaffolding.attribute.EntityAttribute
import simplejpa.scaffolding.generator.basic.BasicGenerator
import griffon.util.*

class DDDGenerator extends BasicGenerator {

    private static Logger log = LoggerFactory.getLogger(DDDGenerator)

    String repositoryVar
    String repositoryType

    DDDGenerator(Scaffolding scaffolding) {
        super(scaffolding)

        attributeGenerators['BasicAttribute'] = DDDBasicAttributeGenerator
        attributeGenerators['CollectionAttribute'] = DDDCollectionAttributeGenerator
        attributeGenerators['DateAttribute'] = DDDDateAttributeGenerator
        attributeGenerators['EntityAttribute'] = DDDEntityAttributeGenerator
        attributeGenerators['EnumeratedAttribute'] = DDDEnumeratedAttributeGenerator
        attributeGenerators['UnknownAttribute'] = DDDUnknownAttributeGenerator
    }

    @Override
    void generate(DomainClass domainClass) {
        repositoryVar = "${domainClass.nameAsProperty}Repository"
        repositoryType = "${domainClass.name}Repository"

        if (domainClass.annotations.find { it.name == 'Entity' }) {

            super.generate(domainClass, 'SimpleJpaModel', 'SimpleJpaView', 'SimpleJpaDDDController')

            // Generate repository
            init(domainClass, "${domainClass.name}Repository")
            generateArtifact('SimpleJpaRepository',
                "${BuildSettingsHolder.settings.baseDir}/src/main/${domainClass.packageName.replace('.', '/')}",
                "${repositoryType}.groovy",
            )
        }
    }

    @Override
    void generateExtra(Map<String, DomainClass> domainClasses) {
        // Disable injection to controller
        File configFile = new File("${BuildSettingsHolder.settings.baseDir}/griffon-app/conf/Config.groovy")
        String configText = configFile.text
        def config = new ConfigSlurper().parse(configText)
        if (config.griffon.simplejpa.finders.injectInto instanceof Map) {
            configFile.append("${System.lineSeparator()}griffon.simplejpa.finders.injectInto = []")
        }

        // Add injection event
        File eventFile = new File("${BuildSettingsHolder.settings.baseDir}/griffon-app/conf/Events.groovy")
        if (!eventFile.exists()) {
            log.info "Creating file $eventFile..."
            eventFile.createNewFile()
        }
        if (!eventFile.text.contains('onInitializeMVCGroup')) {
            eventFile << """\n
onInitializeMVCGroup = { def configuration, def mvcGroup ->
    mvcGroup.members.each { k, v ->
        if (v instanceof griffon.core.GriffonMvcArtifact) {
            simplejpa.SimpleJpaUtil.container.each { String name, Object value ->
                v.hasProperty(name)?.setProperty(v, value)
            }
        }
    }
}
"""
            log.info "$eventFile has been modified"
        }
    }

    @Override
    void generatePair(EntityAttribute attr) {
        generate(attr.target, 'SimpleJpaPairModel', 'SimpleJpaPairView', 'SimpleJpaDDDPairController', attr.target.nameAsPair)
    }

    @Override
    void generateChild(CollectionAttribute attr) {
        generate(attr.target, 'SimpleJpaChildModel', 'SimpleJpaChildView', 'SimpleJpaDDDChildController', attr.target.nameAsChild)
    }

    public boolean hasId() {
        domainClass.annotations.find { it.name == 'DomainClass'}
    }

    public String updates(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.repo_update())
        }
        addTab(result, tab)
    }

    public String listAll_find(int tab) {
        List<String> result = []
        result << "List ${domainClassNameAsProperty}Result = ${repositoryVar}.findAll${domainClassName}()"
        domainClass.attributes.each {
            if (it.generator.respondsTo("findList")) {
                result.addAll(it.generator.findList())
            }
        }
        return addTab(result, tab)
    }

    public String delete(int tab) {
        if (scaffolding.alwaysExcludeSoftDeleted) {
            return addTab(["${repositoryVar}.softDelete(${domainClassNameAsProperty})"], tab)
        } else {
            return addTab(["${repositoryVar}.remove(${domainClassNameAsProperty})"], tab)
        }
    }

    public String domainClassConstructor() {
        List<String> result = []
        if (hasId()) {
            result << "id: model.id"
        }
        domainClass.attributes.each {
            result.addAll(it.generator.constructor())
        }
        "new ${domainClassName}(${result.join(', ')})"
    }



}
