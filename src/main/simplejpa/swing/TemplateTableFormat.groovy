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

import ca.odell.glazedlists.gui.TableFormat
import groovy.text.SimpleTemplateEngine


class TemplateTableFormat implements TableFormat {

    List columnName
    List columnExpression
    private List template = []

    public TemplateTableFormat(List columnName, List columnExpression) {
        this.columnName = columnName
        this.columnExpression = columnExpression
        SimpleTemplateEngine templateEngine = new SimpleTemplateEngine()
        columnExpression.each { template << templateEngine.createTemplate(it) }
    }

    @Override
    int getColumnCount() {
        columnName.size()
    }

    @Override
    String getColumnName(int i) {
        columnName[i]
    }

    @Override
    Object getColumnValue(Object e, int i) {
        String result = e==null? null: template[i].make(["value": e])
        result?:""
    }
}
