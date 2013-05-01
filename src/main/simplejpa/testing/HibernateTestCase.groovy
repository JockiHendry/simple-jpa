package simplejpa.testing

import griffon.core.*
import griffon.test.GriffonUnitTestCase
import org.dbunit.database.DatabaseConnection
import org.dbunit.database.IDatabaseConnection
import org.dbunit.dataset.IDataSet
import org.dbunit.operation.DatabaseOperation
import org.dbunit.util.fileloader.CsvDataFileLoader
import org.dbunit.util.fileloader.FlatXmlDataFileLoader
import org.dbunit.util.fileloader.XlsDataFileLoader
import org.hibernate.internal.SessionImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class HibernateTestCase extends GriffonUnitTestCase {

    private static final Logger log = LoggerFactory.getLogger(HibernateTestCase)

    GriffonApplication app
    GriffonController controller
    GriffonModel model
    GriffonView view
    IDatabaseConnection connection
    IDataSet dataSet

    void setUpDatabase(String mvcGroup, String dataFile, boolean refresh = false) {
        app.startup()
        if (app.controllers[mvcGroup]==null) {
            app.createMVCGroup(mvcGroup)
        }
        controller = app.controllers[mvcGroup]
        model = app.models[mvcGroup]
        view = app.views[mvcGroup]

        connection = new DatabaseConnection(((SessionImpl)(controller.getEntityManager().getDelegate())).connection())
        if (dataFile.endsWith(".xml")) {
            dataSet = new FlatXmlDataFileLoader().load(dataFile)
        } else if (dataFile.endsWith(".xls")) {
            dataSet = new XlsDataFileLoader().load(dataFile)
        } else {
            dataSet = new CsvDataFileLoader().load(dataFile)
        }

        cleanInsert()
    }

    void cleanInsert() {
        DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet)
        if (!connection.connection.autoCommit) {
            connection.connection.commit()
        }
    }

    void deleteAll() {
        DatabaseOperation.DELETE_ALL.execute(connection, dataSet)
        if (!connection.connection.autoCommit) {
            connection.connection.commit()
        }
    }

    void refresh() {
        DatabaseOperation.REFRESH.execute(connection, dataSet)
        if (!connection.connection.autoCommit) {
            connection.connection.commit()
        }
    }

}
