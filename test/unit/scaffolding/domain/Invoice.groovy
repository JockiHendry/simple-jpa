package scaffolding.domain

import org.joda.time.LocalDate
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.OneToOne

@Entity(name="TEST")
class Invoice {

    LocalDate date

    String invoiceNumber

    @ElementCollection(fetch=FetchType.EAGER)
    List<LineItem> items = []

    BigDecimal discount

    @Enumerated
    Status status

    @OneToOne
    Delivery delivery

}






