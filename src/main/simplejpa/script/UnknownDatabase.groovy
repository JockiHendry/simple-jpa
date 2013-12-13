package simplejpa.script

class UnknownDatabase implements Database {

    @Override
    String getJdbcDriver() {
        '// TODO: Add JDBC Driver here!'
    }

    @Override
    String getDependencyConfig() {
        '// TODO: Add JDBC Driver Dependency here!'
    }

    @Override
    String getJdbcUrl(String user, String password, String schema) {
        "// TODO: Add JDBC URL for user $user, password $password and schema $schema here!"
    }

    @Override
    String getDialect() {
        "// TODO: Add database dialect here!"
    }

    @Override
    void setup(String user, String password, String schema, String rootPassword) throws Exception {
    }
}
