/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Formula;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class FormulaTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Account.class
		};
	}

	@Test
	public void testLifecycle() {
		//tag::mapping-column-formula-persistence-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::basic-datetime-temporal-date-persist-example[]
			Account account = new Account();
			account.setId(1L);
			account.setCredit(5000d);
			account.setRate(1.25 / 100);
			entityManager.persist(account);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			Account account = entityManager.find(Account.class, 1L);
			assertEquals(Double.valueOf(62.5d), account.getInterest());
		});
		//end::mapping-column-formula-persistence-example[]
	}

	//tag::mapping-column-formula-example[]
	@Entity(name = "Account")
	public static class Account {

		@Id
		private Long id;

		private Double credit;

		private Double rate;

		@Formula(value = "credit * rate")
		private Double interest;

		//Getters and setters omitted for brevity

	//end::mapping-column-formula-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Double getCredit() {
			return credit;
		}

		public void setCredit(Double credit) {
			this.credit = credit;
		}

		public Double getRate() {
			return rate;
		}

		public void setRate(Double rate) {
			this.rate = rate;
		}

		public Double getInterest() {
			return interest;
		}

		public void setInterest(Double interest) {
			this.interest = interest;
		}

		//tag::mapping-column-formula-example[]
	}
	//end::mapping-column-formula-example[]
}
