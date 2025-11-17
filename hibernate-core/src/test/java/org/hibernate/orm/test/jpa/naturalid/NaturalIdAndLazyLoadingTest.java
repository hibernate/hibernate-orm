/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.naturalid;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;

import static org.assertj.core.api.Assertions.assertThat;


@Jpa(
		annotatedClasses = {
				NaturalIdAndLazyLoadingTest.Wallet.class,
				NaturalIdAndLazyLoadingTest.Currency.class
		}
)
@JiraKey(value = "HHH-15481")
public class NaturalIdAndLazyLoadingTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Currency currency = new Currency( 1, "GPB" );
					Wallet position = new Wallet( 1, new BigDecimal( 1 ), currency );

					entityManager.persist( currency );
					entityManager.persist( position );
				}
		);
	}

	@Test
	public void testCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Wallet> criteriaQuery = criteriaBuilder.createQuery( Wallet.class );
					Root<Wallet> walletRoot = criteriaQuery.from( Wallet.class );

					ParameterExpression<Integer> parameter = criteriaBuilder.parameter( Integer.class, "p" );

					criteriaQuery.where( walletRoot.get( "id" ).in( parameter ) );

					TypedQuery<Wallet> query = entityManager.createQuery( criteriaQuery );
					query.setParameter( "p", 1 );
					List<Wallet> wallets = query.getResultList();

					assertThat( wallets.size() ).isEqualTo( 1 );

					// Currency cannot be lazy initialized without Bytecode Enhancement because of the @JoinColumn(referencedColumnName = "isoCode", nullable = false)
					assertThat( Hibernate.isInitialized( wallets.get( 0 ).getCurrency() ) ).isTrue();
				}
		);
	}

	@Entity(name = "Wallet")
	@Table(name = "WALLET_TABLE")
	public static class Wallet {
		@Id
		private Integer id;

		@Embedded
		private Amount amount;

		public Wallet() {
		}

		public Wallet(Integer id, BigDecimal quantity, Currency currency) {
			this.id = id;
			amount = new Amount( quantity, currency );
		}

		public Amount getAmount() {
			return amount;
		}

		public Currency getCurrency() {
			if ( amount == null ) {
				return null;
			}
			return amount.getCurrency();
		}
	}

	@Embeddable
	public static class Amount {

		private BigDecimal quantity;

		@JoinColumn(referencedColumnName = "isoCode", nullable = false)
		@ManyToOne(fetch = FetchType.LAZY)
		private Currency currency;

		public Amount() {
		}

		public Amount(BigDecimal quantity, Currency currency) {
			this.quantity = quantity;
			this.currency = currency;
		}

		public BigDecimal getQuantity() {
			return quantity;
		}

		public Currency getCurrency() {
			return currency;
		}
	}

	@Entity(name = "Currency")
	@Table(name = "CURRENCY_TABLE")
	public static class Currency {
		@Id
		private Integer id;

		@NaturalId
		@NotNull
		private String isoCode;

		public Currency() {
		}

		public Currency(Integer id, String isoCode) {
			this.id = id;
			this.isoCode = isoCode;
		}
	}
}
