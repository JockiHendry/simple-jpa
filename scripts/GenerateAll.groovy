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

import groovy.text.GStringTemplateEngine
import groovy.text.SimpleTemplateEngine
import groovy.transform.ToString
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.codehaus.groovy.antlr.AntlrASTProcessor
import org.codehaus.groovy.antlr.GroovySourceAST
import org.codehaus.groovy.antlr.SourceBuffer
import org.codehaus.groovy.antlr.UnicodeEscapingReader
import org.codehaus.groovy.antlr.parser.GroovyLexer
import org.codehaus.groovy.antlr.parser.GroovyRecognizer
import org.codehaus.groovy.antlr.treewalker.SourceCodeTraversal
import org.codehaus.groovy.antlr.treewalker.Visitor
import org.codehaus.groovy.antlr.treewalker.VisitorAdapter
import static org.codehaus.groovy.antlr.parser.GroovyTokenTypes.*
import griffon.util.GriffonUtil

/**
 * Gant script that creates a new MVC Group with view, controller and model that performs CRUD operation for
 * specified domain class.
 *
 */

includeTargets << griffonScript("_GriffonCreateArtifacts")

enum RelationType { ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY }

final String PARENT_CLASS = "parentClass"
final String PARENT_ATTRIBUTE = "parentAttribute"

String domainClassName
String generatedPackage
String domainPackageName
String startupGroup
boolean forceOverwrite
boolean softDelete
boolean skipExcel
boolean setStartup
List fieldList
RelationType relationType = null
Map relationParameter = [:]
List generated = []

def getFields

getFields =  { String name, boolean processChild = true ->

    String fullDomainClassName= "${domainPackageName ? domainPackageName : ''}${domainPackageName ? '.' : ''}${GriffonUtil.getClassNameRepresentation(name)}"
    File fullDomainClassFile = new File("${basedir}/src/main/${fullDomainClassName.replace('.', '/')}.groovy")

    if (!fullDomainClassFile.exists()) {
        fail "Can't find domain class $fullDomainClassFile"
    }

    SourceBuffer sourceBuffer = new SourceBuffer()
    UnicodeEscapingReader unicodeReader = new UnicodeEscapingReader(new FileReader(fullDomainClassFile), sourceBuffer)
    GroovyLexer lexer = new GroovyLexer(unicodeReader)
    unicodeReader.setLexer(lexer)
    GroovyRecognizer parser = GroovyRecognizer.make(lexer)
    parser.setSourceBuffer(sourceBuffer)
    parser.compilationUnit()
    GroovySourceAST ast = parser.getAST()

    Visitor domainModelVisitor = new DomainModelVisitor()
    AntlrASTProcessor traverser = new SourceCodeTraversal(domainModelVisitor)
    traverser.process(ast)

    if (!domainModelVisitor.isEntity()) {
        println "Can't process $fullDomainClassFile because this is not an JPA entity.  This class didn't have @Entity annotation."
        return
    }

    List fields = domainModelVisitor.fields;

    def basicType = ["Boolean", "boolean", "Character", "char", "Byte", "byte", "Short", "short",
            "Integer", "int", "Long", "long", "Float", "float", "Double", "double", "String", "BigInteger", "BigDecimal"]
    def dateType = ["DateTime", "LocalDateTime", "LocalDate", "LocalTime"]

    fields.each { field ->

        // Check if primitive or wrapper for primitive
        if (basicType.contains(field.type as String)) {
            field["info"] = "BASIC_TYPE"
            return
        }

        // Check if this is date
        if (dateType.contains(field.type as String)) {
            field["info"] = "DATE"
            return
        }

        // Check if this is another domain model
        if (new File("${basedir}/src/main/${domainPackageName.replace('.', '/')}/${field.type}.groovy").exists()) {
            field["info"] = "DOMAIN_CLASS"
            return
        }

        // Check if this is a List and has one of relationship annotation
        if ("List"==(field.type as String)) {
            if (field.annotations?.containsAnnotation(["ManyToMany","OneToMany"])) {
                List typeArguments = field.type.childrenOfType(TYPE_ARGUMENTS)
                def domainClass
                if (typeArguments.size() > 0) {
                    domainClass = typeArguments[0]?.childAt(0)?.childAt(0)?.childAt(0)
                    if (domainClass!=null) {
                        field["info"] = domainClass.toString()
                    }
                }
                if (processChild) {
                    field["bidirectional"] = false
                    if (field.annotations?.containsAnnotation("OneToMany")) {
                        List relatedAttributes = getFields(domainClass.toString(), false).findAll{
                            it.annotations?.containsAnnotation("ManyToOne") && it.type.toString()==name }
                        if (relatedAttributes.size() > 0) {
                            field["bidirectional"] = true
                            field["linkedAttribute"] = relatedAttributes[0]
                        }
                    }

                    if (field.annotations?.containsAnnotation("ManyToMany")) {
                        List relatedAttributes = getFields(domainClass.toString(), false).findAll {
                            it.annotations?.containsAnnotation("ManyToMany") &&
                            it.type.childrenOfType(TYPE_ARGUMENTS)[0]?.childAt(0)?.childAt(0)?.childAt(0)?.toString()==name }
                        if (relatedAttributes.size() > 0) {
                            field["bidirectional"] = true
                            field["linkedAttribute"] = relatedAttributes[0]
                        }
                    }
                }
            }
            return
        }

        // Unknown
        field["info"] = "UNKNOWN"

    }

    println "Found $fields"
    fields
}

