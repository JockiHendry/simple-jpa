package simplejpa.scaffolding

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.scaffolding.attribute.Attribute
import simplejpa.scaffolding.attribute.CollectionAttribute
import simplejpa.scaffolding.attribute.EntityAttribute
import simplejpa.scaffolding.generator.Generator
import simplejpa.scaffolding.generator.basic.BasicGenerator
import griffon.util.*

class Scaffolding {

    private static Logger log = LoggerFactory.getLogger(Scaffolding)

    String generatorClass = 'simplejpa.scaffolding.generator.basic.BasicGenerator'
    Generator generator = new BasicGenerator(this)
    String domainPackageName = 'domain'
    String alwaysExcludeSoftDeleted = 'N'
    String generatedPackage = 'project'
    String startupGroupName = null
    File persistenceFile = null
    boolean ignoreLazy = false
    boolean forceOverwrite = false
    boolean skipExcel = false
    boolean setStartup = false
    List domainClassesToGenerate = []
    Map<String, DomainClass> domainClasses = [:]

    public void setGeneratorClass(String generatorClass) {
        this.generatorClass = generatorClass
        generator = Class.forName(generatorClass).newInstance([this].toArray())
    }

    public boolean isAlwaysExcludeSoftDeleted() {
        (alwaysExcludeSoftDeleted == 'Y' || alwaysExcludeSoftDeleted == true)? true: false
    }

    public populateDomainClasses() {
        if (domainClassesToGenerate == null || domainClassesToGenerate.isEmpty()) {
            throw new RuntimeException("Domain classes to generate can't be null!")
        }
        if (persistenceFile == null || !persistenceFile.exists()) {
            throw new RuntimeException("File persistence.xml is not specified!")
        }
        def xml = new XmlSlurper(false, false).parse(persistenceFile)
        List<String> files = []
        xml."persistence-unit".'class'.each {
            String domainClass = it.text()
            files << "${BuildSettingsHolder.settings.baseDir}/src/main/${domainClass.replace('.','/')}.groovy"
        }
        populateDomainClasses(files)
    }

    public populateDomainClasses(List<String> files) {
        //
        // First phase:  Initialize all domain classes
        //
        domainClasses.clear()
        files.each {
            File file = new File(it)
            if (!file.exists()) {
                log.error "Skipped a domain class from persistence file because can't find $file!"
                return
            }
            DomainClass domainClass = new DomainClass(file, domainPackageName, generatedPackage)
            domainClasses[domainClass.name] = domainClass
        }
        domainClasses.each { String name, DomainClass domainClass ->
            if (domainClass.parentClassName && domainClasses[domainClass.parentClassName]) {
                domainClass.attributes.addAll(0, domainClasses[domainClass.parentClassName].attributes)
            }
        }

        //
        // Second phase: Determine relation between domain classes
        //
        domainClasses.each { String name, DomainClass domainClass ->
            domainClass.attributes.each { Attribute attribute ->
                if (attribute instanceof CollectionAttribute) {
                    attribute.target = domainClasses[attribute.targetType]
                    if (attribute.target == null) {
                        log.error "Error: Can't find target of collection relation!"
                        return
                    }
                    if (attribute.target.attributes.find { it instanceof EntityAttribute && it.type == name } != null) {
                        attribute.bidirectional = true
                    }
                } else if (attribute instanceof EntityAttribute) {
                    attribute.target = domainClasses[attribute.type]
                    if (attribute.target == null) {
                        log.error "Error: Can't find target of entity relation!"
                        return
                    }
                }
            }
        }
    }

    public void generate() {
        if (!domainClassesToGenerate.empty) {
            populateDomainClasses()
            if (domainClassesToGenerate[0] == '*') {
                domainClassesToGenerate = domainClasses.keySet().toList()
            }
            domainClassesToGenerate.each { generate(it) }
        }
        generateStartupGroup()
    }

    public void generate(String domainClassName) {
        generator.generate(domainClasses[domainClassName])
    }

    public void generateStartupGroup() {
        if (startupGroupName == null) {
            log.info "Didn't generate startup group because startupGroupName is not defined!"
            return
        }
        generator.generateStartupGroup(domainClasses)
    }

}
