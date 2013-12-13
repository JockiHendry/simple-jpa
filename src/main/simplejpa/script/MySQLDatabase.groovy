package simplejpa.script

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement;

public class MySQLDatabase implements Database {

    @Override
    public String getJdbcDriver() {
        'com.mysql.jdbc.Driver'
    }

    @Override
    String getDependencyConfig() {
        'mysql:mysql-connector-java:5.1.20'
    }

    @Override
    public String getJdbcUrl(String user, String password, String schema) {
        "jdbc:mysql://localhost/$schema"
    }

    @Override
    String getDialect() {
        'org.hibernate.dialect.MySQL5Dialect'
    }

    @Override
    public void setup(String user, String password, String schema, String rootPassword) throws Exception {
        Connection cn
        ResultSet rs
        Statement stmt

        cn = DriverManager.getConnection("jdbc:mysql://localhost", "root", rootPassword)
        stmt = cn.createStatement()

        // Check database
        rs = cn.getMetaData().getCatalogs()
        boolean found = false
        while (rs.next()) {
            if (rs.getString(1).equals(schema)) {
                println "Database $schema already exists. Will not create a new database."
                found = true
                break
            }
        }
        if (!found) {
            stmt.execute("CREATE DATABASE $schema")
            println "Database $schema created successfully!"
        }

        // Check if user already exists
        rs = stmt.executeQuery("SELECT user, host FROM mysql.user")
        found = false
        String host = 'localhost'
        while (rs.next()) {
            if (rs.getString(1).equals(user)) {
                println "User $user already exists. Will not create a new user."
                host = rs.getString(2)
                found = true
                break
            }
        }
        if (!found) {
            stmt.execute("CREATE USER `${user}`@'${host}' IDENTIFIED BY '${password}'")
            println "User $user created successfully!"
        }

        // Grant privilleges
        println "Granting privileges..."
        stmt.execute("GRANT ALL ON $schema.* TO `$user`@'${host}'")
        println "Privileges on $schema granted to $user!"

    }
}