def findDomainClasses = {
    new File("${basedir}/src/main/${domainPackageName.replace('.', '/')}").list().grep { String name -> name.endsWith(".groovy") }.collect {
        it.substring(0, it.length()-7)
    }
}

def createMVC = {

    ["Model", "View", "Controller"].each { String type ->
        println "Creating $type..."

        String templateFileName
        String className

        if (startupGroup) {
            templateFileName = "Startup${type}"
            className = startupGroup
        } else {
            className = domainClassName
            templateFileName = "SimpleJpa${type}"

            switch (relationType) {
                case RelationType.ONE_TO_MANY:
                    templateFileName = "SimpleJpaChild${type}"
                    className = "${className}AsChild"
                    break
                case RelationType.ONE_TO_ONE:
                    templateFileName = "SimpleJpaPair${type}"
                    className = "${className}AsPair"
                    break
            }
        }

        def templateFile = resolveTemplate(templateFileName, ".groovy")
        if (!templateFile.exists()) {
            println "Can't find $templateFile."
            return
        }

        className = GriffonUtil.getClassName(className, type)

        String dir = "${basedir}/griffon-app/"

        switch (type) {
            case "Model": dir += "models"; break
            case "View": dir += "views"; break
            case "Controller": dir+= "controllers"; break
        }
        dir = "${dir}/${generatedPackage.replace('.','/')}"

        File file = new File("${dir}/${className}.groovy")

        if (file.exists()) {
            if (forceOverwrite) {
                println "File $file already exists and will be overwritten!"
            } else {
                println "File $file already exists!"
                return
            }
        }

        ant.mkdir('dir': dir)

        def template = new SimpleTemplateEngine().createTemplate(templateFile.file)

        def binding = [
            // values
            "packageName": generatedPackage,
            "domainPackage": domainPackageName,
            "className": className,
            "domainClass": domainClassName,
            "domainClassAsProp": startupGroup?:GriffonUtil.getPropertyName(domainClassName),
            "domainClassLists": startupGroup?findDomainClasses():[],
            "firstField": startupGroup?:fieldList[0]?.name as String,
            "softDelete": softDelete,
            "parentDomainClass": relationParameter[PARENT_CLASS],
            "parentAttribute": relationParameter[PARENT_ATTRIBUTE],
            "fields": fieldList,

            // functions
            "prop": GriffonUtil.&getPropertyName,
            "cls": GriffonUtil.&getClassNameRepresentation,
            "natural": GriffonUtil.&getNaturalName,

            // relational
            "isMappedBy": {field -> field.annotations?.containsAttribute('mappedBy')}.&call,
            "isManyToOne": {field -> field.info=="DOMAIN_CLASS" && field.annotations?.containsAnnotation("ManyToOne")}.&call,
            "isManyToMany": {field -> field.type.toString()=='List' && field.info!='UNKNOWN' && field.annotations?.containsAnnotation("ManyToMany")}.&call,
            "isOneToMany": {field -> field.type.toString()=='List' && field.info!='UNKNOWN' && field.annotations?.containsAnnotation("OneToMany")}.&call,
            "isOneToOne": {field -> field.info=="DOMAIN_CLASS" && field.annotations?.containsAnnotation('OneToOne')}.&call,
            "isRelation": {field -> field.annotations?.containsAnnotation(["ManyToMany", "OneToMany", "OneToOne", "ManyToOne"])}.&call,
            "isCascaded": {field -> field.annotations?.containsAttribute('cascade') && field.annotations?.containsAttribute('orphanRemoval')}.&call,
            "isBidirectional": {field -> field.bidirectional}.&call,
            "linkedAttribute": {field -> field.linkedAttribute}.&call,

            // utilities
            "getField": {String name -> getFields(name)}.&call,

        ]

        String result = template.make(binding)
        file.write(result)

        println "File $file created!"
    }

}

