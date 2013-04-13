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

String generatedPackage
String domainClassName
String domainPackageName
String startupGroup
boolean forceOverwrite
boolean softDelete
boolean skipExcel
List fieldList

def findDomainClasses = {
    new File("${basedir}/src/main/${domainPackageName.replace('.', '/')}").list().grep { String name -> name.endsWith(".groovy") }.collect {
        it.substring(0, it.length()-7)
    }
}

def createView = {
    println "Creating view..."
    def templateFile = resolveTemplate(startupGroup?"StartupView":"SimpleJpaView", ".groovy")
    if (!templateFile.exists()) {
        println "Can't find $templateFile."
        return
    }
    String viewClassName = GriffonUtil.getClassName(startupGroup?:domainClassName, "View")
    File viewFile = new File("${basedir}/griffon-app/views/${generatedPackage.replace('.', '/')}/${viewClassName}.groovy")
    if (viewFile.exists()) {
        if (forceOverwrite) {
            println "File $viewFile already exists and will be overwritten!"
        } else {
            println "File $viewFile already exists!"
            return
        }
    }
    ant.mkdir(dir: "${basedir}/griffon-app/views/${generatedPackage.replace('.', '/')}")

    def template = new GStringTemplateEngine().createTemplate(templateFile.file)
    def binding = ["packageName":generatedPackage, "domainPackage":domainPackageName, "className":viewClassName,
            "domainClass": domainClassName, "domainClassAsProp": startupGroup?:GriffonUtil.getPropertyName(domainClassName),
            "GriffonUtil": GriffonUtil,
            "domainClassLists": startupGroup?findDomainClasses():[],
            "fields": fieldList, "firstField": startupGroup?:fieldList[0]?.name as String,
            "firstFieldUppercase": startupGroup?:GriffonUtil.getClassNameRepresentation(fieldList[0]?.name as String),
            "firstFieldNatural": startupGroup?:GriffonUtil.getNaturalName(fieldList[0]?.name as String)]
    String result = template.make(binding)
    viewFile.write(result)

    println "File $viewFile created!"
}

def createController = {
    println "Creating controller..."
    def templateFile = resolveTemplate(startupGroup?"StartupController":"SimpleJpaController", ".groovy")
    if (!templateFile.exists()) {
        println "Can't find $templateFile."
        return
    }
    String controllerClassName = GriffonUtil.getClassName(startupGroup?:domainClassName, "Controller")
    File controllerFile = new File("${basedir}/griffon-app/controllers/${generatedPackage.replace('.', '/')}/${controllerClassName}.groovy")
    if (controllerFile.exists()) {
        if (forceOverwrite) {
            println "File $controllerFile already exists and will be overwritten!"
        } else {
            println "File $controllerFile already exists!"
            return
        }
    }
    ant.mkdir(dir: "${basedir}/griffon-app/controllers/${generatedPackage.replace('.', '/')}")

    def template = new GStringTemplateEngine().createTemplate(templateFile.file)
    def binding = ["packageName":generatedPackage, "domainPackage":domainPackageName, "className":controllerClassName,
        "domainClass": domainClassName, "domainClassAsProp": domainClassName?GriffonUtil.getPropertyName(domainClassName):null,
        "fields":fieldList, "firstField":startupGroup?:fieldList[0]?.name as String,
        "softDelete": softDelete,
        "firstFieldUppercase": startupGroup?:GriffonUtil.getClassNameRepresentation(fieldList[0]?.name as String),
        "firstFieldNatural": startupGroup?:GriffonUtil.getNaturalName(fieldList[0]?.name as String)]
    String result = template.make(binding)
    controllerFile.write(result)

    println "File $controllerFile created!"
}
def createModel = {

    println "Creating model..."
    def templateFile = resolveTemplate(startupGroup?"StartupModel":"SimpleJpaModel", ".groovy")
    if (!templateFile.exists()) {
        println "Can't find $templateFile."
        return
    }
    String modelClassName = GriffonUtil.getClassName(startupGroup?:domainClassName, "Model")
    File modelFile = new File("${basedir}/griffon-app/models/${generatedPackage.replace('.', '/')}/${modelClassName}.groovy")
    if (modelFile.exists()) {
        if (forceOverwrite) {
            println "File $modelFile already exists and will be overwritten!"
        } else {
            println "File $modelFile already exists!"
            return
        }
    }
    ant.mkdir(dir: "${basedir}/griffon-app/models/${generatedPackage.replace('.', '/')}")

    def template = new GStringTemplateEngine().createTemplate(templateFile.file)
    def binding = ["packageName":generatedPackage, "domainPackage":domainPackageName, "className":modelClassName,
        "domainClass": domainClassName, "domainClassAsProp": startupGroup?:GriffonUtil.getPropertyName(domainClassName),
        "fields":fieldList]
    String result = template.make(binding)
    modelFile.write(result)

    println "File $modelFile created!"

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

        applicationConfigFile.withWriter {
            it.write configText.replaceAll(/\s*mvcGroups\s*\{/, """
mvcGroups {
    // MVC Group for "${GriffonUtil.getPropertyName(mvcGroupName)}"
    '${GriffonUtil.getPropertyName(mvcGroupName)}' {
${parts.join('\n')}
    }
""")
        }
    }
    println "MVCGroup ${mvcGroupName} created."

}

