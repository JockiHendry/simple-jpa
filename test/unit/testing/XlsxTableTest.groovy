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



package testing

import griffon.test.GriffonUnitTestCase
import org.dbunit.dataset.ITableMetaData
import simplejpa.testing.xlsx.XlsxTable

class XlsxTableTest extends GriffonUnitTestCase {

    final def sheet1Xml = new XmlSlurper().parseText('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" mc:Ignorable="x14ac" xmlns:x14ac="http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac"><dimension ref="A1:G4"/><sheetViews><sheetView tabSelected="1" workbookViewId="0"><selection activeCell="F4" sqref="F4"/></sheetView></sheetViews><sheetFormatPr defaultRowHeight="15" x14ac:dyDescent="0.25"/><cols><col min="4" max="4" width="13.85546875" customWidth="1"/><col min="5" max="5" width="12.140625" customWidth="1"/><col min="6" max="6" width="17.85546875" style="1" customWidth="1"/><col min="7" max="7" width="10.7109375" style="2" bestFit="1" customWidth="1"/></cols><sheetData><row r="1" spans="1:7" x14ac:dyDescent="0.25"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c><c r="C1" t="s"><v>2</v></c><c r="D1" t="s"><v>18</v></c><c r="E1" t="s"><v>19</v></c><c r="F1" s="1" t="s"><v>20</v></c><c r="G1" s="2" t="s"><v>21</v></c></row><row r="2" spans="1:7" x14ac:dyDescent="0.25"><c r="A2"><v>1</v></c><c r="B2" t="s"><v>3</v></c><c r="C2"><v>30</v></c><c r="D2" s="1"><v>31119</v></c><c r="E2" s="1" t="b"><v>1</v></c><c r="F2" s="1"><v>41944.416666666664</v></c><c r="G2" s="2"><v>1.2</v></c></row><row r="3" spans="1:7" x14ac:dyDescent="0.25"><c r="A3"><v>2</v></c><c r="B3" t="s"><v>4</v></c><c r="C3"><v>28</v></c><c r="D3" s="1"><v>31825</v></c><c r="E3" s="1" t="b"><v>0</v></c><c r="F3" s="1"><v>41945.5</v></c><c r="G3" s="2"><v>3.75</v></c></row><row r="4" spans="1:7" x14ac:dyDescent="0.25"><c r="A4"><v>3</v></c><c r="B4" t="s"><v>5</v></c><c r="C4"><v>25</v></c><c r="D4" s="1"><v>16689</v></c><c r="E4" s="1" t="b"><v>1</v></c><c r="G4" s="2"><v>2.5</v></c></row></sheetData><pageMargins left="0.7" right="0.7" top="0.75" bottom="0.75" header="0.3" footer="0.3"/><pageSetup paperSize="9" orientation="portrait" r:id="rId1"/><tableParts count="1"><tablePart r:id="rId2"/></tableParts></worksheet>
''')

    final def relationship1Xml = new XmlSlurper().parseText('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/table" Target="../tables/table1.xml"/><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/printerSettings" Target="../printerSettings/printerSettings1.bin"/></Relationships>
''')

