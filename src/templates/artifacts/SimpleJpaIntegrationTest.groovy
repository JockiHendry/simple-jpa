package ${packageName}

import domain.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.testing.HibernateTestCase

class $className extends HibernateTestCase {

    private static final Logger log = LoggerFactory.getLogger($className)

    protected void setUp() {
        super.setUp()
        setUpDatabase("${domainClassAsProp}", "/${packageName.replace('.','/')}/data.xls")
    }

    protected void tearDown() {
        super.tearDown()
    }

}