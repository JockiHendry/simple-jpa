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


import griffon.core.*
import griffon.util.*
import groovy.swing.SwingBuilder
import javax.swing.DefaultCellEditor
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.UIManager
import java.awt.Window

includeTargets << griffonScript("_GriffonBootstrap")

groovy.ui.Console console

def displayMVCGroups = {
    def list = griffonApp.mvcGroupManager.configurations.collect { k, v ->
        [name: k, alive: griffonApp.mvcGroupManager.findGroup(k)!=null]
    }
    JDialog dialog
    java.awt.Frame.getFrames().each {
        if (it.isFocused()) {
            dialog = new JDialog(it, true)
            dialog.setLocationRelativeTo(it)
        }
    }
    if (!dialog) dialog = new JDialog()
    dialog.title = 'List of Griffon MVCGroups'
    dialog.resizable = false
    dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
    UIManager.getLookAndFeelDefaults().remove("Table.alternateRowColor")
    dialog.contentPane = new SwingBuilder().build {
        scrollPane() {
            table(id: 'table') {
                tableModel(id: 'tableModel', list: list) {
                    propertyColumn(header: 'MVCGroup Name', propertyName: 'name', editable: false)
                    propertyColumn(id: 'aliveColumn', header: 'Alive?', propertyName: 'alive', type: Boolean,
                        cellEditor: new DefaultCellEditor(checkBox())).cellEditor.editingStopped = { e ->
                            String mvcGroupName = table.model.getValueAt(table.selectedRow, 0)
                            MVCGroupConfiguration mvcConfiguration = griffonApp.mvcGroupManager.configurations[mvcGroupName]
                            MVCGroup mvcGroup = griffonApp.mvcGroupManager.findGroup(mvcGroupName)
                            doOutside {
                                if (e.source.cellEditorValue) {
                                    if (!mvcGroup?.alive) {
                                        mvcGroup = mvcConfiguration.create()
                                        console.setVariable("${mvcGroupName}View", mvcGroup.view)
                                        console.setVariable("${mvcGroupName}Model", mvcGroup.model)
                                        console.setVariable("${mvcGroupName}Controller", mvcGroup.controller)
                                    }
                                } else {
                                    if (mvcGroup && mvcGroup.alive) {
                                        mvcGroup.destroy()
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
    dialog.pack()
    dialog.visible = true
}

def delegates = [
        rootContainerDelegate:{
            frame(
                    title: 'GroovyConsole for simple-jpa',
                    //location: [100,100], // in groovy 2.0 use platform default location
                    iconImage: imageIcon("/groovy/ui/ConsoleIcon.png").image,
                    defaultCloseOperation: JFrame.DO_NOTHING_ON_CLOSE,
            ) {
                try {
                    current.locationByPlatform = true
                } catch (Exception e) {
                    current.location = [100, 100] // for 1.4 compatibility
                }
                containingWindows += current
            }
        },
        menuBarDelegate: {arg->
            current.JMenuBar = build(arg)
            current.JMenuBar.add(build {
                displayMVCGroupsAction = action(name: 'MVC Groups', closure: displayMVCGroups)
                menu(text: 'simple-jpa', mnemonic: 'J') {
                    menuItem(displayMVCGroupsAction)
                }
            })
        }
];

target(name: 'simplejpaconsole',
        description: "Load simple-jpa interactive Swing console",
        prehook: null, posthook: null) {
    depends(createConfig)

    jardir = ant.antProject.replaceProperties(buildConfig.griffon.jars.destDir)
    ant.copy(todir: jardir) { fileset(dir: "${griffonHome}/lib/", includes: "jline-*.jar") }

    bootstrap()

    def binding = new Binding()
    griffonApp.startup()
    binding.setVariable('app', griffonApp)
    ['Model','Controller','View'].each { type ->
        griffonApp."get${type}s"().each { k, v ->
            String varName = "${GriffonNameUtils.getPropertyName(k)}${type}"
            binding.setVariable(varName, v)
        }
    }

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    console = new groovy.ui.Console(classLoader, binding)
    console.run(delegates)

    while (Window.windows.any {it.visible}) {
        sleep 3000
    }
}

setDefaultTarget('simplejpaconsole')