def createIntegrationTest = {

    println "Creating Integration Test..."

    def templateFile = resolveTemplate("SimpleJpaIntegrationTest", ".groovy")
    if (!templateFile.exists()) {
        println "Can't find $templateFile."
        return
    }
    String testClassName = GriffonUtil.getClassName(domainClassName, "Test")
    File testFile = new File("${basedir}/test/integration/${generatedPackage.replace('.', '/')}/${testClassName}.groovy")
    if (testFile.exists()) {
        if (forceOverwrite) {
            println "File $testFile already exists and will be overwritten!"
        } else {
            println "File $testFile already exists!"
            return
        }
    }
    ant.mkdir(dir: "${basedir}/test/integration/${generatedPackage.replace('.', '/')}")

    def template = new GStringTemplateEngine().createTemplate(templateFile.file)
    def binding = ["packageName":generatedPackage, "domainPackage":domainPackageName, "className":testClassName,
            "domainClass": domainClassName, "domainClassAsProp": startupGroup?:GriffonUtil.getPropertyName(domainClassName),
            "fields":fieldList]
    String result = template.make(binding)
    testFile.write(result)

    // Create XML file called "data.xml" in the same package
    if (skipExcel) {
        println "Will not create XML file for integration testing data!"
    } else {
        File xmlFile = new File("${basedir}/test/integration/${generatedPackage.replace('.', '/')}/data.xls")
        String sheetName = domainClassName.toLowerCase()
        HSSFWorkbook workbook
        if (xmlFile.exists()) {
            println "File $xmlFile already exists..."
            workbook = new HSSFWorkbook(new FileInputStream(xmlFile))
            if (workbook.getSheet(sheetName)) {
                println "Sheet $sheetName already exists, it will not modified!"
                return
            }
        } else {
            workbook = new HSSFWorkbook()
        }
        workbook.createSheet(sheetName)
        FileOutputStream output = new FileOutputStream(xmlFile)
        workbook.write(output)
        output.close()
        println "File $xmlFile created!"
    }
    println "File $testFile created!"
}

