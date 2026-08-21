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
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

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
@Jira("https://hibernate.atlassian.net/browse/HHH-20675")
public class ApplyToLoadByKeyJoinedInheritanceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var payment = new CardPayment();
			payment.id = 1L;
			payment.tenantId = 1L;
			payment.cardNumber = "4111-1111-1111-1111";
			session.persist( payment );

			final var wallet = new Wallet();
			wallet.id = 1L;
			wallet.payment = payment;
			session.persist( wallet );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testJoinFetchJoinedInheritanceWithLoadByKeyFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "tenantFilter" ).setParameter( "tenantId", 1L );
			final List<Wallet> wallets = session.createQuery(
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
		scope.inTransaction( session -> {
			session.enableFilter( "tenantFilter" ).setParameter( "tenantId", 1L );
			final var payment = session.find( Payment.class, 1L );
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
	@Inheritance(strategy = InheritanceType.JOINED)
	@Filter(name = "tenantFilter")
	public abstract static class Payment extends TenantAware {
		@Id
		Long id;
	}

	@Entity(name = "CardPayment")
	public static class CardPayment extends Payment {
		String cardNumber;
	}

	@Entity(name = "WirePayment")
	public static class WirePayment extends Payment {
		String iban;
	}

	@Entity(name = "Wallet")
	public static class Wallet {
		@Id
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		Payment payment;
	}
}
