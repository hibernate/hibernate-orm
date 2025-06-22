/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.initializer;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { Customer.class, Order.class } )
@SessionFactory
public class EmbeddableInitializerTests {
	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Order order = session.get( Order.class, new OrderId( 1, 1 ) );

			assertThat( order ).isNotNull();

			assertThat( order.orderNumber ).isNotNull();
			assertThat( order.customer ).isNotNull();

			assertThat( order.customer.id ).isNotNull();
			assertThat( order.customer.name ).isNotNull();
		});
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from Order o where o.orderNumber = 123" ).list();
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Customer acme = new Customer( 1, "acme" );
			final Customer spacely = new Customer( 2, "spacely" );
			session.persist( acme );
			session.persist( spacely );

			final Order acmeOrder1 = new Order( acme, 1, 123F );
			final Order acmeOrder2 = new Order( acme, 2, 123F );
			session.persist( acmeOrder1 );
			session.persist( acmeOrder2 );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
