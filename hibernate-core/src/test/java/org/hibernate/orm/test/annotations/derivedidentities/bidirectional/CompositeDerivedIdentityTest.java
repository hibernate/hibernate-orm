/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.bidirectional;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				Product.class,
				OrderLine.class,
				Order.class
		}
)
@SessionFactory
public class CompositeDerivedIdentityTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCreateProject(SessionFactoryScope scope) {
		Product product = new Product();
		product.setName( "Product 1" );

		scope.inTransaction(
				session -> {
					session.persist( product );
				}
		);

		Order order = new Order();
		order.setName( "Order 1" );
		order.addLineItem( product, 2 );

		scope.inTransaction(
				session -> {
					session.persist( order );
				}
		);

		Long orderId = order.getId();

		scope.inTransaction(
				session -> {
					Order o = session.get( Order.class, orderId );
					assertEquals( 1, o.getLineItems().size() );
					session.remove( o );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10476")
	public void testBidirectonalKeyManyToOneId(SessionFactoryScope scope) {
		Product product = new Product();
		product.setName( "Product 1" );

		scope.inTransaction(
				session ->
						session.persist( product )
		);

		Order order = new Order();
		order.setName( "Order 1" );
		order.addLineItem( product, 2 );

		scope.inTransaction(
				session ->
						session.persist( order )
		);

		scope.inTransaction(
				session -> {
					OrderLine orderLine = order.getLineItems().iterator().next();
					orderLine.setAmount( 5 );
					OrderLine orderLineGotten = session.get( OrderLine.class, orderLine );
					assertSame( orderLineGotten, orderLine );
					assertEquals( Integer.valueOf( 2 ), orderLineGotten.getAmount() );
					assertTrue( session.getPersistenceContext().isEntryFor( orderLineGotten ) );
					assertFalse( session.getPersistenceContext().isEntryFor( orderLineGotten.getOrder() ) );
					assertFalse( session.getPersistenceContext().isEntryFor( orderLineGotten.getProduct() ) );
				}
		);
	}
}
