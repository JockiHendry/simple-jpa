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

package simplejpa.swing

import groovy.text.SimpleTemplateEngine
import groovy.text.Template

import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxRenderer
import java.awt.*

@Deprecated
public class TemplateListCellRenderer extends JLabel implements ListCellRenderer {

    private Template template

    public TemplateListCellRenderer(String templateString) {
        setOpaque(true)
        setBorder(BorderFactory.createEmptyBorder(2,5,2,5))
        this.template = new SimpleTemplateEngine().createTemplate(templateString)
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }
        else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setFont(list.getFont());

        if (value instanceof Icon) {
            setIcon((Icon)value);
        } else {
            String result = value ? TemplateRenderer.make(template, value) : null
            setText(result?result.toString():"...");
        }
        return this;
    }

}
