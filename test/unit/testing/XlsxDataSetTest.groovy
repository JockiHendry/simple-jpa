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
import simplejpa.testing.xlsx.XlsxDataSet
import simplejpa.testing.xlsx.XlsxTable

class XlsxDataSetTest extends GriffonUnitTestCase {

    void testCreate() {
        File file = new File(Thread.currentThread().contextClassLoader.getResource('testing/sample.xlsx').toURI())
        XlsxDataSet dataSet = new XlsxDataSet(file)

        XlsxTable studentTable = dataSet.getTable('student')
        assertNotNull(studentTable)
        assertEquals(3, studentTable.rowCount)
        assertEquals(7, studentTable.tableMetaData.columns.size())
        XlsxTable teacherTable = dataSet.getTable('teacher')
        assertNotNull(teacherTable)
        assertEquals(3, teacherTable.rowCount)
        assertEquals(2, teacherTable.tableMetaData.columns.size())
        XlsxTable classroomTable = dataSet.getTable('classroom')
        assertNotNull(classroomTable)
        assertEquals(4, classroomTable.rowCount)
        assertEquals(3, classroomTable.tableMetaData.columns.size())
    }

}
