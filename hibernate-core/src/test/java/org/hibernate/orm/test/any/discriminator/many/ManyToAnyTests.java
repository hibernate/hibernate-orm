/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.discriminator.many;

import org.hibernate.orm.test.any.discriminator.CardPayment;
import org.hibernate.orm.test.any.discriminator.CashPayment;
import org.hibernate.orm.test.any.discriminator.CheckPayment;
import org.hibernate.orm.test.any.discriminator.Payment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, CheckPayment.class, Loan.class})
@SessionFactory
public class ManyToAnyTests {
	@Test
	public void testManyToAnyUsage(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Loan loan = session.find( Loan.class, 1 );
			final CashPayment cashPayment = session.find( CashPayment.class, 1 );
			final CardPayment cardPayment = session.find( CardPayment.class, 1 );
			final CheckPayment checkPayment = session.find( CheckPayment.class, 1 );

			loan.getPayments().add( cardPayment );
			session.flush();

			loan.getPayments().add( checkPayment );
			session.flush();

			loan.getPayments().add( cashPayment );
			session.flush();
		} );
	}

	@BeforeEach
	void prepareTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Loan loan = new Loan( 1, "1" );
			final CashPayment cashPayment = new CashPayment( 1, 50.00 );
			final CardPayment cardPayment = new CardPayment( 1, 150.00, "123-456-789" );
			final CheckPayment checkPayment = new CheckPayment( 1, 250.00, 1001, "123", "987" );
			session.persist( loan );
			session.persist( cashPayment );
			session.persist( cardPayment );
			session.persist( checkPayment );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
