/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile;

import java.util.Date;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class MoreFetchProfileTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testFetchWithTwoOverrides() throws Exception {
		Session s = openSession();
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

		c = ( Customer ) s.get( Customer.class, c.getId() );
		assertTrue( Hibernate.isInitialized( c.getLastOrder() ) );
		assertTrue( Hibernate.isInitialized( c.getOrders() ) );
		for ( Order so : c.getOrders() ) {
			assertTrue( Hibernate.isInitialized( so.getCountry() ) );
		}
		final Order order = c.getOrders().iterator().next();
		c.getOrders().remove( order );
		s.remove( c );
		final Order lastOrder = c.getLastOrder();
		c.setLastOrder( null );
		s.remove( order.getCountry() );
		s.remove( lastOrder );
		s.remove( order );

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
