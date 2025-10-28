/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.cid.idclass;

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
@DomainModel( annotatedClasses = {
		Customer.class,
		Order.class,
		Payment.class
})
@SessionFactory
public class NestedIdClassTests {
	@Test
	public void smokeTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Payment payment = session.get( Payment.class, new PaymentId( new OrderId( 1, 1 ), "123" ) );
			assertThat( payment ).isNotNull();
			assertThat( payment.accountNumber ).isNotNull();
			assertThat( payment.order ).isNotNull();

			assertThat( payment.order.orderNumber ).isNotNull();
			assertThat( payment.order.customer ).isNotNull();

			assertThat( payment.order.customer.id ).isNotNull();
			assertThat( payment.order.customer.name ).isNotNull();
		});
	}

	@Test
	public void testHqlIdAttributeReference(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from Payment p where p.order.orderNumber = 123" ).list();
			session.createQuery( "from Payment p where p.id.order.orderNumber = 123" ).list();
		});
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

			final Payment payment = new Payment( acmeOrder1, "123" );
			session.persist( payment );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
