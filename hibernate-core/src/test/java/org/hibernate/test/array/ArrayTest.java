//$Id: ArrayTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.array;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Emmanuel Bernard
 */
public class ArrayTest extends FunctionalTestCase {

	public ArrayTest(String x) {
		super( x );
	}

	public String[] getMappings() {
		return new String[] { "array/A.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ArrayTest.class );
	}

	public void testArrayJoinFetch() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		A a = new A();
		B b = new B();
		a.setBs( new B[] {b} );
		s.persist( a );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		a = (A) s.get( A.class, a.getId() );
		assertNotNull( a );
		assertNotNull( a.getBs() );
		assertEquals( a.getBs().length, 1 );
		assertNotNull( a.getBs()[0] );
		
		s.delete(a);
		s.delete(a.getBs()[0]);
		tx.commit();
		s.close();
	}
}
