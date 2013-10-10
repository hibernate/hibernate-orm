package org.hibernate.test.hqlfetchscroll;

import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.testing.RequiresDialect;

@RequiresDialect( value = { Oracle8iDialect.class, AbstractHANADialect.class },
		comment = "Oracle/HANA do not support the identity column used in the HQLScrollFetchTest mapping." )
public class NoIdentityHQLScrollFetchTest extends HQLScrollFetchTest {

	@Override
	public String[] getMappings() {
		return new String[] { "hqlfetchscroll/NoIdentityParentChild.hbm.xml" };
	}
}
