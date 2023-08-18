/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.CompositeType;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.RequiresDialect;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class JodaMoneyCompositeUserTypeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{Plan.class};
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Plan plan = new Plan();
			plan.setId(1L);
			plan.setOriginalPrice(Money.of(CurrencyUnit.USD, 10));
			plan.setPrice(Money.of(CurrencyUnit.USD, 9.9));
			entityManager.persist(plan);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			Plan plan = entityManager.find(Plan.class, 1L);
			assertEquals(10, plan.getOriginalPrice().getAmount().intValue());
			assertEquals(CurrencyUnit.USD, plan.getOriginalPrice().getCurrencyUnit());
		});
	}

	@Entity(name = "plans")
	public static class Plan {

		@Id
		private Long id;

		@CompositeType(JodaMoneyCompositeUserType.class)
		@AttributeOverrides({@AttributeOverride(name = "currency", column = @Column(name = "original_currency")), @AttributeOverride(name = "amount", column = @Column(name = "original_price"))})
		private org.joda.money.Money originalPrice;

		@CompositeType(JodaMoneyCompositeUserType.class)
		@AttributeOverrides({@AttributeOverride(name = "currency", column = @Column(name = "currency")), @AttributeOverride(name = "amount", column = @Column(name = "price"))})
		private org.joda.money.Money price;


		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Money getOriginalPrice() {
			return originalPrice;
		}

		public void setOriginalPrice(Money originalPrice) {
			this.originalPrice = originalPrice;
		}

		public Money getPrice() {
			return price;
		}

		public void setPrice(Money price) {
			this.price = price;
		}

	}
}
