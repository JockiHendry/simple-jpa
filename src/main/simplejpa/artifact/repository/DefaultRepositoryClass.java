package simplejpa.artifact.repository;

import griffon.core.GriffonApplication;
import org.codehaus.griffon.runtime.core.DefaultGriffonClass;

public class DefaultRepositoryClass extends DefaultGriffonClass {

    public DefaultRepositoryClass(GriffonApplication app, Class<?> clazz) {
        super(app, clazz, "repository", "Repository");
    }

}
