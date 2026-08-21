/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.fetch;

import java.util.List;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				JoinFetchAnyQueryCacheTest.Purchase.class,
				JoinFetchAnyQueryCacheTest.CardPayment.class,
				JoinFetchAnyQueryCacheTest.CashPayment.class,
				JoinFetchAnyQueryCacheTest.Currency.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = CacheSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = CacheSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = CacheSettings.QUERY_CACHE_LAYOUT, value = "shallow")
		}
)
@SessionFactory(generateStatistics = true, useCollectingStatementInspector = true)
class JoinFetchAnyQueryCacheTest {

	private static final String CARD_PAYMENT_CURRENCY_PROFILE = "card-payment-currency";
	private static final String HQL = "select p from Purchase p join fetch p.payment order by p.id";

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
		scope.getSessionFactory().getStatistics().clear();
	}

	@Test
	void testShallowQueryCacheHitInitializesNestedFetchUnderAnyAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Currency eur = new Currency( 1L, "EUR" );
			final CardPayment cardPayment = new CardPayment( 1L, eur, "4111111111111111" );
			final CashPayment cashPayment = new CashPayment( 2L, "store credit" );
			session.persist( eur );
			session.persist( cardPayment );
			session.persist( cashPayment );
			session.persist( new Purchase( 1L, cardPayment ) );
			session.persist( new Purchase( 2L, cashPayment ) );
		} );

		final Statistics statistics = scope.getSessionFactory().getStatistics();
		final var statementInspector = scope.getCollectingStatementInspector();

		statistics.clear();
		statementInspector.clear();

		final List<Purchase> firstRunPurchases = scope.fromTransaction( session -> {
			session.enableFetchProfile( CARD_PAYMENT_CURRENCY_PROFILE );
			return findPurchases( session );
		} );
		assertEquals( 1, statementInspector.getSqlQueries().size() );
		assertGraphLoaded( firstRunPurchases );

		assertEquals( 0, statistics.getQueryCacheHitCount() );
		assertEquals( 1, statistics.getQueryCacheMissCount() );
		assertEquals( 1, statistics.getQueryCachePutCount() );

		statistics.clear();
		statementInspector.clear();

		final List<Purchase> secondRunPurchases = scope.fromTransaction( session -> {
			session.enableFetchProfile( CARD_PAYMENT_CURRENCY_PROFILE );
			return findPurchases( session );
		} );

		assertEquals( 0, statementInspector.getSqlQueries().size() );
		assertEquals( 1, statistics.getQueryCacheHitCount() );
		assertEquals( 0, statistics.getQueryCacheMissCount() );
		assertEquals( 0, statistics.getQueryCachePutCount() );
		assertEquals( 0, statistics.getQueryStatistics( HQL ).getExecutionCount() );
		assertGraphLoaded( secondRunPurchases );
	}

	private static List<Purchase> findPurchases(Session session) {
		return session.createSelectionQuery( HQL, Purchase.class )
				.setCacheable( true )
				.getResultList();
	}

	private static void assertGraphLoaded(List<Purchase> purchases) {
		assertEquals( 2, purchases.size() );

		final Purchase cardPurchase = purchases.get( 0 );
		assertTrue( isInitialized( cardPurchase.payment ) );
		assertInstanceOf( CardPayment.class, cardPurchase.payment );
		final CardPayment cardPayment = (CardPayment) cardPurchase.payment;
		assertNotNull( cardPayment. getCurrency(), "Shallow query-cache hit lost CardPayment.currency" );
		assertTrue( isInitialized( cardPayment.getCurrency() ) );
		assertEquals( "EUR", cardPayment.getCurrency().getCode() );

		final Purchase cashPurchase = purchases.get( 1 );
		assertTrue( isInitialized( cashPurchase.payment ) );
		assertInstanceOf( CashPayment.class, cashPurchase.payment );
	}

	@Entity(name = "Purchase")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Purchase {
		@Id
		Long id;

		@Any(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@JoinColumn(name = "PAYMENT_ID")
		@Column(name = "PAYMENT_TYPE")
		@AnyDiscriminatorValue(discriminator = "CARD", entity = CardPayment.class)
		@AnyDiscriminatorValue(discriminator = "CASH", entity = CashPayment.class)
		Payment payment;

		Purchase() {
		}

		Purchase(Long id, Payment payment) {
			this.id = id;
			this.payment = payment;
		}
	}

	interface Payment {
	}

	@Entity(name = "CardPayment")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@FetchProfile(
			name = CARD_PAYMENT_CURRENCY_PROFILE,
			fetchOverrides = @FetchProfile.FetchOverride(
					entity = CardPayment.class,
					association = "currency",
					mode = FetchMode.JOIN
			)
	)
	static class CardPayment implements Payment {
		@Id
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		Currency currency;

		String cardNumber;

		public Currency getCurrency() {
			return currency;
		}

		CardPayment() {
		}

		CardPayment(Long id, Currency currency, String cardNumber) {
			this.id = id;
			this.currency = currency;
			this.cardNumber = cardNumber;
		}
	}

	@Entity(name = "CashPayment")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class CashPayment implements Payment {
		@Id
		Long id;

		String label;

		CashPayment() {
		}

		CashPayment(Long id, String label) {
			this.id = id;
			this.label = label;
		}
	}

	@Entity(name = "Currency")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Currency {
		@Id
		Long id;

		String code;

		String getCode() {
			return code;
		}

		Currency() {
		}

		Currency(Long id, String code) {
			this.id = id;
			this.code = code;
		}
	}
}
