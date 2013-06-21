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

class SimpleJpaUtil {

    public static SimpleJpaUtil instance = new SimpleJpaUtil()
    private static final Logger LOG = LoggerFactory.getLogger(SimpleJpaUtil)

    private List handlerLists = []

    public void registerHandler(SimpleJpaHandler handler) {
        LOG.debug "Registering $handler"
        handlerLists << handler
    }

    public List getHandlers() {
        handlerLists.asImmutable()
    }

    // Methods for globally manipulating handlers here!
}
