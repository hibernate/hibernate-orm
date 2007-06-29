package org.hibernate.test.keymanytoone.bidir.component;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.event.def.DefaultLoadEventListener;
import org.hibernate.event.LoadEvent;
import org.hibernate.event.LoadEventListener;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.TestCase;

/**
 * @author Steve Ebersole
 */
public class EagerKeyManyToOneTest extends TestCase {

	public EagerKeyManyToOneTest(String name) {
		super( name );
	}

	protected String[] getMappings() {
		return new String[] { "keymanytoone/bidir/component/EagerMapping.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( EagerKeyManyToOneTest.class );
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		LoadEventListener[] baseListeners = cfg.getEventListeners().getLoadEventListeners();
		int baseLength = baseListeners.length;
		LoadEventListener[] expandedListeners = new LoadEventListener[ baseLength + 1 ];
		expandedListeners[ 0 ] = new CustomLoadListener();
		System.arraycopy( baseListeners, 0, expandedListeners, 1, baseLength );
		cfg.getEventListeners().setLoadEventListeners( expandedListeners );
	}

	public void testSaveCascadedToKeyManyToOne() {
		// test cascading a save to an association with a key-many-to-one which refers to a
		// just saved entity
		Session s = openSession();
		s.beginTransaction();
		Customer cust = new Customer( "Acme, Inc." );
		Order order = new Order( new Order.Id( cust, 1 ) );
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
		Order order = new Order( new Order.Id( cust, 1 ) );
		cust.getOrders().add( order );
		s.save( cust );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

// Here is an example of HHH-2277
// essentially we have a bidirectional association where one side of the
// association is actually part of a composite PK
//
// See #testLoadEntityWithEagerFetchingToKeyManyToOneReferenceBackToSelfFailureExpected() below...
//
// The way these are mapped causes the problem because both sides
// are defined as eager which leads to the infinite loop; if only
// one side is marked as eager, then all is ok...
//		cust = ( Customer ) s.get( Customer.class, cust.getId() );
//		assertEquals( 1, cust.getOrders().size() );
//		s.clear();

		cust = ( Customer ) s.createQuery( "from Customer" ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		cust = ( Customer ) s.createQuery( "from Customer c join fetch c.orders" ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		cust = ( Customer ) s.createQuery( "from Customer c join fetch c.orders as o join fetch o.id.customer" ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		cust = ( Customer ) s.createCriteria( Customer.class ).uniqueResult();
		assertEquals( 1, cust.getOrders().size() );
		s.clear();

		s.delete( cust );
		s.getTransaction().commit();
		s.close();
	}

	public void testLoadEntityWithEagerFetchingToKeyManyToOneReferenceBackToSelfFailureExpected() {
		// long winded method name to say that this is a test specifically for HHH-2277 ;)
		// essentially we have a bidirectional association where one side of the
		// association is actually part of a composite PK.
		//
		// The way these are mapped causes the problem because both sides
		// are defined as eager which leads to the infinite loop; if only
		// one side is marked as eager, then all is ok.  In other words the
		// problem arises when both pieces of instance data are coming from
		// the same result set.  This is because no "entry" can be placed
		// into the persistence context for the association with the
		// composite key because we are in the process of trying to build
		// the composite-id instance
		Session s = openSession();
		s.beginTransaction();
		Customer cust = new Customer( "Acme, Inc." );
		Order order = new Order( new Order.Id( cust, 1 ) );
		cust.getOrders().add( order );
		s.save( cust );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		try {
			cust = ( Customer ) s.get( Customer.class, cust.getId() );
		}
		catch( OverflowCondition overflow ) {
			fail( "get()/load() caused overflow condition" );
		}
		s.delete( cust );
		s.getTransaction().commit();
		s.close();
	}

	private static class OverflowCondition extends RuntimeException {
	}

	private static class CustomLoadListener extends DefaultLoadEventListener {
		private int internalLoadCount = 0;
		public void onLoad(LoadEvent event, LoadType loadType) throws HibernateException {
			if ( LoadEventListener.INTERNAL_LOAD_EAGER.getName().equals( loadType.getName() ) ) {
				internalLoadCount++;
				if ( internalLoadCount > 10 ) {
					throw new OverflowCondition();
				}
			}
			super.onLoad( event, loadType );
			internalLoadCount--;
		}
	}
}
