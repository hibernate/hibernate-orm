package org.hibernate.test.keymanytoone.bidir.embedded;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Steve Ebersole
 */
public class KeyManyToOneTest extends FunctionalTestCase {
	public KeyManyToOneTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( KeyManyToOneTest.class );
	}

	public String[] getMappings() {
		return new String[] { "keymanytoone/bidir/embedded/Mapping.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	public void testSaveCascadedToKeyManyToOne() {
		// test cascading a save to an association with a key-many-to-one which refers to a
		// just saved entity
		Session s = openSession();
		s.beginTransaction();
		Customer cust = new Customer( "Acme, Inc." );
		Order order = new Order( cust, 1 );
		cust.getOrders().add( order );
		s.save( cust );
		s.flush();
		assertEquals( 2, sfi().getStatistics().getEntityInsertCount() );
		s.delete( cust );
		s.getTransaction().commit();
		s.close();
	}

	public void testLoadingStrategies() {
		Session s = openSession();
		s.beginTransaction();
		Customer cust = new Customer( "Acme, Inc." );
		Order order = new Order( cust, 1 );
		cust.getOrders().add( order );
		s.save( cust );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		cust = ( Customer ) s.get( Customer.class, cust.getId() );
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		cust = ( Customer ) s.createQuery( "from Customer" ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		cust = ( Customer ) s.createQuery( "from Customer c join fetch c.orders" ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		s.delete( cust );
		s.getTransaction().commit();
		s.close();
	}
}
