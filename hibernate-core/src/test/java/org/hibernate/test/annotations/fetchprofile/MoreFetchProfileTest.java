/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.test.annotations.fetchprofile;

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
