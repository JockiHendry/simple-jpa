package simplejpa.scaffolding.generator.basic

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.scaffolding.DomainClass
import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.Attribute
import simplejpa.scaffolding.attribute.CollectionAttribute
import simplejpa.scaffolding.attribute.EntityAttribute
import simplejpa.scaffolding.generator.Generator
import griffon.util.*

class BasicGenerator extends Generator {

    private static Logger log = LoggerFactory.getLogger(BasicGenerator)

    Map<String, DomainClass> domainClasses
    DomainClass domainClass
    String firstAttr
    String firstAttrAsNatural
    String firstAttrAsCapitalized
    String firstAttrSearch
    String domainClassName
    String domainClassGlazedListVariable
    String domainClassNameAsNatural
    String domainClassNameAsProperty
    String domainPackageName
    String targetPackageName
    String customClassName

    private List<String> generatedMVCGroups = []

    public BasicGenerator(Scaffolding scaffolding) {
        this.scaffolding = scaffolding
    }

    @Override
    void generateStartupGroup(Map<String, DomainClass> domainClasses) {
        this.domainClasses = domainClasses
        targetPackageName = scaffolding.generatedPackage

        log.info "Creating startup group: ${scaffolding.startupGroupName}..."

        // Generate model
        generateArtifact("StartupModel",
            "${BuildSettingsHolder.settings.baseDir}/griffon-app/models/${scaffolding.generatedPackage.replace('.', '/')}",
            "${scaffolding.startupGroupName}Model.groovy")

        // generate view
        generateArtifact("StartupView",
            "${BuildSettingsHolder.settings.baseDir}/griffon-app/views/${scaffolding.generatedPackage.replace('.', '/')}",
            "${scaffolding.startupGroupName}View.groovy")

        // generate controller
        generateArtifact("StartupController",
            "${BuildSettingsHolder.settings.baseDir}/griffon-app/controllers/${scaffolding.generatedPackage.replace('.', '/')}",
            "${scaffolding.startupGroupName}Controller.groovy")

        // create MVC group
        createMVCGroup(targetPackageName, scaffolding.startupGroupName)
        setStartupGroup(scaffolding.startupGroupName)
    }

