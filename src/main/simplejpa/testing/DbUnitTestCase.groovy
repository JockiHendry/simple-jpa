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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

abstract class DbUnitTestCase extends GriffonUnitTestCase {

    private static final Logger log = LoggerFactory.getLogger(DbUnitTestCase)

    public static IDatabaseConnection CONNECTION

    public static final List<String> CLEANING_SQL
    public static final List<String> BEFORE_SQL
    public static final List<String> AFTER_SQL

    static {
        griffon.util.ApplicationHolder.application.startup()

        String dbUrl, dbUser, dbPassword
        boolean dbAutoCommit = true
        InputStream is = griffon.util.ApplicationHolder.application.getResourceAsStream("metainf/persistence.xml")
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
            Connection jdbcConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
            jdbcConnection.autoCommit = dbAutoCommit
            CONNECTION = new DatabaseConnection(jdbcConnection)
        } finally {
            is?.close()
        }

        // Check if `clean.sql` is exists
        InputStream cleanSqlStream = Thread.currentThread().contextClassLoader.getResourceAsStream('clean.sql')
        if (cleanSqlStream) {
            CLEANING_SQL = new ArrayList<>()
            cleanSqlStream.eachLine { CLEANING_SQL.add(it) }
        }

        // Check if `before.sql` is exists
        InputStream beforeSqlStream = Thread.currentThread().contextClassLoader.getResourceAsStream('before.sql')
        if (beforeSqlStream ) {
            BEFORE_SQL = new ArrayList<>()
            beforeSqlStream .eachLine { BEFORE_SQL.add(it) }
        }

        // Check if `after.sql` is exists
        InputStream afterSqlStream = Thread.currentThread().contextClassLoader.getResourceAsStream('after.sql')
        if (afterSqlStream ) {
            AFTER_SQL = new ArrayList<>()
            afterSqlStream .eachLine { AFTER_SQL.add(it) }
        }
    }

    GriffonApplication app
    GriffonController controller
    GriffonModel model
    GriffonView view
    IDataSet dataSet

    void loadMVC(String mvcGroup) {
        if (app.mvcGroupManager.findGroup(mvcGroup) != null) {
            if (app.controllers[mvcGroup]==null) {
                app.createMVCGroup(mvcGroup)
            }
            controller = app.controllers[mvcGroup]
            model = app.models[mvcGroup]
            view = app.views[mvcGroup]
        }
    }

    void setUpDatabase(String dataFile, DatabaseOperation preOperation = null,
                       DatabaseOperation insertOperation = DatabaseOperation.CLEAN_INSERT) {
        if (dataFile.endsWith(".xml")) {
            dataSet = new FlatXmlDataFileLoader().load(dataFile)
        } else if (dataFile.endsWith(".xls")) {
            dataSet = new XlsDataFileLoader().load(dataFile)
        } else {
            dataSet = new CsvDataFileLoader().load(dataFile)
        }

        beforeSetupDatabase()

        ITableFilter filter = new DatabaseSequenceFilter(CONNECTION)
        dataSet = new FilteredDataSet(filter, dataSet)
        if (preOperation) preOperation.execute(CONNECTION, dataSet)

        cleanDataset()
        insertOperation.execute(CONNECTION, dataSet)

        afterSetupDatabase()
    }

    void beforeSetupDatabase() {
        if (BEFORE_SQL) {
            execute(BEFORE_SQL)
        }
    }

    void afterSetupDatabase() {
        if (AFTER_SQL) {
            execute(AFTER_SQL)
        }
    }

    void cleanDataset() {
        if (CLEANING_SQL) {
            execute(CLEANING_SQL)
        }
    }

    void execute(List<String> sqls) {
        Connection conn = CONNECTION.connection
        Statement statement = conn.createStatement()
        try {
            sqls.each { String line ->
                statement.addBatch(line)
            }
            statement.executeBatch()
        } finally {
            statement.close()
        }
    }

    void cleanInsert() {
        DatabaseOperation.CLEAN_INSERT.execute(CONNECTION, dataSet)
        if (!CONNECTION.connection.autoCommit) {
            CONNECTION.connection.commit()
        }
    }

    void truncateTable() {
        DatabaseOperation.TRUNCATE_TABLE.execute(CONNECTION, dataSet)
    }

    void deleteAll() {
        DatabaseOperation.DELETE_ALL.execute(CONNECTION, dataSet)
        if (!CONNECTION.connection.autoCommit) {
            CONNECTION.connection.commit()
        }
    }

    void refresh() {
        DatabaseOperation.REFRESH.execute(CONNECTION, dataSet)
        if (!CONNECTION.connection.autoCommit) {
            CONNECTION.connection.commit()
        }
    }

}
