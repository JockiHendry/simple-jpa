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

import javax.persistence.PostLoad
import javax.persistence.PrePersist
import javax.persistence.PreUpdate

class AuditingEntityListener {

    @PrePersist
    void createdDate(Object target) {
        target."createdDate" = Calendar.instance.time
    }

    @PreUpdate
    void modifiedDate(Object target) {
        target."modifiedDate" = Calendar.instance.time
    }

    @PostLoad
    void checkThreadSafety(Object target) {
        /**
         * EM is not thread-safe so an entity shouldn't be used by
         * other threads beside the one that creates it.
         */
        if (SimpleJpaUtil.instance.handlers[0].isCheckThreadSafeLoading) {
            Thread supposedThread = SimpleJpaUtil.instance.getThreadForEntity(target)
            if (Thread.currentThread() != supposedThread) {
                throw new ConcurrentModificationException("${target.class} ${target.id} is loaded " +
                    "from thread ${Thread.currentThread().name} but should be ${supposedThread.name}")
            }
        }
    }

}