def processStartupGroup = {
    createModel()
    createController()
    createView()
    createMVCGroup(startupGroup)
}

def processDomainClass = { String name ->

    domainClassName = GriffonUtil.getClassNameRepresentation(name)
    String fullDomainClassName= "${domainPackageName ? domainPackageName : ''}${domainPackageName ? '.' : ''}$domainClassName"
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

    fieldList = domainModelVisitor.fields;

    def basicType = ["Boolean", "boolean", "Character", "char", "Byte", "byte", "Short", "short",
            "Integer", "int", "Long", "long", "Float", "float", "Double", "double", "String", "BigInteger", "BigDecimal"]
    def dateType = ["DateTime", "LocalDateTime", "LocalDate", "LocalTime"]

    fieldList.each { field ->

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

        // Check if this is a List and has one of "ManyToMany" or "OneToMany" annotation
        if ("List"==(field.type as String)) {
            if (field.annotations.find { ["ManyToMany","OneToMany"].contains(it.toString()) }) {
                List typeArguments = field.type.childrenOfType(TYPE_ARGUMENTS)
                if (typeArguments.size() > 0) {
                    def domainClass = typeArguments[0]?.childAt(0)?.childAt(0)?.childAt(0)
                    if (domainClass!=null) {
                        field["info"] = domainClass
                    }
                }
            }
            return
        }

        // Unknown
        field["info"] = "UNKNOWN"

    }

    println "Found $fieldList"

    createModel()
    createController()
    createView()
    createIntegrationTest()
    createMVCGroup(name)
}

target(name: 'generateAll', description: "Create CRUD scaffolding for specified domain class", prehook: null, posthook: null) {

    if (argsMap?.params?.isEmpty() && argsMap['startup-group']==null) {
        println '''
Usage: griffon generate-all *
       griffon generate-all * --force-overwrite
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

    def config = new ConfigSlurper().parse(configFile.toURL())
    domainPackageName = config.griffon?.simpleJpa?.model?.package ?: 'domain'
    softDelete = config.griffon?.simpleJpa?.finder?.alwaysExcludeSoftDeleted ?: false

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

class DomainModelVisitor extends VisitorAdapter {

    List fields = []
    boolean ignoreMode = false
    boolean entity = false

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
                def list = fields.last().'annotations' ?: []
                list << node.childAt(0)
                fields.last().'annotations' = list
            }
        }
    }

}

setDefaultTarget(generateAll)