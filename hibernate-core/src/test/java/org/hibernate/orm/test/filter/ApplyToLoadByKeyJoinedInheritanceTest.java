/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import java.util.List;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(annotatedClasses = {
		ApplyToLoadByKeyJoinedInheritanceTest.Wallet.class,
		ApplyToLoadByKeyJoinedInheritanceTest.Payment.class,
		ApplyToLoadByKeyJoinedInheritanceTest.CardPayment.class,
		ApplyToLoadByKeyJoinedInheritanceTest.WirePayment.class
})
@SessionFactory
@JiraKey("HHH-20675")
public class ApplyToLoadByKeyJoinedInheritanceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			CardPayment payment = new CardPayment();
			payment.tenantId = 1L;
			payment.cardNumber = "4111-1111-1111-1111";
			session.persist( payment );

			Wallet wallet = new Wallet();
			wallet.tenantId = 1L;
			wallet.payment = payment;
			session.persist( wallet );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testJoinFetchJoinedInheritanceWithLoadByKeyFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "tenantFilter" ).setParameter( "tenantId", 1L );

			List<Wallet> wallets = session.createQuery(
					"select w from Wallet w left join fetch w.payment",
					Wallet.class
			).getResultList();

			assertEquals( 1, wallets.size() );
			assertNotNull( wallets.get( 0 ).payment );
			assertInstanceOf( CardPayment.class, wallets.get( 0 ).payment );
			assertEquals( "4111-1111-1111-1111", ( (CardPayment) wallets.get( 0 ).payment ).cardNumber );
		} );
	}

	@Test
	public void testFindByIdWithLoadByKeyFilter(SessionFactoryScope scope) {
		Long paymentId = scope.fromTransaction( session ->
				session.createQuery( "select w.payment.id from Wallet w", Long.class ).getSingleResult()
		);

		scope.inTransaction( session -> {
			session.enableFilter( "tenantFilter" ).setParameter( "tenantId", 1L );
			Payment payment = session.find( Payment.class, paymentId );
			assertNotNull( payment );
			assertInstanceOf( CardPayment.class, payment );
			assertEquals( "4111-1111-1111-1111", ( (CardPayment) payment ).cardNumber );
		} );
	}

	@MappedSuperclass
	@FilterDef(
			name = "tenantFilter",
			applyToLoadByKey = true,
			defaultCondition = "tenant_id = :tenantId",
			parameters = @ParamDef(name = "tenantId", type = Long.class)
	)
	public abstract static class TenantAware {
		@Column(name = "tenant_id")
		Long tenantId;
	}

	@Entity(name = "Payment")
	@Table(name = "hh20675_payment")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Filter(name = "tenantFilter")
	public abstract static class Payment extends TenantAware {
		@Id
		@GeneratedValue
		Long id;
	}

	@Entity(name = "CardPayment")
	@Table(name = "hh20675_card_payment")
	@Filter(name = "tenantFilter")
	public static class CardPayment extends Payment {
		@Column(name = "card_number")
		String cardNumber;
	}

	@Entity(name = "WirePayment")
	@Table(name = "hh20675_wire_payment")
	@Filter(name = "tenantFilter")
	public static class WirePayment extends Payment {
		@Column(name = "iban")
		String iban;
	}

	@Entity(name = "Wallet")
	@Table(name = "hh20675_wallet")
	@Filter(name = "tenantFilter")
	public static class Wallet extends TenantAware {
		@Id
		@GeneratedValue
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "payment_id")
		Payment payment;
	}
}
