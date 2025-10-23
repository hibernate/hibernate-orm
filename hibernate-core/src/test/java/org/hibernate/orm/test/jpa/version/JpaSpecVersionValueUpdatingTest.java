/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-7138" )
@DomainModel(annotatedClasses = {Customer.class, Order.class})
@SessionFactory
public class JpaSpecVersionValueUpdatingTest {

	@AfterEach
	public void afterEach(SessionFactoryScope scope) { scope.dropData(); }

	@Test
	public void testVersionNotIncrementedOnModificationOfNonOwningCollectionNonCascaded(SessionFactoryScope scope) {
		final Customer initialCustomer = new Customer();
		initialCustomer.id = 1L;

		scope.inTransaction( session -> session.persist( initialCustomer ) );

		long initialVersion = initialCustomer.version;

		Customer customer = scope.fromTransaction( session -> {
			Customer c = session.find( Customer.class, 1L );
			assertEquals( initialVersion, c.version );
			Order order = new Order();
			order.id = 1L;
			order.customer = c;
			c.orders.add( order );
			session.persist( order );
			return c;
		} );
		assertEquals( initialVersion, customer.version );

		customer = scope.fromTransaction( session -> {
			Customer c = session.find( Customer.class, 1L );
			assertEquals( initialVersion, c.version );
			Order order2 = new Order();
			order2.id = 2L;
			order2.customer = c;
			c.orders.add( order2 );
			session.persist( order2 );
			return c;
		} );
		assertEquals( initialVersion, customer.version );

		scope.inTransaction( session -> {
			Customer c = session.getReference( Customer.class, 1L );
			assertEquals( initialVersion, c.version );
		} );
	}

	@Test
	public void testVersionNotIncrementedOnModificationOfNonOwningCollectionCascaded(SessionFactoryScope scope) {
		Customer initialCustomer = new Customer();
		initialCustomer.id = 1L;

		scope.inTransaction( session -> session.persist( initialCustomer ) );

		long initialVersion = initialCustomer.version;

		Customer customer = scope.fromTransaction( session -> {
				Customer c = session.find( Customer.class, 1L );
				assertEquals( initialVersion, c.version );
				Order order = new Order();
				order.id = 1L;
				order.customer = c;
				c.orders.add( order );
				return c;
			}
		);
		assertEquals( initialVersion, customer.version );

		customer = scope.fromTransaction( session -> {
				Customer c = session.find( Customer.class, 1L );
				Order order2 = new Order();
				order2.id = 2L;
				order2.customer = c;
				c.orders.add( order2 );
				return c;
			}
		);
		assertEquals( initialVersion, customer.version );

		scope.inTransaction( session -> {
			Customer c = session.getReference( Customer.class, 1L );
			assertEquals( initialVersion, c.version );
		} );
	}
}
