package org.hibernate.test.sql.hand.identity;

import junit.framework.Test;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.dialect.Dialect;
import org.hibernate.Session;
import org.hibernate.JDBCException;
import org.hibernate.test.sql.hand.Organization;

/**
 * Custom SQL tests for combined usage of custom insert SQL and identity columns
 *
 * @author Gail Badner
 */
public class CustomInsertSQLWithIdentityColumnTest extends DatabaseSpecificFunctionalTestCase {

	public CustomInsertSQLWithIdentityColumnTest(String str) {
		super( str );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CustomInsertSQLWithIdentityColumnTest.class );
	}

	public String[] getMappings() {
		return new String[] {"sql/hand/identity/Mappings.hbm.xml"};
	}

	public boolean appliesTo(Dialect dialect) {
		return dialect.supportsIdentityColumns();
	}

	public void testBadInsertionFails() {
		Session session = openSession();
		session.beginTransaction();
		Organization org = new Organization( "hola!" );
		try {
			session.save( org );
			session.delete( org );
			fail( "expecting bad custom insert statement to fail" );
		}
		catch( JDBCException e ) {
			// expected failure
		}

		session.getTransaction().rollback();
		session.close();
	}
}
