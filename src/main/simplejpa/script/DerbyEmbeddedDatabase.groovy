package simplejpa.script

import java.sql.CallableStatement
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

class DerbyEmbeddedDatabase implements Database {

    @Override
    String getJdbcDriver() {
        'org.apache.derby.jdbc.EmbeddedDriver'
    }

    @Override
    String getDependencyConfig() {
        'org.apache.derby:derby:10.10.1.1'
    }

    @Override
    String getJdbcUrl(String user, String password, String schema) {
        "jdbc:derby:$schema"
    }

    @Override
    String getDialect() {
        'org.hibernate.dialect.DerbyTenSevenDialect'
    }

    @Override
    void setup(String user, String password, String schema, String rootPassword) throws Exception {
        Connection cn
        CallableStatement cs
        PreparedStatement ps

        Properties p = new Properties()
        p['user'] = 'root'
        p['password'] = rootPassword ?: password
        cn = DriverManager.getConnection("jdbc:derby:$schema;create=true", p)
        println "Database [$schema] owned by ${p['user']} created successfully!"

        // Setup user
        ps = cn.prepareStatement('SELECT USERNAME FROM SYS.SYSUSERS WHERE UPPER(USERNAME) = UPPER(?)')
        cs = cn.prepareCall('CALL SYSCS_UTIL.SYSCS_CREATE_USER(?, ?)')

        // DBO
        ps.setString(1, p['user'])
        if (ps.executeQuery().next()) {
            println "Didn't create user ${p['user']} because it is already exists!"
        } else {
            cs.setString(1, p['user'])
            cs.setString(2, p['password'])
            cs.execute()
            println "User ${p['user']} created successfully."
        }

        // User
        ps.setString(1, user)
        if (ps.executeQuery().next()) {
            println "Didn't create user $user because it is already exists!"
        } else {
            cs.setString(1, user)
            cs.setString(2, password)
            cs.execute()
            println "User $user created successfully!"
        }
    }
}
