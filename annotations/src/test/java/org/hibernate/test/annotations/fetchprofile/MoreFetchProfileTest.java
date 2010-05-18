package org.hibernate.test.annotations.fetchprofile;

import java.util.Date;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.FailureExpected;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class MoreFetchProfileTest extends TestCase{

	@FailureExpected( jiraKey = "HHH-5233")
	public void testFetchWithTwoOverrides() throws Exception {
		Session s = openSession(  );
		s.enableFetchProfile( "customer-with-orders-and-country" );
		final Transaction transaction = s.beginTransaction();
		Country ctry = new Country();
		ctry.setName( "France" );
		Order o = new Order();
		o.setCountry( ctry );
		o.setDeliveryDate( new Date() );
		o.setOrderNumber( 1 );
		Order o2 = new Order();
		o2.setCountry( ctry );
		o2.setDeliveryDate( new Date() );
		o2.setOrderNumber( 2 );
		Customer c = new Customer();
		c.setCustomerNumber( 1 );
		c.setName( "Emmanuel" );
		c.getOrders().add( o );
		c.setLastOrder( o2 );

		s.persist( ctry );
		s.persist( o );
		s.persist( o2 );
		s.persist( c );

		s.flush();

		s.clear();

		c = (Customer) s.get( Customer.class, c.getId() );
		assertTrue( Hibernate.isInitialized( c.getLastOrder() ) );
		assertTrue( Hibernate.isInitialized( c.getOrders() ) );
		for(Order so : c.getOrders() ) {
			assertTrue( Hibernate.isInitialized( so.getCountry() ) );
		}
		final Order order = c.getOrders().iterator().next();
		c.getOrders().remove( order );
		s.delete( c );
		final Order lastOrder = c.getLastOrder();
		c.setLastOrder( null );
		s.delete( order.getCountry() );
		s.delete( lastOrder );
		s.delete( order );

		transaction.commit();
		s.close();

	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Order.class,
				Country.class,
				Customer.class,
				SupportTickets.class
		};
	}
}
