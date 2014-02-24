package simplejpa.transaction

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

    public TransactionHolder(EntityManager em, TransactionHolder another) {
        this.em = em
        this.resumeLevel = another.resumeLevel
        this.isRollback = another.isRollback
    }

    public boolean isInTransaction() {
        return resumeLevel > 0
    }

    public EntityTransaction getTransaction() {
        assert em != null
        em.transaction
    }

    public boolean beginTransaction(boolean resume = true) {
        assert em != null
        if (resumeLevel > 0) {
            if (!resume) {
                LOG.debug "Commiting transaction."
                em.transaction.commit()
                LOG.debug "Starting new transaction."
                em.transaction.begin()
                resumeLevel = 1
                return true
            } else {
                resumeLevel++
                LOG.debug "Resuming from previous transaction, now in tr [$resumeLevel]."
                return false
            }
        } else if (resumeLevel==0) {
            LOG.debug "Start a new transaction..."
            if (!em.transaction.active) {
                em.transaction.begin()
            }
            resumeLevel = 1
            LOG.debug "Now in tr [$resumeLevel]."
            return true
        }

    }

    public boolean commitTransaction() {
        assert em != null
        LOG.debug "Trying to ${isRollback?'rollback':'commit'} from tr [$resumeLevel] from thread ${Thread.currentThread().id}"
        if (resumeLevel>0) {
            boolean commit = false
            if (resumeLevel==1) {
                if (isRollback) {
                    rollbackTransaction()
                    return false
                }
                LOG.debug "Commiting transaction..."
                em.transaction.commit()
                commit = true
            } else {
                LOG.debug "Not committing yet [$resumeLevel]."
            }
            resumeLevel--
            LOG.debug "Now in tr  [${resumeLevel>0?resumeLevel:'no transaction'}]."
            return commit
        } else if (resumeLevel==0) {
            LOG.debug "Can't commit: Not inside a transaction. This is normal if transaction was rollbacked due to exception."
            return false
        }
    }

    public boolean rollbackTransaction() {
        assert em != null
        if (resumeLevel==0) {
            LOG.debug "Can't rollback: Not inside a transaction."
            return false
        } else if (resumeLevel==1) {
            LOG.debug "Rollback transaction..."
            em.transaction.rollback()
            resumeLevel = 0
            LOG.debug "Now in [no transaction]."
            isRollback = false
            return true
        } else if (resumeLevel > 1) {
            LOG.debug "No rollback yet [$resumeLevel]"
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
