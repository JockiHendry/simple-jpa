/*
 * Copyright 2014 Jocki Hendry.
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



package simplejpa.testing.xlsx

import org.dbunit.dataset.AbstractTable
import org.dbunit.dataset.Column
import org.dbunit.dataset.DataSetException
import org.dbunit.dataset.DefaultTableMetaData
import org.dbunit.dataset.ITableMetaData
import org.dbunit.dataset.datatype.DataType

class XlsxTable extends AbstractTable {

    private ITableMetaData metaData
    private def sheetXml, relationShipXml, sharedStringsXml, stylesXml

    public XlsxTable(String sheetName, def sheetXml, def relationShipXml, def sharedStringsXml, def stylesXml) {
        this.sheetXml = sheetXml
        this.relationShipXml = relationShipXml
        this.sharedStringsXml = sharedStringsXml
        this.stylesXml = stylesXml

        def columns
        if (sheetXml.sheetData.row.size()==0) {
            columns = new Column[0]
        } else {
            columns = sheetXml.sheetData.row[0].c.collect { new Column(getValueAsString(it.v.text()), DataType.UNKNOWN) }
        }
        this.metaData = new DefaultTableMetaData(sheetName, (Column[]) columns.toArray())
    }

    @Override
    ITableMetaData getTableMetaData() {
        metaData
    }

    @Override
    int getRowCount() {
        sheetXml.sheetData.row.size() - 1
    }

    @Override
    Object getValue(int rowIndex, String columnName) throws DataSetException {
        assertValidRowIndex(rowIndex)
        getValue(rowIndex+1, getColumnIndex(columnName))
    }

    def getValue(int rowIndex, int columnIndex) {
        def c = sheetXml.sheetData.row[rowIndex].c.find { it.@r.text() == "${(('A' as char) +columnIndex) as char}${rowIndex+1}"}
        def value = c?.v?.text()?: null
        if (value) {
            if (c.@t.text() == 's') {
                value = getValueAsString(value)
            } else if (c.@t.text() == 'b') {
                value = getValueAsBoolean(value)
            } else if (!c.@s.isEmpty()) {
                def numFmtId = stylesXml.cellXfs.xf[c.@s.text() as Integer].@numFmtId.text()
                if ((14..22).contains(numFmtId as Integer)) {
                    value = value as Double
                    int days = (int) Math.floor(value)
                    int time = (int)((value - days) * 86400 + 0.5) * 1000
                    Calendar cal = Calendar.getInstance()
                    cal.set(1900, 0, (value as Integer) - 1, 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, time)
                    value = cal.getTime()
                }
            }
        }
        value
    }

    String getValueAsString(def value) {
        sharedStringsXml.si[value as Integer].t.text()
    }

    Boolean getValueAsBoolean(def value) {
        if (value == '1') {
            return true
        } else if (value == '0') {
            return false
        } else {
            throw new IllegalArgumentException("Invalid boolean value: [$value]")
        }
    }

}
