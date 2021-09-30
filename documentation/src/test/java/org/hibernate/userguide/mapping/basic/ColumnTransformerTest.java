/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@NotImplementedYet(
		reason = "composition-as-basic (basic mapped to multiple columns) is no longer supported. See https://github.com/hibernate/hibernate-orm/discussions/3960",
		strict = false
)
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
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::basic-datetime-temporal-date-persist-example[]
			Savings savings = new Savings( );
			savings.setId( 1L );
			savings.setCurrency( Currency.getInstance( Locale.US ) );
			savings.setAmount( BigDecimal.TEN );
			entityManager.persist( savings );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Savings savings = entityManager.find( Savings.class, 1L );
			assertEquals( 10, savings.getAmount().intValue());
			assertEquals( Currency.getInstance( Locale.US ), savings.getCurrency() );
		} );
		//end::mapping-column-read-and-write-composite-type-persistence-example[]
	}

	//tag::mapping-column-read-and-write-composite-type-example[]
	@Entity(name = "Savings")
	public static class Savings {

		@Id
		private Long id;

		@ColumnTransformer(
				read = "amount / 100",
				write = "? * 100"
		)
		private BigDecimal amount;
		private Currency currency;

		//Getters and setters omitted for brevity

	//end::mapping-column-read-and-write-composite-type-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public Currency getCurrency() {
			return currency;
		}

		public void setCurrency(Currency currency) {
			this.currency = currency;
		}


	//tag::mapping-column-read-and-write-composite-type-example[]
	}
	//end::mapping-column-read-and-write-composite-type-example[]
}
