//$Id$
package org.hibernate.test.annotations.notfound;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class NotFoundTest extends TestCase {

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

	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Coin.class,
				Currency.class
		};
	}
}
