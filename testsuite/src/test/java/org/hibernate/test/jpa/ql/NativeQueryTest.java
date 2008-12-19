package org.hibernate.test.jpa.ql;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.jpa.AbstractJPATest;

/**
 * todo: describe NativeQueryTest
 *
 * @author Steve Ebersole
 */
public class NativeQueryTest extends AbstractJPATest {
	public NativeQueryTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( NativeQueryTest.class );
	}

	public void testJpaStylePositionalParametersInNativeSql() {
		Session s = openSession();
		s.beginTransaction();
		s.createSQLQuery( "select NAME from EJB3_ITEM where ITEM_ID = ?1" ).setParameter( "1", new Long( 123 ) ).list();
		s.getTransaction().commit();
		s.close();
	}
}
