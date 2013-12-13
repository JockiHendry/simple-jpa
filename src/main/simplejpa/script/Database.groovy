package simplejpa.script;

public interface Database {

    public String getJdbcDriver();

    public String getDependencyConfig();

    public String getJdbcUrl(String user, String password, String schema);

    public String getDialect();

    public void setup(String user, String password, String schema, String rootPassword) throws Exception;

}
