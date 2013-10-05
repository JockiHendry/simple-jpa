package simplejpa

import org.joda.time.DateTime

import javax.persistence.PostLoad
import javax.persistence.PrePersist
import javax.persistence.PreUpdate

class AuditingEntityListener {

    @PrePersist
    void createdDate(Object target) {
        target."createdDate" = DateTime.now()
    }

    @PreUpdate
    void modifiedDate(Object target) {
        target."modifiedDate" = DateTime.now()
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
