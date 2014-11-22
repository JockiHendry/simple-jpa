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

import org.dbunit.dataset.AbstractDataSet
import org.dbunit.dataset.DataSetException
import org.dbunit.dataset.DefaultTableIterator
import org.dbunit.dataset.ITable
import org.dbunit.dataset.ITableIterator
import org.dbunit.dataset.OrderedTableNameMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.zip.ZipFile

class XlsxDataSet extends AbstractDataSet {

    private static final Logger log = LoggerFactory.getLogger(XlsxDataSet)

    private final OrderedTableNameMap _tables
    private def workbookXml, relationshipXml, sharedStringsXml, stylesXml

    public XlsxDataSet(File file) throws IOException, DataSetException {
        this(new ZipFile(file))
    }

    public XlsxDataSet(ZipFile zipFile) throws IOException, DataSetException {
        _tables = createTableNameMap()
        workbookXml = new XmlSlurper().parse(zipFile.getInputStream(zipFile.getEntry('xl/workbook.xml')))
        relationshipXml = new XmlSlurper().parse(zipFile.getInputStream(zipFile.getEntry('xl/_rels/workbook.xml.rels')))

        // Find shared strings and styles XML
        def r = relationshipXml.Relationship.find { it.@Type.text() == 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings' }
        if (r && r.size()) {
            sharedStringsXml = new XmlSlurper().parse(zipFile.getInputStream(zipFile.getEntry('xl/' + r.@Target.text())))
        }
        r = relationshipXml.Relationship.find { it.@Type.text() == 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles' }
        if (r && r.size()) {
            stylesXml = new XmlSlurper().parse(zipFile.getInputStream(zipFile.getEntry('xl/' + r.@Target.text())))
        }

        // Find worksheets
        workbookXml.sheets.sheet.each { sheet ->
            r = relationshipXml.Relationship.find { it.@Id.text() == sheet.@'r:id'.text() }
            String target = r.@Target.text()
            def sheetXml = new XmlSlurper().parse(zipFile.getInputStream(zipFile.getEntry('xl/' + target)))
            def tmp = target.split('/')
            tmp[tmp.length-1] = '_rels/' + tmp[tmp.length-1] + '.rels'
            String relationName = tmp.join('/')
            def zipEntry = zipFile.getEntry('xl/' + relationName)
            def sheetRelationXml
            if (!zipEntry) {
                log.debug "Part xl/$relationName is not found."
            } else {
                sheetRelationXml = new XmlSlurper().parse(zipFile.getInputStream(zipFile.getEntry('xl/' + relationName)))
            }
            XlsxTable table = new XlsxTable(sheet.@name.text(), sheetXml, sheetRelationXml, sharedStringsXml, stylesXml)
            _tables.add(sheet.@name.text(), table)
        }
    }

    @Override
    protected ITableIterator createIterator(boolean reversed) throws DataSetException {
        new DefaultTableIterator(_tables.orderedValues().toArray(new ITable[0]), reversed)
    }

}
