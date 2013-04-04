package simplejpa

import org.joda.time.DateTime

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
}
