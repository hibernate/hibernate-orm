/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.discriminator.explicit;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
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

import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, CheckPayment.class, Order.class})
@SessionFactory
public class ExplicitValueTests {

	@Test
	void verifyExplicitMappingHandling(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Order order = session.find( Order.class, 1 );
			final CashPayment cashPayment = session.find( CashPayment.class, 1 );
			final CardPayment cardPayment = session.find( CardPayment.class, 1 );
			final CheckPayment checkPayment = session.find( CheckPayment.class, 1 );

			order.paymentExplicit = cardPayment;
			session.flush();
			verifyDiscriminatorValue( "explicit_type", "CARD", session );

			order.paymentExplicit = checkPayment;
			session.flush();
			verifyDiscriminatorValue( "explicit_type", "CHECK", session );

			// NOTE : cash is not explicitly mapped and implicit mappings are not enabled, so this should be an error
			try {
				order.paymentExplicit = cashPayment;
				session.flush();
				fail( "Expecting an error" );
			}
			catch (HibernateException expected) {
				assertThat( expected ).hasMessageContaining( "Cannot determine discriminator value from entity-name" );
			}
		} );
	}

	private void verifyDiscriminatorValue(String columnName, String expectedValue, SessionImplementor session) {
		final String qry = String.format( "select %s from orders", columnName );
		session.doWork( (connection) -> {
			try (final Statement stmnt = connection.createStatement() ) {
				try (ResultSet resultSet = stmnt.executeQuery( qry )) {
					assertThat( resultSet.next() ).isTrue();
					final String discriminatorValue = resultSet.getString( columnName );
					assertThat( resultSet.next() ).isFalse();
					assertThat( discriminatorValue ).isEqualTo( expectedValue );
				}
			}
		} );
	}

	@BeforeEach
	void prepareTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Order order = new Order( 1, "1" );
			final CashPayment cashPayment = new CashPayment( 1, 50.00 );
			final CardPayment cardPayment = new CardPayment( 1, 150.00, "123-456-789" );
			final CheckPayment checkPayment = new CheckPayment( 1, 250.00, 1001, "123", "987" );
			session.persist( order );
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
