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



package simplejpa.transaction

import org.codehaus.groovy.runtime.StackTraceUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.persistence.EntityManager
import javax.persistence.EntityTransaction

class TransactionHolder {

    private static Logger LOG = LoggerFactory.getLogger(TransactionHolder)

    EntityManager em
    int resumeLevel
    boolean isRollback

    public TransactionHolder(EntityManager em) {
        this.em = em
        this.resumeLevel = 0
        this.isRollback = false
    }

    public boolean isInTransaction() {
        return resumeLevel > 0
    }

    public EntityTransaction getTransaction() {
        assert em != null
        em.transaction
    }

    public void debug(String message, Exception ex = null) {
        if (LOG.isDebugEnabled()) {
            StackTraceElement[] traces = Thread.currentThread().getStackTrace()
            StackTraceElement caller
            for (StackTraceElement e: traces) {
                if (StackTraceUtils.isApplicationClass(e.className)) {
                    if (e.className.startsWith('simplejpa') || e.className.startsWith('java_lang_Thread') ||
                        e.className.startsWith('org.codehaus.griffon')) {
                            continue
                    }
                    caller = e
                    break
                }
            }
            LOG.debug("${caller?:traces[0]}: $message", ex)
        }
    }

    public boolean beginTransaction(boolean propagate = true) {
        if (resumeLevel > 0) {
            if (!propagate) {
                debug "Commiting transaction."
                em.transaction.commit()
                debug "Starting new transaction."
                em.transaction.begin()
                resumeLevel = 1
                return true
            } else {
                resumeLevel++
                debug "Resuming from previous transaction, now in tr [$resumeLevel]."
                return false
            }
        } else if (resumeLevel==0) {
            debug "Start a new transaction..."
            if (!em.transaction.active) {
                em.transaction.begin()
            }
            resumeLevel = 1
            debug "Now in tr [$resumeLevel]."
            return true
        }

    }

    public boolean commitTransaction() {
        debug "Trying to ${isRollback?'rollback':'commit'} from tr [$resumeLevel]"
        if (resumeLevel>0) {
            boolean commit = false
            if (resumeLevel==1) {
                if (isRollback) {
                    rollbackTransaction()
                    return false
                }
                debug "Commiting transaction..."
                try {
                    em.transaction.commit()
                    em.close()
                    commit = true
                } catch (Exception ex) {
                    LOG.error "Exception while committing!", ex
                    throw ex
                }
                finally {
                    debug "After committing, tr [$resumeLevel]."
                    resumeLevel--
                }
            } else {
                debug "Not committing yet [$resumeLevel]."
                resumeLevel--
            }
            debug "Now in tr  [${resumeLevel>0?resumeLevel:'no transaction'}]."
            return commit
        } else if (resumeLevel==0) {
            debug "Can't commit: Not inside a transaction. This is normal if transaction was rollbacked due to exception."
            return false
        }
    }

    public boolean rollbackTransaction() {
        if (resumeLevel==0) {
            debug "Can't rollback: Not inside a transaction."
            return false
        } else if (resumeLevel==1) {
            debug "Rollback transaction..."
            try {
                em.transaction.rollback()
                em.close()
            } finally {
                resumeLevel = 0
                debug "Now in [no transaction]."
                isRollback = false
                return true
            }
        } else if (resumeLevel > 1) {
            debug "No rollback yet [$resumeLevel]"
            isRollback = true
            resumeLevel--
            return false
        }
    }

    @Override
    public java.lang.String toString() {
        "TransactionHolder[em=$em, resumeLevel=$resumeLevel]"
    }
}
