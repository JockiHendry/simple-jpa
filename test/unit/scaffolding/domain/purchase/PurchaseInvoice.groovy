package scaffolding.domain.purchase

import scaffolding.domain.Invoice
import scaffolding.domain.Supplier

import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class PurchaseInvoice extends Invoice {

    @ManyToOne
    Supplier supplier

}
