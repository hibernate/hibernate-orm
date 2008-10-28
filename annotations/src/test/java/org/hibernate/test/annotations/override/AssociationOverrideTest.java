//$Id$
package org.hibernate.test.annotations.override;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class AssociationOverrideTest extends TestCase {

	public void testOverriding() throws Exception {
		Location paris = new Location();
		paris.setName( "Paris" );
		Location atlanta = new Location();
		atlanta.setName( "Atlanta" );
		Trip trip = new Trip();
		trip.setFrom( paris );
		//trip.setTo( atlanta );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( paris );
		s.persist( atlanta );
		try {
			s.persist( trip );
			s.flush();
			fail( "Should be non nullable" );
		}
		catch (HibernateException e) {
			//success
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	protected Class[] getMappings() {
		return new Class[]{
				Location.class,
				Move.class,
				Trip.class
		};
	}
}
