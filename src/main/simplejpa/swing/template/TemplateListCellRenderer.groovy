/*
 * Copyright 2015 Jocki Hendry.
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



package simplejpa.swing.template

import javax.swing.*
import java.awt.*

public class TemplateListCellRenderer extends DefaultListCellRenderer {

    TemplateRenderer templateRenderer

    public TemplateListCellRenderer(def template) {
        super()
        templateRenderer = new TemplateRenderer()
        templateRenderer.add(template)
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        String result = (value ? templateRenderer.make(value) : null)?: '...'
        super.getListCellRendererComponent(list, result, index, isSelected, cellHasFocus)
    }

}
