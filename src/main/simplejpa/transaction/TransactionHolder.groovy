package simplejpa.transaction

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.persistence.EntityTransaction
import java.util.concurrent.ArrayBlockingQueue

class TransactionHolder {

    private EntityTransaction transaction
    private int resumeLevel
    private boolean isRollback
    private static Logger LOG = LoggerFactory.getLogger(TransactionHolder)

    public TransactionHolder(EntityTransaction transaction) {
        this.transaction = transaction
        this.resumeLevel = 0
        this.isRollback = false
    }

    public boolean isInTransaction() {
        return resumeLevel > 0
    }

    public EntityTransaction getTransaction() {
        transaction
    }

    public boolean beginTransaction() {
        if (resumeLevel > 0) {
            resumeLevel++
            LOG.info "Resuming from previous transaction, now in transaction [$resumeLevel]."
            return false
        } else if (resumeLevel==0) {
            LOG.info "Start a new transaction..."
            transaction.begin()
            resumeLevel++
            LOG.info "Now in transaction [$resumeLevel]."
            return true
        }


    }

    public void commitTransaction() {
        LOG.info "Now in transaction [$resumeLevel]."
        if (resumeLevel>0) {
            if (resumeLevel==1) {
                if (isRollback) {
                    rollbackTransaction()
                    return
                }
                LOG.info "Commiting transaction..."
                transaction.commit()
            } else {
                LOG.info "Not committing yet [$resumeLevel]."
            }
            resumeLevel--
            LOG.info "Now in transaction [${resumeLevel>0?resumeLevel:'no transaction'}]."
        } else if (resumeLevel==0) {
            LOG.info "Can't commit: Not inside a transaction."
        }
    }

    public void rollbackTransaction() {
        if (resumeLevel==0) {
            LOG.info "Can't rollback: Not inside a transaction."
        } else if (resumeLevel==1) {
            LOG.info "Rollback transaction..."
            transaction.rollback()
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
