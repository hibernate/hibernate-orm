package org.hibernate.ejb.test.mapping;
import org.hibernate.ejb.test.BaseEntityManagerFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class QuotingTest extends BaseEntityManagerFunctionalTestCase {

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