    final def sharedStringXml = new XmlSlurper().parseText('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="26" uniqueCount="22"><si><t>id</t></si><si><t>name</t></si><si><t>age</t></si><si><t>jocki</t></si><si><t>lena</t></si><si><t>snake</t></si><si><t>unknown</t></si><si><t>mystery</t></si><si><t>voidless</t></si><si><t>number</t></si><si><t>A1</t></si><si><t>B1</t></si><si><t>java</t></si><si><t>borneo</t></si><si><t>C1</t></si><si><t>sumatra</t></si><si><t>D1</t></si><si><t>bali</t></si><si><t>birthdate</t></si><si><t>flag</t></si><si><t>registered</t></si><si><t>gpa</t></si></sst>
''')

    final def stylesXml = new XmlSlurper().parseText('''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" mc:Ignorable="x14ac" xmlns:x14ac="http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac"><fonts count="1" x14ac:knownFonts="1"><font><sz val="11"/><color theme="1"/><name val="Calibri"/><family val="2"/><charset val="1"/><scheme val="minor"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="14" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="2" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles><dxfs count="5"><dxf><numFmt numFmtId="2" formatCode="0.00"/></dxf><dxf><numFmt numFmtId="19" formatCode="dd/mm/yyyy"/></dxf><dxf><numFmt numFmtId="19" formatCode="dd/mm/yyyy"/></dxf><dxf><font><b/><i val="0"/></font><fill><patternFill><bgColor rgb="FFD7D7D7"/></patternFill></fill></dxf><dxf><font><b val="0"/><i val="0"/></font><fill><patternFill patternType="none"><bgColor indexed="65"/></patternFill></fill></dxf></dxfs><tableStyles count="1" defaultTableStyle="TableStyleMedium2" defaultPivotStyle="PivotStyleLight16"><tableStyle name="MySqlDefault" pivot="0" table="0" count="2"><tableStyleElement type="wholeTable" dxfId="4"/><tableStyleElement type="headerRow" dxfId="3"/></tableStyle></tableStyles><extLst><ext uri="{EB79DEF2-80B8-43e5-95BD-54CBDDF9020C}" xmlns:x14="http://schemas.microsoft.com/office/spreadsheetml/2009/9/main"><x14:slicerStyles defaultSlicerStyle="SlicerStyleLight1"/></ext></extLst></styleSheet>
''')

    void testTableMetaData() {
        XlsxTable table = new XlsxTable('sheet1', sheet1Xml, relationship1Xml, sharedStringXml, stylesXml)
        ITableMetaData metaData = table.getTableMetaData()

        assertEquals('sheet1', metaData.tableName)
        assertEquals(7, metaData.columns.size())
        assertEquals('id', metaData.columns[0].columnName)
        assertEquals('name', metaData.columns[1].columnName)
        assertEquals('age', metaData.columns[2].columnName)
        assertEquals('birthdate', metaData.columns[3].columnName)
        assertEquals('flag', metaData.columns[4].columnName)
        assertEquals('registered', metaData.columns[5].columnName)
        assertEquals('gpa', metaData.columns[6].columnName)
    }

    void testGetRowCount() {
        XlsxTable table = new XlsxTable('sheet1', sheet1Xml, relationship1Xml, sharedStringXml, stylesXml)
        assertEquals(3, table.getRowCount())
    }

    void testGetValue() {
        XlsxTable table = new XlsxTable('sheet1', sheet1Xml, relationship1Xml, sharedStringXml, stylesXml)
        Calendar c = Calendar.getInstance()

        assertEquals(1, table.getValue(0, 'id') as Integer)
        assertEquals('jocki', table.getValue(0, 'name'))
        assertEquals(30, table.getValue(0, 'age') as Integer)
        c.setTime(table.getValue(0, 'birthdate'))
        assertEquals(13, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(2, c.get(Calendar.MONTH))
        assertEquals(1985, c.get(Calendar.YEAR))
        assertTrue(table.getValue(0, 'flag') as Boolean)
        c.setTime(table.getValue(0, 'registered'))
        assertEquals(1, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, c.get(Calendar.MONTH))
        assertEquals(2014, c.get(Calendar.YEAR))
        assertEquals(10, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertEquals(new BigDecimal('1.20'), table.getValue(0, 'gpa') as BigDecimal)

        assertEquals(2, table.getValue(1, 'id') as Integer)
        assertEquals('lena', table.getValue(1, 'name'))
        assertEquals(28, table.getValue(1, 'age') as Integer)
        c.setTime(table.getValue(1, 'birthdate'))
        assertEquals(17, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(1, c.get(Calendar.MONTH))
        assertEquals(1987, c.get(Calendar.YEAR))
        assertFalse(table.getValue(1, 'flag') as Boolean)
        c.setTime(table.getValue(1, 'registered'))
        assertEquals(2, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, c.get(Calendar.MONTH))
        assertEquals(2014, c.get(Calendar.YEAR))
        assertEquals(12, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertEquals(new BigDecimal('3.75'), table.getValue(1, 'gpa') as BigDecimal)

        assertEquals(3, table.getValue(2, 'id') as Integer)
        assertEquals('snake', table.getValue(2, 'name'))
        assertEquals(25, table.getValue(2, 'age') as Integer)
        c.setTime(table.getValue(2, 'birthdate'))
        assertEquals(9, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, c.get(Calendar.MONTH))
        assertEquals(1945, c.get(Calendar.YEAR))
        assertTrue(table.getValue(2, 'flag') as Boolean)
        assertNull(table.getValue(2, 'registered'))
        assertEquals(new BigDecimal('2.50'), table.getValue(2, 'gpa') as BigDecimal)
    }

}