    @Override
    void generate(DomainClass domainClass, String modelTemplate = 'SimpleJpaModel', String viewTemplate = 'SimpleJpaView',
              String controllerTemplate = 'SimpleJpaController', String customClassName = null) {

        log.info "Generating ${domainClass.name}..."

        this.domainClass = domainClass
        this.customClassName = customClassName

        // Initialize variables
        if (domainClass.attributes.isEmpty()) {
            firstAttr = "ReplaceThis"
        } else {
            firstAttr = domainClass.attributes[0].name
        }
        firstAttrSearch = "${firstAttr}Search"
        firstAttrAsNatural = GriffonNameUtils.getNaturalName(firstAttr)
        firstAttrAsCapitalized = GriffonNameUtils.capitalize(firstAttr)
        domainClassName = domainClass.name
        domainClassGlazedListVariable = GriffonNameUtils.getPropertyName(domainClassName) + "List"
        domainClassNameAsNatural = GriffonNameUtils.getNaturalName(domainClassName)
        domainClassNameAsProperty = GriffonNameUtils.getPropertyName(domainClassName)
        domainPackageName = domainClass.packageName
        targetPackageName = domainClass.targetPackage

        // Add attribute generator to attributes
        Set<EntityAttribute> pairs = new HashSet<>()
        Set<CollectionAttribute> childs = new HashSet<>()
        domainClass.attributes.each {
            it.generator = Class.forName("simplejpa.scaffolding.generator.basic.${it.class.simpleName}Generator")
                .newInstance([it].toArray())
            if (it instanceof CollectionAttribute) it.generator.ignoreLazy = scaffolding.ignoreLazy

            // Add relations
            if (it instanceof EntityAttribute && it.oneToOne) {
                pairs << it
            } else if (it instanceof CollectionAttribute && it.oneToMany) {
                childs << it
            }
        }

        // Generate model
        generateArtifact(modelTemplate,
            "${BuildSettingsHolder.settings.baseDir}/griffon-app/models/${scaffolding.generatedPackage.replace('.', '/')}",
            customClassName? "${customClassName}Model.groovy": "${domainClassName}Model.groovy")

        // generate view
        generateArtifact(viewTemplate,
            "${BuildSettingsHolder.settings.baseDir}/griffon-app/views/${scaffolding.generatedPackage.replace('.', '/')}",
            customClassName? "${customClassName}View.groovy": "${domainClassName}View.groovy")

        // generate controller
        generateArtifact(controllerTemplate,
            "${BuildSettingsHolder.settings.baseDir}/griffon-app/controllers/${scaffolding.generatedPackage.replace('.', '/')}",
            customClassName? "${customClassName}Controller.groovy": "${domainClassName}Controller.groovy")

        // generate integration test
        if (!customClassName) {
            log.info "Creating integration test..."

            generateArtifact('SimpleJpaIntegrationTest',
                "${BuildSettingsHolder.settings.baseDir}/test/integration/${scaffolding.generatedPackage.replace('.', '/')}",
                "${domainClassName}Test.groovy")

            if (!scaffolding.skipExcel) {
                File xmlFile = new File("${BuildSettingsHolder.settings.baseDir}/test/integration/${scaffolding.generatedPackage.replace('.', '/')}/data.xls")
                String sheetName = domainClassName.toLowerCase()
                HSSFWorkbook workbook = xmlFile.exists()? new HSSFWorkbook(new FileInputStream(xmlFile)): new HSSFWorkbook()
                if (workbook.getSheet(sheetName)) {
                    log.info "Sheet $sheetName already exists, it will not modified!"
                } else {
                    workbook.createSheet(sheetName)
                    FileOutputStream output = new FileOutputStream(xmlFile)
                    workbook.write(output)
                    output.close()
                }
                log.info "File $xmlFile created or updated!"
            }
        }

        // create MVCGroup
        createMVCGroup(targetPackageName, customClassName?: domainClassName)
        generatedMVCGroups << customClassName?: domainClassName

        // generate pairs
        pairs.each {
            if (!generatedMVCGroups.contains(it.target.nameAsPair)) {
                generate(it.target, 'SimpleJpaPairModel', 'SimpleJpaPairView', 'SimpleJpaPairController', it.target.nameAsPair)
            }
        }

        // generate childs
        childs.each {
            if (!generatedMVCGroups.contains(it.target.nameAsChild)) {
                generate(it.target, 'SimpleJpaChildModel', 'SimpleJpaChildView', 'SimpleJpaChildController', it.target.nameAsChild)
            }
        }

    }

