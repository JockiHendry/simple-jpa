package simplejpa.scaffolding.generator

import groovy.text.SimpleTemplateEngine
import org.codehaus.griffon.artifacts.model.Plugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import simplejpa.scaffolding.DomainClass
import simplejpa.scaffolding.Scaffolding
import java.nio.file.Files
import java.nio.file.Paths
import griffon.util.*

public abstract class Generator {

    private static Logger log = LoggerFactory.getLogger(Generator)

    public static final PathMatchingResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver()

    public Scaffolding scaffolding

    public String addTab(List<String> str, int numOfTab, boolean appendNewLineToEnd = false) {
        StringBuilder result = new StringBuilder()
        if (str.empty) return ''
        for (int i=0; i<str.size(); i++) {
            if (str[i]==null || str[i].allWhitespace) continue
            (1..numOfTab).each { result.append('\t') }
            result.append(str[i])
            if (i<str.size()-1) {
                result.append(System.lineSeparator())
            }
        }
        if (appendNewLineToEnd) {
            result.append(System.lineSeparator())
        }
        result.toString()
    }

    public void generateContent(Resource templateFile, String targetDir, File targetFile) {
        if (targetFile.exists()) {
            if (scaffolding.forceOverwrite) {
                log.warn "File $targetFile is exists and will be overwritten!"
            } else {
                log.warn "File $targetFile is exists and ignored!"
                return
            }
        }
        if (!Paths.get(targetDir).toFile().exists()) {
            Files.createDirectory(Paths.get(targetDir))
        }
        def template = new SimpleTemplateEngine().createTemplate(templateFile.file)
        def binding = ["g": this]
        String result = template.make(binding)
        targetFile.write(result)
        log.info "Done processing: $targetFile"
    }

    public void generateArtifact(def templateName, def targetDir, def targetFileName) {
        def templateFile = resolveTemplate(templateName, ".groovy")
        if (!templateFile.exists()) {
            log.error "Can't file $templateFile"
            return
        }
        generateContent(templateFile, targetDir, new File("${targetDir}/${targetFileName}"))
    }

    //
    // Taken from BuildSettings.groovy in Griffon source
    //
    public static def resolveResources(String pattern) {
        try {
            Resource[] resources = RESOLVER.getResources(pattern)
            // Filter hidden folders from OSX
            if (resources) {
                List<Resource> tmp = []
                for (Resource r : resources) {
                    if (r.URL.toString().contains('.DS_Store')) continue
                    tmp.add(r)
                }
                resources = tmp.toArray(new Resource[tmp.size()])
            }
            return resources
        } catch (Throwable e) {
            return [] as Resource[]
        }
    }

    //
    // Taken from _GriffonCreateArtifacts.groovy in Griffon source
    //
    public def resolveTemplate(String template, String fileSuffix) {
        String basedir = BuildSettingsHolder.settings.baseDir
        ArtifactSettings artifactSettings = BuildSettingsHolder.settings.artifactSettings
        String griffonWorkDir = BuildSettingsHolder.settings.griffonWorkDir
        // first check for presence of template in application
        def templateFile = new FileSystemResource("${basedir}/src/templates/artifacts/${template}${fileSuffix}")
        if (!templateFile.exists()) {
            // now check for template provided by plugins
            def pluginTemplateFiles = resolveResources("file:${artifactSettings.artifactBase(Plugin.TYPE)}/*/src/templates/artifacts/${template}${fileSuffix}")
            if (pluginTemplateFiles) {
                templateFile = pluginTemplateFiles[0]
            }
        }
        templateFile
    }

    public void createMVCGroup(String packageName, String name) {
        String basedir = BuildSettingsHolder.settings.baseDir
        def applicationConfigFile = new File("${basedir}/griffon-app/conf/Application.groovy")
        def configText = applicationConfigFile.text
        if (configText =~ /(?s)\s*mvcGroups\s*\{.*'${GriffonUtil.getPropertyName(name)}'.*\}/) {
            log.warn("No MVC group added because it already exists!")
            return
        } else {
            if (!(configText =~ /\s*mvcGroups\s*\{/)) {
                configText += """
mvcGroups {
}
"""
            }

            List parts = []
            parts << "        model      = '${packageName}.${GriffonUtil.getClassName(name, "Model")}'"
            parts << "        view       = '${packageName}.${GriffonUtil.getClassName(name, "View")}'"
            parts << "        controller = '${packageName}.${GriffonUtil.getClassName(name, "Controller")}'"

            configText = configText.replaceAll(/\s*mvcGroups\s*\{/, """
mvcGroups {
    // MVC Group for "${GriffonUtil.getPropertyName(name)}"
    '${GriffonUtil.getPropertyName(name)}' {
${parts.join('\n')}
    }
""")
            // save changes
            applicationConfigFile.withWriter {
                it.write configText
            }

            log.info "MVCGroup $name created."
        }
    }

    public void setStartupGroup(String name) {
        String basedir = BuildSettingsHolder.settings.baseDir
        def applicationConfigFile = new File("${basedir}/griffon-app/conf/Application.groovy")
        def configText = applicationConfigFile.text
        configText = configText.replaceFirst(/startupGroups = \['.*'\]/, "startupGroups = ['${GriffonUtil.getPropertyName(name)}']")
        applicationConfigFile.withWriter {
            it.write configText
        }
        log.info "Startup group set to $name."
    }

    public abstract void generate(DomainClass domainClass);

    public abstract void generateStartupGroup(Map<String,DomainClass> domainClasses);

}