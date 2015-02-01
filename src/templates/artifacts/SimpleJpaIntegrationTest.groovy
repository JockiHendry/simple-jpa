package ${g.targetPackageName}

import ${g.domainPackageName}.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import simplejpa.testing.DbUnitTestCase

class ${g.domainClassName}Test extends DbUnitTestCase {

	private static final Logger log = LoggerFactory.getLogger(${g.domainClassName}Test)

	protected void setUp() {
		super.setUp()
		setUpDatabase("/${g.targetPackageName.replace('.','/')}/data.xls")
	}

}