    public String modelAttrs(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.modelAttr())
        }
        return addTab(result, tab)
    }

    public String table(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.asColumn())
        }
        return addTab(result, tab)
    }

    public String dataEntry(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            result << "label('${GriffonNameUtils.getNaturalName(it.name)}:')"
            result.addAll(it.generator.asDataEntry())
            if (it instanceof CollectionAttribute && it.isManyToMany()) {
                result << "errorLabel(path: '${it.name}', constraints: 'skip 1,grow,span,wrap')"
            } else {
                result << "errorLabel(path: '${it.name}', constraints: 'wrap')"
            }
        }
        return addTab(result, tab)
    }

    public String listAll_clear(int tab) {
        List<String> result = []
        result << "model.${domainClassGlazedListVariable}.clear()"
        domainClass.attributes.each {
            if (it.generator.respondsTo("clearList")) {
                result.addAll(it.generator.clearList())
            }
        }
        return addTab(result, tab)
    }

    public String listAll_find(int tab) {
        List<String> result = []
        result << "List ${domainClassNameAsProperty}Result = findAll${domainClassName}()"
        domainClass.attributes.each {
            if (it.generator.respondsTo("findList")) {
                result.addAll(it.generator.findList())
            }
        }
        return addTab(result, tab)
    }

    public String listAll_set(int tab) {
        List<String> result = []
        result << "model.${domainClassGlazedListVariable}.addAll(${domainClassNameAsProperty}Result)"
        result << "model.${firstAttrSearch} = null"
        result << "model.searchMessage = app.getMessage('simplejpa.search.all.message')"
        domainClass.attributes.each {
            if (it.generator.respondsTo("setList")) {
                result.addAll(it.generator.setList())
            }
        }
        return addTab(result, tab)
    }

    public String domainClassConstructor() {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.constructor())
        }
        "new ${domainClassName}(${result.join(', ')})"
    }

    public String saveOneToManyInverse(DomainClass currentDomainClass, int tab, String customLhs = null) {
        return addTab(processSaveOneToManyInverse(currentDomainClass, 0, customLhs), tab, true)
    }

    private List<String> processSaveOneToManyInverse(DomainClass currentDomainClass, int tab, String customLhs = null) {
        List<String> result = []
        String lhs = customLhs?: currentDomainClass.nameAsProperty
        String tabs = "\t" * tab
        currentDomainClass.getOneToManyInverse().each { CollectionAttribute attr ->
            result << "${tabs}${lhs}.${attr.name}.each { ${attr.targetType} ${GriffonNameUtils.getPropertyName(attr.targetType)} ->"
            result << "${tabs}\t${GriffonNameUtils.getPropertyName(attr.targetType)}.${currentDomainClass.nameAsProperty} = $lhs"
            result.addAll(processSaveOneToManyInverse(attr.target, tab+1, null))
            result << "${tabs}}"
        }
        result
    }

    public String saveManyToManyInverse(DomainClass currentDomainClass, int tab, String customLhs = null) {
        return addTab(processSaveManyToManyInverse(currentDomainClass, 0, customLhs), tab, true)
    }

    public List<String> processSaveManyToManyInverse(DomainClass currentDomainClass, int tab, String customLhs = null) {
        List<String> result = []
        String lhs = customLhs?: currentDomainClass.nameAsProperty
        String tabs = "\t" * tab
        currentDomainClass.getManyToManyInverse().each { CollectionAttribute attr ->
            String targetName = GriffonNameUtils.getPropertyName(attr.target.name)
            result << "${tabs}${lhs}.${attr.name}.each { ${attr.target.name} $targetName ->"
            result << "${tabs}\tif (!${targetName}.${attr.mappedBy}.contains(${currentDomainClass.nameAsProperty})) {"
            result << "${tabs}\t\t${targetName}.${attr.mappedBy}.add($lhs)"
            result.addAll(processSaveManyToManyInverse(attr.target, tab+1, null))
            result << "${tabs}\t}"
            result << "${tabs}}"
        }
        result
    }

    public String update(int tab, String lhs = null) {
        List<String> result = []
        lhs = lhs?: "selected${domainClassName}"
        domainClass.attributes.each {
            result.addAll(it.generator.update(lhs))
        }
        return addTab(result, tab)
    }

    public String clear(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.clear())
        }
        return addTab(result, tab)
    }

    public String selected(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.selected())
        }
        return addTab(result, tab)
    }

    public String delete(int tab) {
        if (scaffolding.isAlwaysExcludeSoftDeleted()) {
            return addTab(["softDelete(${domainClassNameAsProperty})"], tab)
        } else {
            return addTab(["remove(${domainClassNameAsProperty})"], tab)
        }
    }

    public String pair_init(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.pair_init("model.$domainClassNameAsProperty"))
        }
        return addTab(result, tab)
    }

    public String sub_listAll_clear(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            if (it.generator.respondsTo("clearList")) {
                result.addAll(it.generator.clearList())
            }
        }
        if (!result.empty) {
            result = result.collectAll { "\t${it}" }
            result.add(0, 'execInsideUISync {')
            result << '}'
            return addTab(result, tab, true)
        }
        ''
    }

    public String sub_listAll_find(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            if (it.generator.respondsTo("findList")) {
                result.addAll(it.generator.findList())
            }
        }
        addTab(result, tab, true)
    }

    public String sub_listAll_set(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            if (it.generator.respondsTo("setList")) {
                result.addAll(it.generator.setList())
            }
        }
        if (!result.empty) {
            result = result.collectAll { "\t${it}" }
            result.add(0, 'execInsideUISync {')
            result.add('}')
            return addTab(result, tab, true)
        }
        ''
    }


}
