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

import org.dbunit.dataset.DataSetException
import org.dbunit.dataset.IDataSet
import org.dbunit.util.fileloader.AbstractDataFileLoader

class XlsxDataFileLoader extends AbstractDataFileLoader {

    XlsxDataFileLoader() {}

    XlsxDataFileLoader(Map ro) {
        super(ro)
    }

    XlsxDataFileLoader(Map ro, Map rs) {
        super(ro, rs)
    }

    @Override
    protected IDataSet loadDataSet(URL url) throws DataSetException, IOException {
        new XlsxDataSet(new File(url.toURI()))
    }

}
