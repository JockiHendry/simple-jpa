package scaffolding.domain

import javax.persistence.Embeddable
import javax.persistence.ManyToOne

@Embeddable
class LineItem {

    @ManyToOne
    Product produk

    Integer quantity

    BigDecimal price

}
