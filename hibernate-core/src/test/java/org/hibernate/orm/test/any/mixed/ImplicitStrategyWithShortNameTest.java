/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.mixed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.AnyDiscriminatorValueStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ImplicitStrategyWithShortNameTest {
	@Test
	@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, CheckPayment.class, Bill.class})
	@SessionFactory
	void testIt(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Bill bill = new Bill( 1, "Water" );
			session.persist( bill );

			final CashPayment cashPayment = new CashPayment( 1, 123.45 );
			session.persist( cashPayment );

			final CardPayment cardPayment = new CardPayment( 1, 987.50, "12345" );
			session.persist( cardPayment );

			bill.implicitPayment = cashPayment;
			bill.mixedPayment = cardPayment;
		} );

		sessions.inTransaction( (session) -> session.doWork( (conn) -> {
			final Statement statement = conn.createStatement();
			final ResultSet resultSet = statement.executeQuery( "select implicit_kind, mixed_kind from billz" );
			assertThat( resultSet.next() ).isTrue();
			assertThat( resultSet.getString( 1 ) ).isEqualTo( "CashPayment" );
			assertThat( resultSet.getString( 2 ) ).isEqualTo( "CardPayment" );
		} ) );

		sessions.inTransaction( (session) -> {
			final Bill bill = session.find( Bill.class, 1 );
			assertThat( bill.implicitPayment ).isInstanceOf( CashPayment.class );
			assertThat( bill.mixedPayment ).isInstanceOf( CardPayment.class );
		} );
	}

	@AfterEach
	@SessionFactory
	void dropTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( session -> {
			session.createMutationQuery( "delete Bill" ).executeUpdate();
			session.createMutationQuery( "delete CashPayment" ).executeUpdate();
			session.createMutationQuery( "delete CardPayment" ).executeUpdate();
		} );
	}

	@Entity(name="Bill")
	@Table(name="billz")
	public static class Bill {
		@Id
		private Integer id;
		private String name;

		@Any
		@AnyDiscriminator( valueStrategy = AnyDiscriminatorValueStrategy.IMPLICIT, implicitEntityShortName = true )
		@Column(name = "implicit_kind")
		@AnyKeyJavaClass( Integer.class )
		@JoinColumn( name = "implicit_fk" )
		private Payment implicitPayment;

		@Any
		@AnyDiscriminator( valueStrategy = AnyDiscriminatorValueStrategy.MIXED, implicitEntityShortName = true )
		@Column(name = "mixed_kind")
		@AnyKeyJavaClass( Integer.class )
		@JoinColumn( name = "mixed_fk" )
		private Payment mixedPayment;

		public Bill() {
		}

		public Bill(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
