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
    boolean alwaysExcludeSoftDeleted = false
    String generatedPackage = 'project'
    String startupGroupName = null
    File persistenceFile = null
    boolean ignoreLazy = true
    boolean forceOverwrite = false
    boolean skipExcel = false
    boolean setStartup = false
    String dateTimeStyle = null
    List domainClassesToGenerate = []
    Map<String, DomainClass> domainClasses = [:]

    public Scaffolding(def config = null) {
        if (config) {
            setDomainPackageName(ConfigUtils.getConfigValueAsString(config, 'griffon.simplejpa.model.package', 'domain'))
            setAlwaysExcludeSoftDeleted(ConfigUtils.getConfigValueAsBoolean(config, 'griffon.simplejpa.finders.alwaysExcludeSoftDeleted', false))
            setGeneratorClass(ConfigUtils.getConfigValueAsString(config, 'griffon.simplejpa.scaffolding.generator', null))
            setGeneratedPackage(ConfigUtils.getConfigValueAsString(config, 'griffon.simplejpa.scaffolding.generatedPackage', 'project'))
            setStartupGroupName(ConfigUtils.getConfigValueAsString(config, 'griffon.simplejpa.scaffolding.startupGroup', null))
            setIgnoreLazy(ConfigUtils.getConfigValueAsBoolean(config, 'griffon.simplejpa.scaffolding.ignoreLazy', true))
            setForceOverwrite(ConfigUtils.getConfigValueAsBoolean(config, 'griffon.simplejpa.scaffolding.forceOverwrite', false))
            setSkipExcel(ConfigUtils.getConfigValueAsBoolean(config, 'griffon.simplejpa.scaffolding.skipExcel', false))
            setDateTimeStyle(ConfigUtils.getConfigValueAsString(config, 'griffon.simplejpa.scaffolding.dateTimeStyle', null))
            String target = ConfigUtils.getConfigValueAsString(config, 'griffon.simplejpa.scaffolding.target', '*')
            domainClassesToGenerate = target.split(',')
        }
        persistenceFile = new File("${BuildSettingsHolder.settings.baseDir}/griffon-app/conf/metainf/persistence.xml")
    }

    public populateDomainClasses() {
        if (persistenceFile == null || !persistenceFile.exists()) {
            throw new RuntimeException("File persistence.xml is not specified!")
        }
        def xml = new XmlSlurper(false, false).parse(persistenceFile)
        def files = []
        xml."persistence-unit".'class'.each {
            String domainClass = it.text()
            files << [className: domainClass,
                      file: "${BuildSettingsHolder.settings.baseDir}/src/main/${domainClass.replace('.','/')}.groovy"]
        }
        populateDomainClasses(files)
    }

    public populateDomainClasses(List<String> files) {
        //
        // First phase:  Initialize all domain classes
        //
        domainClasses.clear()
        files.each {
            File file = new File(it.'file')
            String className = it.'className'
            if (!file.exists()) {
                log.error "Skipped a domain class from persistence file because can't find $file!"
                return
            }
            String currentDomainPackageName = domainPackageName
            String currentGeneratedPackage = generatedPackage
            if (className.startsWith(domainPackageName)) {
                def tmp = className.split('\\.')
                if (tmp.length > 2) {
                    currentDomainPackageName = tmp[0..tmp.length - 2].join('.')
                    currentGeneratedPackage = generatedPackage + '.' + (tmp[0] == domainPackageName?
                        tmp[1..tmp.length - 2].join('.'): tmp[0..tmp.length - 2].join('.'))
                }
            }
            DomainClass domainClass = new DomainClass(file, currentDomainPackageName, currentGeneratedPackage)
            try {
                domainClass.sourceClass = Class.forName(className)
            } catch (Exception ex) {
                log.error "Can't load class [$className]"
            }
            domainClasses[domainClass.name] = domainClass
        }
        domainClasses.each { String name, DomainClass domainClass ->
            // Add inherited attributes
            DomainClass parent = domainClasses[domainClass.parentClassName]
            while (parent) {
                parent.attributes.each {
                    if (!domainClass.attributes.contains(it)) {
                        domainClass.attributes.add(0, it)
                    }
                }
                parent = parent.parentClassName? domainClasses[parent.parentClassName]: null
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
                        log.error "Can't find type [$attribute.targetType] for collection relation [${domainClass.name}.${attribute.name}] in persistence.xml!"
                        return
                    }
                    if (attribute.target.attributes.find { it instanceof EntityAttribute && it.type == name } != null) {
                        attribute.bidirectional = true
                    }
                } else if (attribute instanceof EntityAttribute) {
                    attribute.target = domainClasses[attribute.type]
                    if (attribute.target == null) {
                        log.error "Can't find type [$attribute.type] for attribute [${domainClass.name}.${attribute.name}] in persistence.xml!"
                        return
                    }
                }
            }
        }
    }

    public void generate() {
        populateDomainClasses()
        if (!domainClassesToGenerate?.empty) {
            if (domainClassesToGenerate[0] == '*') {
                domainClassesToGenerate = domainClasses.keySet().toList()
            }
            domainClassesToGenerate.each { generate(it) }
        }
        generateStartupGroup()
        generateExtra()
    }

    public void generate(String domainClassName) {
        DomainClass domainClass = domainClassName.contains('.')?
            domainClasses.find { k,v -> v.sourceClass.name == domainClassName}.value:
            domainClasses[domainClassName]
        if (domainClass==null) {
            log.error "Can't find $domainClassName in persistence.xml!  Nothing will be generated for this entry!"
        } else {
            generator.generate(domainClass)
        }
        generateExtra()
    }

    public void generateStartupGroup() {
        if (startupGroupName == null) {
            log.info "Didn't generate startup group because startupGroupName is not defined!"
            return
        }
        generator.generateStartupGroup(domainClasses)
    }

    public void generateExtra() {
        generator.generateExtra(domainClasses)
    }

    public void setStartupGroupName(String startupGroupName) {
        this.startupGroupName = startupGroupName?.capitalize()
    }

    public void setGeneratorClass(String generatorClass) {
        this.generatorClass = (generatorClass==null || generatorClass.isAllWhitespace())?
            'simplejpa.scaffolding.generator.basic.BasicGenerator': generatorClass
        Class c = ApplicationClassLoader.get().loadClass(this.generatorClass)
        generator = c.newInstance([this].toArray())
    }

}
