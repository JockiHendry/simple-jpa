package scaffolding.domain

import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.OneToMany

@Entity
class Delivery {

    DateTime date

    @OneToMany
    List<Shipment> shipments

}