def createMVCGroup = { String mvcGroupName ->

    println "Adding new MVC Group..."

    if (relationType==RelationType.ONE_TO_MANY) {
        mvcGroupName+="AsChild"
    } else if (relationType==RelationType.ONE_TO_ONE) {
        mvcGroupName+="AsPair"
    }

    // create mvcGroup in an application
    def applicationConfigFile = new File("${basedir}/griffon-app/conf/Application.groovy")
    def configText = applicationConfigFile.text
    if (configText =~ /(?s)\s*mvcGroups\s*\{.*'${GriffonUtil.getPropertyName(mvcGroupName)}'.*\}/) {
        println "No MVC group added because it already exists!"
        return
    } else {
        if (!(configText =~ /\s*mvcGroups\s*\{/)) {
            configText += """
mvcGroups {
}
"""
        }

        List parts = []
        parts << "        model      = '${generatedPackage}.${GriffonUtil.getClassName(mvcGroupName, "Model")}'"
        parts << "        view       = '${generatedPackage}.${GriffonUtil.getClassName(mvcGroupName, "View")}'"
        parts << "        controller = '${generatedPackage}.${GriffonUtil.getClassName(mvcGroupName, "Controller")}'"

        configText = configText.replaceAll(/\s*mvcGroups\s*\{/, """
mvcGroups {
    // MVC Group for "${GriffonUtil.getPropertyName(mvcGroupName)}"
    '${GriffonUtil.getPropertyName(mvcGroupName)}' {
${parts.join('\n')}
    }
""")
    }

    // set startupGroup to this MVCGroup
    if (setStartup || startupGroup) {
        configText = configText.replaceFirst(/startupGroups = \['.*'\]/, "startupGroups = ['${GriffonUtil.getPropertyName(mvcGroupName)}']")
    }

    // save changes
    applicationConfigFile.withWriter {
        it.write configText
    }

    println "MVCGroup ${mvcGroupName} created."

}

def processStartupGroup = {
    createMVC()
    createMVCGroup(startupGroup)
}

def processDomainClass

processDomainClass = { String name ->

    domainClassName = GriffonUtil.getClassNameRepresentation(name)
    fieldList = getFields(domainClassName)

    createMVC()
    createIntegrationTest()
    createMVCGroup(name)


    relationType = null
    relationParameter.clear()

    // For one-to-many relation
    fieldList.each { field ->
        if (field.annotations?.containsAnnotation("OneToMany")) {
            if (!generated.contains(field["info"])) {
                relationType = RelationType.ONE_TO_MANY
                relationParameter[PARENT_CLASS] = new String(domainClassName)
                relationParameter[PARENT_ATTRIBUTE] = field.name as String
                processDomainClass(field["info"])
                generated << domainClassName
            }
        } else if (field.annotations?.containsAnnotation("OneToOne") && !field.annotations?.containsAttribute("mappedBy")) {
            if (!generated.contains(field.type as String)) {
                relationType = RelationType.ONE_TO_ONE
                relationParameter[PARENT_CLASS] = domainClassName
                processDomainClass(field.type as String)
                generated << (field.type as String)
            }
        }
    }
}

target(name: 'generateAll', description: "Create CRUD scaffolding for specified domain class", prehook: null, posthook: null) {

    if (argsMap?.params?.isEmpty() && argsMap['startup-group']==null) {
        println '''
Usage: griffon generate-all *
       griffon generate-all * --force-overwrite --set-startup
       griffon generate-all [domainClass]
       griffon generate-all --startup-group=[startupGroupName]

Parameter: --force-overwrite : will overwrite existing file without any warning!
           --skip-xml : will not generate XML for DbUnit data (integration testing).

Example: griffon generate-all Student

Domain class package location is retrieved from the value of griffon.simpleJpa.model.package in Config.groovy
(default is 'domain').
'''
        println "Can't execute generate-all"
        return
    }

    generatedPackage = argsMap['generated-package'] ?: 'project'
    startupGroup = argsMap['startup-group']
    forceOverwrite = argsMap.containsKey('force-overwrite')
    skipExcel = argsMap.containsKey('skip-excel')
    setStartup = argsMap['set-startup']

    def config = new ConfigSlurper().parse(configFile.toURL())
    domainPackageName = config.griffon?.simplejpa?.model?.package ?: 'domain'
    softDelete = config.griffon?.simplejpa?.finder?.alwaysExcludeSoftDeleted ?: false

    if (startupGroup!=null) {
        processStartupGroup()
    } else {
        if (argsMap.params[0]=="*") {
            findDomainClasses().each { String name -> processDomainClass(name)}
        } else {
            processDomainClass(argsMap.params[0])
        }
    }

    String validationMessages = "${basedir}/griffon-app/i18n/messages.properties"
    File file = new File(validationMessages)

    ["simplejpa.dialog.save.button": "Save",
     "simplejpa.dialog.cancel.button": "Cancel",
     "simplejpa.dialog.delete.button": "Delete",
     "simplejpa.dialog.update.button": "Update",
     "simplejpa.dialog.close.button": "Close",
     "simplejpa.search.all.message": "Display all data",
     "simplejpa.search.result.message": "Display {0} search result for {1}",
     "simplejpa.error.alreadyExist.message": "already registered!",
     "simplejpa.dialog.delete.message": "Do you really want to delete this?",
     "simplejpa.dialog.delete.title": "Delete Confirmation",
     "simplejpa.dialog.update.message": "Do you really want to update this?",
     "simplejpa.dialog.update.title": "Update Confirmation",
     "simplejpa.search.label": "Search",
     "simplejpa.search.all.label": "Display All"].each { k, v ->
        if (!file.text.contains(k)) {
            println "Adding $k to message.properties..."
            file << "\n$k = $v"
        }
    }
}

@ToString
class Annotations {

    Map annotations = [:]

    void addAnnotation(String name) {
        annotations[name] = new AnnotationFound('name': name)
    }

    void addAnnotation(AnnotationFound annotation) {
        annotations[annotation.name] = annotation
    }

    AnnotationFound get(String annotation) {
        annotations[annotation]
    }

    def containsAttribute(String attribute) {
        annotations.findAll { k, v -> v.getAttribute(attribute) }
    }

    boolean containsAnnotation(String search) {
        annotations.keySet().find { it==search}
    }

    def containsAnnotation(List search) {
        annotations.keySet().findAll { search.contains(it) }
    }

}

@ToString
class AnnotationFound {
    String name
    def value
    Map attributes = [:]

    void addAttribute(String name, def value) {
        attributes[name] = value
    }

    def getAttribute(String name) {
        attributes[name]
    }
}



class DomainModelVisitor extends VisitorAdapter {

    List fields = []
    boolean ignoreMode = false
    boolean entity = false
    AnnotationFound lastAnnotation = null

    public void visitVariableDef(GroovySourceAST node, int visitType) {
        if (visitType==Visitor.OPENING_VISIT && !ignoreMode)  {
            def name = node.childOfType(IDENT)
            def type = node.childOfType(TYPE).childAt(0)
            fields << ['name': name, 'type': type]
        }
    }

    public void visitMethodDef(GroovySourceAST node, int visitType) {
        if (visitType==Visitor.OPENING_VISIT) {
            ignoreMode = true
        } else if (visitType==Visitor.CLOSING_VISIT) {
            ignoreMode = false
        }
    }

    public void visitAnnotation(GroovySourceAST node, int visitType) {
        if (visitType==Visitor.OPENING_VISIT) {
            if (node.childAt(0).toString()=="Entity") entity = true
            if (fields.size() > 0) {
                def annotations = fields.last().'annotations' ?: new Annotations()
                lastAnnotation = new AnnotationFound(name: node.childAt(0).toString())
                annotations.addAnnotation(lastAnnotation)
                fields.last().'annotations' = annotations
            }
        }
    }

    public void visitAnnotationMemberValuePair(GroovySourceAST node, int visitType) {
        if (visitType==Visitor.OPENING_VISIT) {
            String memberName = node.childAt(0).toString()
            if (fields.size() > 0 && lastAnnotation) {
                lastAnnotation.addAttribute(memberName, node.childAt(1)?.toString())
            }
        }
    }

}


setDefaultTarget(generateAll)