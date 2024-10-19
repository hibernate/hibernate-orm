/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.mixed;

import org.hibernate.HibernateException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link org.hibernate.type.AnyDiscriminatorValueStrategy}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class AnyDiscriminatorValueHandlingTests {
	@Test
	@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, CheckPayment.class, Order.class})
	@SessionFactory
	void verifyImplicitMappingHandling(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Order order = new Order( 1, "1" );
			final CashPayment cashPayment = new CashPayment( 1, 50.00 );
			final CardPayment cardPayment = new CardPayment( 1, 150.00, "123-456-789" );
			final CheckPayment checkPayment = new CheckPayment( 1, 250.00, 1001, "123", "987" );
			session.persist( order );
			session.persist( cashPayment );
			session.persist( cardPayment );
			session.persist( checkPayment );
			session.flush();

			order.implicitPayment = cardPayment;
			session.flush();

			order.implicitPayment = checkPayment;
			session.flush();

			order.implicitPayment = cashPayment;
			session.flush();
		} );
	}

	@Test
	@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, CheckPayment.class, Order.class})
	@SessionFactory
	void verifyExplicitMappingHandling(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Order order = new Order( 1, "1" );
			final CashPayment cashPayment = new CashPayment( 1, 50.00 );
			final CardPayment cardPayment = new CardPayment( 1, 150.00, "123-456-789" );
			final CheckPayment checkPayment = new CheckPayment( 1, 250.00, 1001, "123", "987" );
			session.persist( order );
			session.persist( cashPayment );
			session.persist( cardPayment );
			session.persist( checkPayment );
			session.flush();

			order.explicitPayment = cardPayment;
			session.flush();

			order.explicitPayment = checkPayment;
			session.flush();

			// NOTE : cash is not explicitly mapped
			try {
				order.explicitPayment = cashPayment;
				session.flush();
				fail( "Expecting an error" );
			}
			catch (HibernateException expected) {
				assertThat( expected ).hasMessageContaining( "Entity not explicitly mapped for ANY discriminator" );
			}
		} );
	}

	@Test
	@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, CheckPayment.class, Order.class})
	@SessionFactory
	void verifyMixedMappingHandling(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Order order = new Order( 1, "1" );
			final CashPayment cashPayment = new CashPayment( 1, 50.00 );
			final CardPayment cardPayment = new CardPayment( 1, 150.00, "123-456-789" );
			final CheckPayment checkPayment = new CheckPayment( 1, 250.00, 1001, "123", "987" );
			session.persist( order );
			session.persist( cashPayment );
			session.persist( cardPayment );
			session.persist( checkPayment );
			session.flush();

			order.mixedPayment = cardPayment;
			session.flush();

			order.mixedPayment = checkPayment;
			session.flush();

			order.mixedPayment = cashPayment;
			session.flush();
		} );
	}

	@AfterEach
	@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, CheckPayment.class, Order.class})
	@SessionFactory
	void dropTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.createMutationQuery( "delete Order" ).executeUpdate();
			session.createMutationQuery( "delete CashPayment" ).executeUpdate();
			session.createMutationQuery( "delete CardPayment" ).executeUpdate();
			session.createMutationQuery( "delete CheckPayment" ).executeUpdate();
		} );

	}
}
