package simplejpa.testing

import griffon.core.*
import griffon.test.GriffonUnitTestCase
import org.dbunit.database.DatabaseConnection
import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.database.IDatabaseConnection
import org.dbunit.dataset.FilteredDataSet
import org.dbunit.dataset.IDataSet
import org.dbunit.dataset.filter.ITableFilter
import org.dbunit.operation.DatabaseOperation
import org.dbunit.util.fileloader.CsvDataFileLoader
import org.dbunit.util.fileloader.FlatXmlDataFileLoader
import org.dbunit.util.fileloader.XlsDataFileLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager

abstract class DbUnitTestCase extends GriffonUnitTestCase {

    private static final Logger log = LoggerFactory.getLogger(DbUnitTestCase)

    GriffonApplication app
    GriffonController controller
    GriffonModel model
    GriffonView view
    IDatabaseConnection connection
    IDataSet dataSet

    void setUpDatabase(String mvcGroup, String dataFile, DatabaseOperation preOperation = null) {

        String dbUrl, dbUser, dbPassword
        boolean dbAutoCommit = true

        InputStream is = app.getResourceAsStream("metainf/persistence.xml")

        try {
            def persistence = new XmlParser().parse(is)
            def persistenceUnitRoot = persistence."persistence-unit".find { it.'@name'=='default' }
            if (persistenceUnitRoot) {
                dbUrl = persistenceUnitRoot.properties.property.find { it.'@name'=='javax.persistence.jdbc.url' }.'@value'
                dbUser = persistenceUnitRoot.properties.property.find { it.'@name'=='javax.persistence.jdbc.user' }.'@value'
                dbPassword = persistenceUnitRoot.properties.property.find { it.'@name'=='javax.persistence.jdbc.password' }.'@value'
                dbAutoCommit = persistenceUnitRoot.properties.property.find { it.'@name'=='hibernate.connection.autocommit' }.'@value'
            } else {
                dbUrl = "jdbc:mysql://localhost:3306/default"
                dbUser = "root"
                dbPassword = ""
            }

            app.startup()
            if (app.mvcGroupManager.findGroup(mvcGroup) != null) {
                if (app.controllers[mvcGroup]==null) {
                    app.createMVCGroup(mvcGroup)
                }
                controller = app.controllers[mvcGroup]
                model = app.models[mvcGroup]
                view = app.views[mvcGroup]
            }

            Connection jdbcConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
            jdbcConnection.autoCommit = dbAutoCommit
            connection = new DatabaseConnection(jdbcConnection)

            if (dataFile.endsWith(".xml")) {
                dataSet = new FlatXmlDataFileLoader().load(dataFile)
            } else if (dataFile.endsWith(".xls")) {
                dataSet = new XlsDataFileLoader().load(dataFile)
            } else {
                dataSet = new CsvDataFileLoader().load(dataFile)
            }

            ITableFilter filter = new DatabaseSequenceFilter(connection)
            dataSet = new FilteredDataSet(filter, dataSet)
            if (preOperation) preOperation.execute(connection, dataSet)
            cleanInsert()

        } finally {
            is?.close()
        }
    }

    void cleanInsert() {
        DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet)
        if (!connection.connection.autoCommit) {
            connection.connection.commit()
        }
    }

    void truncateTable() {
        DatabaseOperation.TRUNCATE_TABLE.execute(connection, dataSet)
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
