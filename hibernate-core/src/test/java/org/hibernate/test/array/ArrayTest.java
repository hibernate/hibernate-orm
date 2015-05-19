/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.array;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class ArrayTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "array/A.hbm.xml" };
	}

	@Test
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
		assertEquals( 1, a.getBs().length );
		assertNotNull( a.getBs()[0] );
		
		s.delete(a);
		s.delete(a.getBs()[0]);
		tx.commit();
		s.close();
	}
}
