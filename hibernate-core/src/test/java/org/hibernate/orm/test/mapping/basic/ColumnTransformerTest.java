/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CompositeType;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class ColumnTransformerTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Savings.class
		};
	}

	@Test
	public void testLifecycle() {
		//tag::mapping-column-read-and-write-composite-type-persistence-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::basic-datetime-temporal-date-persist-example[]
			Savings savings = new Savings();
			savings.setId(1L);
			savings.setWallet(new MonetaryAmount(BigDecimal.TEN, Currency.getInstance(Locale.US)));
			entityManager.persist(savings);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			Savings savings = entityManager.find(Savings.class, 1L);
			assertEquals(10, savings.getWallet().getAmount().intValue());
			assertEquals(Currency.getInstance(Locale.US), savings.getWallet().getCurrency());
		});
		//end::mapping-column-read-and-write-composite-type-persistence-example[]
	}

	//tag::mapping-column-read-and-write-composite-type-example[]
	@Entity(name = "Savings")
	public static class Savings {

		@Id
		private Long id;

		@CompositeType(MonetaryAmountUserType.class)
		@AttributeOverrides({
			@AttributeOverride(name = "amount", column = @Column(name = "money")),
			@AttributeOverride(name = "currency", column = @Column(name = "currency"))
		})
		@ColumnTransformer(
				forColumn = "money",
				read = "money / 100",
				write = "? * 100"
		)
		private MonetaryAmount wallet;

		//Getters and setters omitted for brevity

	//end::mapping-column-read-and-write-composite-type-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MonetaryAmount getWallet() {
			return wallet;
		}

		public void setWallet(MonetaryAmount wallet) {
			this.wallet = wallet;
		}


	//tag::mapping-column-read-and-write-composite-type-example[]
	}
	//end::mapping-column-read-and-write-composite-type-example[]
}
