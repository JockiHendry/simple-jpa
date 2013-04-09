package simplejpa.transaction

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.persistence.EntityManager
import javax.persistence.EntityTransaction

class TransactionHolder {

    EntityManager em
    private int resumeLevel
    private boolean isRollback
    private static Logger LOG = LoggerFactory.getLogger(TransactionHolder)

    public TransactionHolder() {
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

    public boolean beginTransaction() {
        assert em != null
        if (resumeLevel > 0) {
            resumeLevel++
            LOG.info "Resuming from previous transaction, now in tr [$resumeLevel]."
            return false
        } else if (resumeLevel==0) {
            LOG.info "Start a new transaction..."
            em.transaction.begin()
            em.clear()
            resumeLevel++
            LOG.info "Now in tr [$resumeLevel]."
            return true
        }

    }

    public void commitTransaction() {
        assert em != null
        LOG.info "Trying to ${isRollback?'rollback':'commit'} from tr [$resumeLevel] from thread ${Thread.currentThread().id}"
        if (resumeLevel>0) {
            if (resumeLevel==1) {
                if (isRollback) {
                    rollbackTransaction()
                    return
                }
                LOG.info "Commiting transaction..."
                em.transaction.commit()
            } else {
                LOG.info "Not committing yet [$resumeLevel]."
            }
            resumeLevel--
            LOG.info "Now in tr  [${resumeLevel>0?resumeLevel:'no transaction'}]."
        } else if (resumeLevel==0) {
            LOG.info "Can't commit: Not inside a transaction."
        }
    }

    public void rollbackTransaction() {
        assert em != null
        if (resumeLevel==0) {
            LOG.info "Can't rollback: Not inside a transaction."
        } else if (resumeLevel==1) {
            LOG.info "Rollback transaction..."
            em.transaction.rollback()
            resumeLevel = 0
            LOG.info "Now in [no transaction]."
            isRollback = false
        } else if (resumeLevel > 1) {
            LOG.info "No rollback yet [$resumeLevel]"
            isRollback = true
            resumeLevel--
        }
    }

}
