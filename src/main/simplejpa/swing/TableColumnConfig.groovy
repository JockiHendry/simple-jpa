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

import javax.swing.JTable
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel


class TableColumnConfig extends AbstractFactory {

    @Override
    Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        if (!(builder.getCurrent() instanceof JTable)) {
            throw new IllegalArgumentException("$name must be declared inside JTable")
        }

        JTable table = (JTable) builder.getCurrent()
        TableColumnModel columnModel = table.getColumnModel()

        attributes.each { index, configs ->
            TableColumn column = columnModel.getColumn(index)
            configs.each { method, v ->
                column."$method" = v
            }
        }
        attributes.clear()

        columnModel
    }




}
