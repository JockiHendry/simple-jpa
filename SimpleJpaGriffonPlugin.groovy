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

class SimpleJpaGriffonPlugin {
    // the plugin version
    String version = '0.7'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.2.0 > *'
    // the other plugins this plugin depends on
    Map dependsOn = ['miglayout': '1.0.0', 'swingx-builder': '0.7']
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, qt
    List toolkits = ['swing']
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = 'http://jockihendry.github.io/simple-jpa'
    // URL where source can be found
    String source = 'https://github.com/JockiHendry/simple-jpa'
    // Map of Bnd directives and/or Manifest entries
    // see http://www.aqute.biz/Bnd/Bnd for reference
    Map manifest = [
            'Bundle-Description': 'simple-jpa is a plugin for developing JPA and Swing based desktop application'
    ]

    List authors = [
            [
                    name: 'Jocki Hendry',
                    email: 'jocki.hendry@gmail.com'
            ]
    ]
    String title = 'simple-jpa is a plugin for developing JPA and Swing based desktop application'
    // accepts Markdown syntax. See http://daringfireball.net/projects/markdown/ for details
    String description = '''

**Warning**: During developer preview stage, new release may include features or changes that are
not compatible with previous releases.

Documentation: [http://jockihendry.github.io/simple-jpa](http://jockihendry.github.io/simple-jpa)

News: [http://thesolidsnake.wordpress.com/tag/simple-jpa](http://thesolidsnake.wordpress.com/tag/simple-jpa)

'''
}
