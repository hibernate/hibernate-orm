/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.notfound;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNull;

/**
 * @author Emmanuel Bernard
 */
public class NotFoundTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testManyToOne() throws Exception {
		Currency euro = new Currency();
		euro.setName( "Euro" );
		Coin fiveC = new Coin();
		fiveC.setName( "Five cents" );
		fiveC.setCurrency( euro );
		Session s = openSession();
		s.getTransaction().begin();
		s.persist( euro );
		s.persist( fiveC );
		s.getTransaction().commit();
		s.clear();
		Transaction tx = s.beginTransaction();
		euro = (Currency) s.get( Currency.class, euro.getId() );
		s.delete( euro );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		fiveC = (Coin) s.get( Coin.class, fiveC.getId() );
		assertNull( fiveC.getCurrency() );
		s.delete( fiveC );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Coin.class, Currency.class };
	}
}
