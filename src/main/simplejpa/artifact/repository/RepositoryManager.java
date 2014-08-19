package simplejpa.artifact.repository;

import java.util.Map;

public interface RepositoryManager {

    public Object findRepository(String name);

    public Object doInstantiate(String name, boolean triggerEvent);

    public Map<String, Object> getRepositories();

}
