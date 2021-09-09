/**
 * 
 */
package org.hibernate.orm.test.mapping.converted.enums;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author gtoison
 *
 */
public class NamedEnumUserTypeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "";
	}
	
	@Override
	protected String[] getMappings() {
		return new String[] { "org/hibernate/orm/test/mapping/converted/enums/PersonNamedEnumsUserType.xml" };
	}

	@Override
	protected void prepareTest() {
		doInHibernate( this::sessionFactory, s -> {
			s.persist( Person.person( Gender.MALE, HairColor.BLACK ) );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
	
	@Ignore
	@Test
	@TestForIssue(jiraKey = "HHH-14820")
	public void testNamedEnumUserType() {
		// This fails because the same instance of NamedEnumUserType is used for both enums
		doInHibernate( this::sessionFactory, s -> {
			s.createQuery( "from Person p", Person.class );
		} );
	}
}
