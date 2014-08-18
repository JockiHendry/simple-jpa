/*
 * Copyright 2013 Jocki Hendry.
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

package simplejpa

import griffon.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.artifact.repository.RepositoryArtifactHandler.RepositoryManager
import simplejpa.obfuscate.Obfuscator
import simplejpa.transaction.TransactionHolder

import javax.persistence.EntityManagerFactory
import javax.persistence.PersistenceUnitUtil

class SimpleJpaUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleJpaUtil)

    public static SimpleJpaUtil instance = new SimpleJpaUtil()
    public static final String FILE_ANNOTATED = 'simple-jpa-annotated.txt'
    public static Map container = [:]

    SimpleJpaHandler handler
    EntityManagerFactory entityManagerFactory
    RepositoryManager repositoryManager

    private SimpleJpaUtil() {}

    public Map getEMFProperties() {
        entityManagerFactory.properties
    }

    public String getDbUsername() {
        getEMFProperties()['javax.persistence.jdbc.user']
    }

    @Deprecated
    public String getDbPassword() {
        getEMFProperties()['javax.persistence.jdbc.password']
    }

    public String getDbUrl() {
        getEMFProperties()['javax.persistence.jdbc.url']
    }

    public String getDbName() {
        def matcher = getDbUrl() =~ /jdbc:.+\/([^?]+).*/
        matcher[0][1]
    }

    public Map getJpaConfig() {
        // Read properties in Config.groovy
        def app = ApplicationHolder.application
        def config = app.config.griffon.simplejpa.entityManager.properties
        LOG.debug "Properties overrides from Config.groovy: $config"

        // Read from properties file (default to simplejpa.properties) using Groovy ConfigSlurper format
        def configFileName = System.getProperty("griffon.simplejpa.entityManager.propertiesFile")?.trim()
        if (!configFileName) {
            configFileName = ConfigUtils.getConfigValueAsString(app.config, "griffon.simplejpa.entityManager.propertiesFile",
                    "simplejpa.properties")
        }
        File configFile = new File(configFileName)
        if (configFile.exists()) {
            LOG.debug "Reading properties from file ${configFile.absolutePath}"
            def configFromFile = new ConfigSlurper(Environment.current.name).parse(configFile.toURI().toURL())
            LOG.debug "Properties overrides from file: $configFromFile"
            config.merge(configFromFile)
        }

        // Read from system properties
        config = config.flatten()
        System.properties.each { String k, v ->
            if (k.startsWith('javax.persistence')) {
                config[k] = v
            }
        }

        LOG.debug "Properties overrides: $config"

        // Decrypting value
        config.each {k,v ->
            if (v.toString().startsWith('obfuscated:')) {
                config[k] = Obfuscator.reverse(v)
            }
        }

        config
    }

    public PersistenceUnitUtil getPersistenceUnitUtil() {
        entityManagerFactory.getPersistenceUnitUtil()
    }

    // Methods for globally manipulating handlers here!
}
