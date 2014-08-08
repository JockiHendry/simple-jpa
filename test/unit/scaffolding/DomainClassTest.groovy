package scaffolding

import org.junit.Test
import simplejpa.scaffolding.Scaffolding
import simplejpa.scaffolding.attribute.Attribute
import simplejpa.scaffolding.attribute.BasicAttribute
import simplejpa.scaffolding.attribute.CollectionAttribute
import simplejpa.scaffolding.attribute.DateAttribute
import simplejpa.scaffolding.DomainClass
import simplejpa.scaffolding.attribute.EntityAttribute
import simplejpa.scaffolding.attribute.EnumeratedAttribute
import simplejpa.scaffolding.generator.basic.BasicGenerator

import static org.junit.Assert.*

class DomainClassTest {

    List<String> files = [
        "${BuildSettingsHolder.settings.baseDir}/test/unit/scaffolding/domain/Delivery.groovy",
        "${BuildSettingsHolder.settings.baseDir}/test/unit/scaffolding/domain/Invoice.groovy",
        "${BuildSettingsHolder.settings.baseDir}/test/unit/scaffolding/domain/LineItem.groovy",
        "${BuildSettingsHolder.settings.baseDir}/test/unit/scaffolding/domain/Product.groovy",
        "${BuildSettingsHolder.settings.baseDir}/test/unit/scaffolding/domain/purchase/PurchaseInvoice.groovy",
        "${BuildSettingsHolder.settings.baseDir}/test/unit/scaffolding/domain/Shipment.groovy",
        "${BuildSettingsHolder.settings.baseDir}/test/unit/scaffolding/domain/Status.groovy",
        "${BuildSettingsHolder.settings.baseDir}/test/unit/scaffolding/domain/Supplier.groovy",
    ];

    @Test
    public void testInvoice() {
        Scaffolding scaffolding = new Scaffolding()
        scaffolding.domainPackageName = "scaffolding.domain"
        scaffolding.populateDomainClasses(files)
        DomainClass domainClass = scaffolding.getDomainClasses().'Invoice'

        assertEquals("Invoice", domainClass.name)
        assertTrue(domainClass.entity)
        assertEquals("scaffolding.domain", domainClass.packageName)

        List<Attribute> fields = domainClass.attributes
        assertEquals(6, fields.size())

        assertEquals(DateAttribute, fields[0].class)
        assertEquals('LocalDate', fields[0].type)
        assertEquals('date', fields[0].name)

        assertEquals(BasicAttribute, fields[1].class)
        assertEquals('String', fields[1].type)
        assertEquals('invoiceNumber', fields[1].name)

        assertEquals(CollectionAttribute, fields[2].class)
        assertEquals('List', fields[2].type)
        assertEquals('items', fields[2].name)
        assertEquals(false, fields[2].bidirectional)
        assertEquals('LineItem', fields[2].targetType)
        assertNotNull(fields[2].target)
        assertEquals('LineItem', fields[2].target.name)
        assertEquals(3, fields[2].target.attributes.size())
        assertEquals('produk', fields[2].target.attributes[0].name)
        assertNotNull(fields[2].target.attributes[0].target)
        assertEquals(1, fields[2].target.attributes[0].target.attributes.size())
        assertEquals('name', fields[2].target.attributes[0].target.attributes[0].name)
        assertEquals('quantity', fields[2].target.attributes[1].name)
        assertEquals('price', fields[2].target.attributes[2].name)

        assertEquals(BasicAttribute, fields[3].class)
        assertEquals('BigDecimal', fields[3].type)
        assertEquals('discount', fields[3].name)

        assertEquals(EnumeratedAttribute, fields[4].class)
        assertEquals('Status', fields[4].type)
        assertEquals('status', fields[4].name)

        assertEquals(EntityAttribute, fields[5].class)
        assertEquals('Delivery', fields[5].type)
        assertEquals('delivery', fields[5].name)
        assertNotNull(fields[5].target)
        assertEquals('Delivery', fields[5].target.name)
        assertEquals(2, fields[5].target.attributes.size())
        assertEquals('date', fields[5].target.attributes[0].name)
        assertEquals('shipments', fields[5].target.attributes[1].name)
        assertNotNull(fields[5].target.attributes[1].target)
        assertEquals('Shipment', fields[5].target.attributes[1].target.name)
    }

    @Test
    public void testPurchaseInvoice() {
        Scaffolding scaffolding = new Scaffolding()
        scaffolding.domainPackageName = "scaffolding.output"
        scaffolding.populateDomainClasses(files)
        DomainClass domainClass = scaffolding.getDomainClasses().'PurchaseInvoice'

        assertEquals("PurchaseInvoice", domainClass.name)
        assertTrue(domainClass.entity)

        List<Attribute> fields = domainClass.attributes
        assertEquals(7, fields.size())

        assertEquals(DateAttribute, fields[0].class)
        assertEquals('LocalDate', fields[0].type)
        assertEquals('date', fields[0].name)

        assertEquals(BasicAttribute, fields[1].class)
        assertEquals('String', fields[1].type)
        assertEquals('invoiceNumber', fields[1].name)

        assertEquals(CollectionAttribute, fields[2].class)
        assertEquals('List', fields[2].type)
        assertEquals('items', fields[2].name)
        assertEquals(false, fields[2].bidirectional)
        assertEquals('LineItem', fields[2].targetType)
        assertNotNull(fields[2].target)
        assertEquals('LineItem', fields[2].target.name)
        assertEquals(3, fields[2].target.attributes.size())
        assertEquals('produk', fields[2].target.attributes[0].name)
        assertNotNull(fields[2].target.attributes[0].target)
        assertEquals(1, fields[2].target.attributes[0].target.attributes.size())
        assertEquals('name', fields[2].target.attributes[0].target.attributes[0].name)
        assertEquals('quantity', fields[2].target.attributes[1].name)
        assertEquals('price', fields[2].target.attributes[2].name)

        assertEquals(BasicAttribute, fields[3].class)
        assertEquals('BigDecimal', fields[3].type)
        assertEquals('discount', fields[3].name)

        assertEquals(EnumeratedAttribute, fields[4].class)
        assertEquals('Status', fields[4].type)
        assertEquals('status', fields[4].name)

        assertEquals(EntityAttribute, fields[5].class)
        assertEquals('Delivery', fields[5].type)
        assertEquals('delivery', fields[5].name)
        assertNotNull(fields[5].target)
        assertEquals('Delivery', fields[5].target.name)
        assertEquals(2, fields[5].target.attributes.size())
        assertEquals('date', fields[5].target.attributes[0].name)
        assertEquals('shipments', fields[5].target.attributes[1].name)
        assertNotNull(fields[5].target.attributes[1].target)
        assertEquals('Shipment', fields[5].target.attributes[1].target.name)

        assertEquals(EntityAttribute, fields[6].class)
        assertEquals('Supplier', fields[6].type)
        assertEquals('supplier', fields[6].name)
        assertNotNull(fields[6].target)
        assertEquals('Supplier', fields[6].target.name)
    }

}
