/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.formula;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.Formula;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Yanming Zhou
 * @author Nathan Xu
 */
@DomainModel( annotatedClasses = FormulaWithAliasTest.Customer.class )
@SessionFactory
@RequiresDialect(H2Dialect.class)
public class FormulaWithAliasTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Customer company1 = new Customer();
			company1.setBalance(new BigDecimal(100));
			company1.setVip(true);
			session.persist(company1);

			final Customer company2 = new Customer();
			company2.setBalance(new BigDecimal(1000));
			company2.setVip(false);
			session.persist(company2);
		} );
	}
	@Test
	@JiraKey(value = "HHH-12280")
	void testFormulaWithAlias(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Customer> customers = session.createQuery( "select c from Customer c ", Customer.class ).getResultList();

			assertEquals(2, customers.size());
			assertEquals(1d, customers.get(0).getPercentage().doubleValue(), 0);
			assertEquals(1d, customers.get(1).getPercentage().doubleValue(), 0);
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Customer")
	public static class Customer implements Serializable{

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private BigDecimal balance;

		@Formula("balance/(select sum(c.balance) from Customer c where c.vip = {alias}.vip)")
		private BigDecimal percentage;

		private boolean vip;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public BigDecimal getBalance() {
			return balance;
		}

		public void setBalance(BigDecimal balance) {
			this.balance = balance;
		}

		public BigDecimal getPercentage() {
			return percentage;
		}

		public void setPercentage(BigDecimal percentage) {
			this.percentage = percentage;
		}

		public boolean isVip() {
			return vip;
		}

		public void setVip(boolean vip) {
			this.vip = vip;
		}

	}

}
