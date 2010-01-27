package org.hibernate.ejb.test.mapping;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class QuotingTest extends TestCase {

	public void testQuote() {
		// the configuration was failing
	}

	@Override
	public String[] getEjb3DD() {
		return new String[] {
				"org/hibernate/ejb/test/mapping/orm.xml"
		};
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Phone.class
		};
	}
}
