//$Id$
package org.hibernate.test.annotations.entity;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class PropertyDefaultMappingsTest extends TestCase {
	public PropertyDefaultMappingsTest(String x) {
		super( x );
	}

	public void testSerializableObject() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Country c = new Country();
		c.setName( "France" );
		Address a = new Address();
		a.setCity( "Paris" );
		a.setCountry( c );
		s.persist( a );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Address reloadedAddress = (Address) s.get( Address.class, a.getId() );
		assertNotNull( reloadedAddress );
		assertNotNull( reloadedAddress.getCountry() );
		assertEquals( a.getCountry().getName(), reloadedAddress.getCountry().getName() );
		tx.rollback();
		s.close();
	}

	public void testTransientField() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		WashingMachine wm = new WashingMachine();
		wm.setActive( true );
		s.persist( wm );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		wm = (WashingMachine) s.get( WashingMachine.class, wm.getId() );
		assertFalse( "transient should not be persistent", wm.isActive() );
		s.delete( wm );
		tx.commit();
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Address.class,
				WashingMachine.class
		};
	}
}
