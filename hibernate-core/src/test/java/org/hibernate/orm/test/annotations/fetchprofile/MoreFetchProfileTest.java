/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetchprofile;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@DomainModel(
		annotatedClasses = {
				Order.class,
				Country.class,
				Customer.class,
				SupportTickets.class
		}
)
@SessionFactory
public class MoreFetchProfileTest {

	@Test
	public void testFetchWithTwoOverrides(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.enableFetchProfile( "customer-with-orders-and-country" );
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

					session.persist( ctry );
					session.persist( o );
					session.persist( o2 );
					session.persist( c );

					session.flush();

					session.clear();

					c = session.find( Customer.class, c.getId() );
					assertThat( Hibernate.isInitialized( c.getLastOrder() ) ).isTrue();
					assertThat( Hibernate.isInitialized( c.getOrders() ) ).isTrue();
					for ( Order so : c.getOrders() ) {
						assertThat( Hibernate.isInitialized( so.getCountry() ) ).isTrue();
					}
					final Order order = c.getOrders().iterator().next();
					c.getOrders().remove( order );
					session.remove( c );
					final Order lastOrder = c.getLastOrder();
					c.setLastOrder( null );
					session.remove( order.getCountry() );
					session.remove( lastOrder );
					session.remove( order );
				}
		);
	}
}
