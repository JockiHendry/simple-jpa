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
    Attribute firstPair
    Attribute firstChild
    String domainClassName
    String domainClassGlazedListVariable
    String domainClassNameAsNatural
    String domainClassNameAsProperty
    String domainPackageName
    String targetPackageName
    String customClassName
    List<String> generatedMVCGroups = []

    public BasicGenerator(Scaffolding scaffolding) {
        super(scaffolding)

        attributeGenerators['BasicAttribute'] = BasicAttributeGenerator
        attributeGenerators['CollectionAttribute'] = CollectionAttributeGenerator
        attributeGenerators['DateAttribute'] = DateAttributeGenerator
        attributeGenerators['EntityAttribute'] = EntityAttributeGenerator
        attributeGenerators['EnumeratedAttribute'] = EnumeratedAttributeGenerator
        attributeGenerators['UnknownAttribute'] = UnknownAttributeGenerator
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

    void init(DomainClass domainClass, String customClassName = null) {
        this.domainClass = domainClass
        this.customClassName = customClassName

        // Initialize variables
        if (domainClass.attributes.isEmpty()) {
            firstAttr = "replaceThis"
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
        firstPair = domainClass.attributes.find { it instanceof EntityAttribute && it.pair }
        firstChild = domainClass.attributes.find { it instanceof CollectionAttribute && it.oneToMany && !(it.mappedBy && !it.hasCascadeAndOrphanRemoval) }
    }

    @Override
    void generate(DomainClass domainClass, String modelTemplate = 'SimpleJpaModel', String viewTemplate = 'SimpleJpaView',
              String controllerTemplate = 'SimpleJpaController', String customClassName = null) {

        log.info "Generating ${domainClass.name}..."

        init(domainClass, customClassName)
        Set<EntityAttribute> pairs = new HashSet<>()
        Set<CollectionAttribute> childs = new HashSet<>()
        domainClass.attributes.each {
            addAttributeGeneratorTo(it)

            // Add relations
            if (it instanceof EntityAttribute && it.pair) {
                pairs << it
            } else if (it instanceof CollectionAttribute && it.oneToMany) {
                childs << it
            }
        }


        // Generate model
        generateArtifact(modelTemplate,
            "${BuildSettingsHolder.settings.baseDir}/griffon-app/models/${targetPackageName.replace('.', '/')}",
            customClassName? "${customClassName}Model.groovy": "${domainClassName}Model.groovy")

        // generate view
        generateArtifact(viewTemplate,
            "${BuildSettingsHolder.settings.baseDir}/griffon-app/views/${targetPackageName.replace('.', '/')}",
            customClassName? "${customClassName}View.groovy": "${domainClassName}View.groovy")

        // generate controller
        generateArtifact(controllerTemplate,
            "${BuildSettingsHolder.settings.baseDir}/griffon-app/controllers/${targetPackageName.replace('.', '/')}",
            customClassName? "${customClassName}Controller.groovy": "${domainClassName}Controller.groovy")

        // generate integration test
        if (!customClassName) {
            log.info "Creating integration test..."

            generateArtifact('SimpleJpaIntegrationTest',
                "${BuildSettingsHolder.settings.baseDir}/test/integration/${targetPackageName.replace('.', '/')}",
                "${domainClassName}Test.groovy")

            if (!scaffolding.skipExcel) {
                File xmlFile = new File("${BuildSettingsHolder.settings.baseDir}/test/integration/${targetPackageName.replace('.', '/')}/data.xls")
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
        if (scaffolding.setStartup && domainClassName) {
            setStartupGroup(domainClassName)
        }
        generatedMVCGroups << customClassName?: domainClassName

        pairs.each {
            if (!generatedMVCGroups.contains(it.target.nameAsPair)) {
                generatePair(it)
            }
        }
        childs.each {
            if (!generatedMVCGroups.contains(it.target.nameAsChild)) {
                generateChild(it)
            }
        }
    }

    public void generatePair(EntityAttribute attr) {
        generate(attr.target, 'SimpleJpaPairModel', 'SimpleJpaPairView', 'SimpleJpaPairController', attr.target.nameAsPair)
    }

    public void generateChild(CollectionAttribute attr) {
        generate(attr.target, 'SimpleJpaChildModel', 'SimpleJpaChildView', 'SimpleJpaChildController', attr.target.nameAsChild)
    }

    public String modelAttrs(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.modelAttr())
        }
        return addTab(result, tab)
    }

    public String actions(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.action())
        }
        return addTab(result, tab, true)
    }

    public String popups(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            List<String> popups = it.generator.popup()
            if (!popups.empty) {
                result.add('\n')
                result.addAll(popups)
            }
        }
        return addTab(result, tab, true)
    }

    public String tableActions() {
        def attribute = firstChild?: firstPair
        if (attribute) {
            return ", doubleClickAction: ${attribute.actionName}, enterKeyAction: ${attribute.actionName}"
        }
        ""
    }

    public String table(int tab) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.asColumn())
        }
        return addTab(result, tab)
    }

    public String dataEntry(int tab, boolean addAuditing = true) {
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
        if (addAuditing && !domainClass.excludeAuditing) {
            result << "panel(visible: bind{table.isRowSelected}, constraints: 'span, growx, wrap') {"
            result << "\tflowLayout(alignment: FlowLayout.LEADING)"
            result << "\tlabel('Created:')"
            result << "\tlabel(text: bind{model.created})"
            result << "\tlabel(text: bind{model.createdBy})"
            result << "\tlabel('   Modified:')"
            result << "\tlabel(text: bind{model.modified})"
            result << "\tlabel(text: bind{model.modifiedBy})"
            result << "}"
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

    public List<String> processSaveOneToManyInverse(DomainClass currentDomainClass, int tab, String customLhs = null) {
        List<String> result = []
        String lhs = customLhs?: currentDomainClass.nameAsProperty
        String tabs = "\t" * tab
        currentDomainClass.getOneToManyInverse().each { CollectionAttribute attr ->
            if (!attr.eager && !scaffolding.ignoreLazy) return;
            result << "${tabs}${lhs}.${attr.name}.each { ${attr.targetType} ${GriffonNameUtils.getPropertyName(attr.targetType)} ->"
            result << "${tabs}\t${GriffonNameUtils.getPropertyName(attr.targetType)}.${currentDomainClass.nameAsProperty} = $lhs"
            result.addAll(processSaveOneToManyInverse(attr.target, tab+1))
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
            if (!attr.eager && !scaffolding.ignoreLazy) return;
            String targetName = GriffonNameUtils.getPropertyName(attr.target.name)
            result << "${tabs}${lhs}.${attr.name}.each { ${attr.target.name} $targetName ->"
            result << "${tabs}\tif (!${targetName}.${attr.mappedBy}.contains(${currentDomainClass.nameAsProperty})) {"
            result << "${tabs}\t\t${targetName}.${attr.mappedBy}.add($lhs)"
            result.addAll(processSaveManyToManyInverse(attr.target, tab+1))
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

    public String clear(int tab, boolean addAuditing = true) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.clear())
        }
        if (addAuditing && !domainClass.excludeAuditing) {
            result << "model.created = null"
            result << "model.createdBy = null"
            result << "model.modified = null"
            result << "model.modifiedBy = null"
        }
        return addTab(result, tab)
    }

    public String selected(int tab, boolean addAuditing = true) {
        List<String> result = []
        domainClass.attributes.each {
            result.addAll(it.generator.selected())
        }
        if (addAuditing && !domainClass.excludeAuditing) {
            String style = scaffolding.dateTimeStyle
            String customFunction = style? "DateFormat.getDateTimeInstance(DateFormat.$style, DateFormat.$style).format(": null
            result << "model.created = ${customFunction?'selected.createdDate?' + customFunction:''}selected.createdDate${customFunction?'):null':''}"
            result << "model.createdBy = selected.createdBy?'('+selected.createdBy+')':null"
            result << "model.modified = ${customFunction?'selected.modifiedDate?' + customFunction:''}selected.modifiedDate${customFunction?'):null':''}"
            result << "model.modifiedBy = selected.modifiedBy?'('+selected.modifiedBy+')':null"
        }
        return addTab(result, tab)
    }

    public String delete(int tab) {
        if (scaffolding.alwaysExcludeSoftDeleted) {
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

    public String imports() {
        List<String> result = []
        result << "import ${domainPackageName}.*"
        domainClass.attributes.each {
            if (it instanceof CollectionAttribute && it.target.packageName != domainPackageName) {
                result << "import ${it.target.packageName}.*"
            } else if (it instanceof EntityAttribute && it.target.packageName != domainPackageName) {
                result << "import ${it.target.packageName}.*"
            }
        }
        if (scaffolding.dateTimeStyle && !domainClass.excludeAuditing) {
            result << 'import java.text.DateFormat'
        }
        return addTab(result, 0)
    }


}
