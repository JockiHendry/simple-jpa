/*
 * Copyright 2015 Jocki Hendry.
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

package simplejpa.artifact.repository;

import griffon.core.*;
import griffon.exceptions.NewInstanceCreationException;
import griffon.util.ApplicationHolder;
import griffon.util.GriffonNameUtils;
import groovy.lang.MetaClass;
import groovy.lang.MetaProperty;
import org.codehaus.griffon.runtime.core.ArtifactHandlerAdapter;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simplejpa.SimpleJpaUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static griffon.util.ConfigUtils.getConfigValueAsBoolean;
import static griffon.util.GriffonExceptionHandler.sanitize;
import static java.util.Arrays.asList;

public class RepositoryArtifactHandler extends ArtifactHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryArtifactHandler.class);

    public class AbstractRepositoryManager implements RepositoryManager {
        private final Map<String, Object> instances = new ConcurrentHashMap<>();

        public AbstractRepositoryManager() {
            getApp().addShutdownHandler(new RepositoryManagerShutdownHandler());
        }

        public Map<String, Object> getRepositories() {
            return Collections.unmodifiableMap(instances);
        }

        public Object findRepository(String name) {
            if (!name.endsWith("Repository")) {
                name += "Repository";
            }
            // Change first letter of the name to lower case if necessary
            name = GriffonNameUtils.getPropertyNameRepresentation(name);
            Object repositoryInstance = instances.get(name);
            if (repositoryInstance == null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Instantiating repository identified by '" + name + "'");
                }
                repositoryInstance = doInstantiate(name, true);
            }
            return repositoryInstance;
        }

        public Object doInstantiate(String name, boolean triggerEvent) {
            Object instance = null;
            GriffonClass griffonClass = findClassFor(name);
            if (griffonClass != null) {
                try {
                    instance = griffonClass.getClazz().newInstance();
                } catch (Exception e) {
                    Throwable targetException = null;
                    if (e instanceof InvocationTargetException) {
                        targetException = ((InvocationTargetException) e).getTargetException();
                    } else {
                        targetException = e;
                    }
                    throw new NewInstanceCreationException("Could not create a new instance of class " + griffonClass.getClazz().getName(), sanitize(targetException));
                }
                instances.put(name, instance);
                if (griffonClass.hasProperty("app")) {
                    InvokerHelper.setProperty(instance, "app", getApp());
                }
                getApp().addApplicationEventListener(instance);
                if (triggerEvent) {
                    getApp().event(GriffonApplication.Event.NEW_INSTANCE.getName(), asList(griffonClass.getClazz(), "repository", instance));
                }
            }
            return instance;
        }

        private class RepositoryManagerShutdownHandler implements ShutdownHandler {

            @Override
            public boolean canShutdown(GriffonApplication application) {
                return true;
            }

            @Override
            public void onShutdown(GriffonApplication application) {
                for(Map.Entry<String, Object> entry : instances.entrySet()) {
                    Object repository = entry.getValue();
                    application.removeApplicationEventListener(repository);
                    application.event(GriffonApplication.Event.DESTROY_INSTANCE.getName(), Arrays.asList(repository.getClass(), "repository", repository));
                }
            }

        }

    }

    private final RepositoryManager repositoryManager;

    public RepositoryArtifactHandler(GriffonApplication app) {
        super(app, "repository", "Repository");
        repositoryManager = new AbstractRepositoryManager();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Registering " + repositoryManager + " to SimpleJpaUtil.");
        }
        SimpleJpaUtil.instance.setRepositoryManager(repositoryManager);
    }

    @Override
    protected GriffonClass newGriffonClassInstance(Class clazz) {
        return new DefaultRepositoryClass(getApp(), clazz);
    }

    @Override
    public void initialize(ArtifactInfo[] artifacts) {
        super.initialize(artifacts);
        getApp().addApplicationEventListener(this);
        if (isEagerInstantiationEnabled()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Instantiating repository instances eagerly");
            }
            for (ArtifactInfo artifactInfo : artifacts) {
                GriffonClass griffonClass = getClassFor(artifactInfo.getClazz());
                repositoryManager.doInstantiate(griffonClass.getPropertyName(), false);
            }
            for (ArtifactInfo artifactInfo : artifacts) {
                GriffonClass griffonClass = getClassFor(artifactInfo.getClazz());
                Object repositoryInstance = repositoryManager.findRepository(griffonClass.getPropertyName());
                getApp().event(GriffonApplication.Event.NEW_INSTANCE.getName(), asList(griffonClass.getClazz(), "repository", repositoryInstance));
            }
        }
    }

    /**
     * Application event listener.<p>
     * Lazily injects repository instances if {@code app.config.griffon.basic_injection.disable}
     * is not set to true
     */
    public void onNewInstance(Class klass, String t, Object instance) {
        MetaClass metaClass = InvokerHelper.getMetaClass(instance);
        for (MetaProperty property : metaClass.getProperties()) {
            String propertyName = property.getName();
            if (!propertyName.endsWith(getTrailing())) continue;
            Object repositoryInstance = repositoryManager.findRepository(propertyName);
            if (repositoryInstance != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Injecting repository " + repositoryInstance + " on " + instance + " using property '" + propertyName + "'");
                }
                InvokerHelper.setProperty(instance, propertyName, repositoryInstance);
            }
        }
    }

    private boolean isEagerInstantiationEnabled() {
        return getConfigValueAsBoolean(getApp().getConfig(), "griffon.simplejpa.repository.eager_instantiation", false);
    }

}