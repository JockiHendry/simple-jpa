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



package simplejpa.testing

import org.dbunit.dataset.IDataSet
import org.dbunit.util.fileloader.CsvDataFileLoader
import org.dbunit.util.fileloader.FlatXmlDataFileLoader
import org.dbunit.util.fileloader.XlsDataFileLoader
import simplejpa.testing.xlsx.XlsxDataFileLoader

class DataSetCache {

    Map dataSets = [:]

    public IDataSet get(String resourceName) {
        IDataSet result = dataSets[resourceName]
        if (!result) {
            if (resourceName.endsWith(".xml")) {
                result = new FlatXmlDataFileLoader().load(resourceName)
            } else if (resourceName.endsWith(".xls")) {
                result = new XlsDataFileLoader().load(resourceName)
            } else if (resourceName.endsWith('.xlsx')) {
                result = new XlsxDataFileLoader().load(resourceName)
            } else {
                result = new CsvDataFileLoader().load(resourceName)
            }
            dataSets[resourceName] = result
        }
        result
    }

}
