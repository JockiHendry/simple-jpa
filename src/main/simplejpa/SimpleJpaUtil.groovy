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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.transaction.TransactionHolder

import javax.persistence.EntityManagerFactory

class SimpleJpaUtil {

    public static SimpleJpaUtil instance = new SimpleJpaUtil()
    private static final Logger LOG = LoggerFactory.getLogger(SimpleJpaUtil)

    private List<SimpleJpaHandler> handlerLists = []
    EntityManagerFactory entityManagerFactory

    private SimpleJpaUtil() {}

    public void registerHandler(SimpleJpaHandler handler) {
        LOG.debug "Registering $handler"
        handlerLists << handler
    }

    public List getHandlers() {
        handlerLists.asImmutable()
    }

    public Thread getThreadForEntity(Object object) {
        Thread result = null
        handlerLists.find { SimpleJpaHandler handler ->
            handler.mapTransactionHolder.any {  Thread thread, TransactionHolder th ->
                if (th.em.contains(object)) {
                    result = thread
                    true
                }
                false
            }
        }
        result
    }

    public Map getEMFProperties() {
        entityManagerFactory.properties
    }

    public String getDbUsername() {
        getEMFProperties()['javax.persistence.jdbc.user']
    }

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

    // Methods for globally manipulating handlers here!
}
