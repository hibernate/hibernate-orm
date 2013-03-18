package org.hibernate.test.hqlfetchscroll;

import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.testing.RequiresDialect;

@RequiresDialect( value = { Oracle8iDialect.class },
		comment = "Oracle does not support the identity column used in the HQLScrollFetchTest mapping." )
public class NoIdentityHQLScrollFetchTest extends HQLScrollFetchTest {

	@Override
	public String[] getMappings() {
		return new String[] { "hqlfetchscroll/NoIdentityParentChild.hbm.xml" };
	}
}
