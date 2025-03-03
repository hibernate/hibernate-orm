/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class FilterSqlFragementAliasTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Account.class
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {

			Account account1 = new Account();
			account1.setId(1L);
			account1.setAmount(5000d);
			account1.setRate(1.25 / 100);
			account1.setActive(true);
			entityManager.persist(account1);

			Account account2 = new Account();
			account2.setId(2L);
			account2.setAmount(0d);
			account2.setRate(1.05 / 100);
			account2.setActive(false);
			entityManager.persist(account2);

			Account account3 = new Account();
			account3.setId(3L);
			account3.setAmount(250d);
			account3.setRate(1.05 / 100);
			account3.setActive(true);
			entityManager.persist(account3);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			log.infof("Activate filter [%s]", "activeAccount");

			//tag::pc-filter-sql-fragment-alias-query-example[]
			entityManager
				.unwrap(Session.class)
				.enableFilter("activeAccount")
				.setParameter("active", true);

			List<Account> accounts = entityManager.createQuery(
				"select a from Account a", Account.class)
			.getResultList();
			//end::pc-filter-sql-fragment-alias-query-example[]
			assertEquals(2, accounts.size());
		});
	}

	//tag::pc-filter-sql-fragment-alias-example[]
	@Entity(name = "Account")
	@Table(name = "account")
	@Comment(on="account", value = "The account table")
	@SecondaryTable(
		name = "account_details"
	)
	@Comment(on="account_details", value = "The account details secondary table")
	@SQLDelete(
		sql = "UPDATE account_details SET deleted = true WHERE id = ? "
	)
	@FilterDef(
		name="activeAccount",
		parameters = @ParamDef(
			name="active",
			type=Boolean.class
		)
	)
	@Filter(
		name="activeAccount",
		condition="{a}.active = :active and {ad}.deleted = false",
		aliases = {
			@SqlFragmentAlias(alias = "a", table= "account"),
			@SqlFragmentAlias(alias = "ad", table= "account_details"),
		}
	)
	public static class Account {

		@Id
		private Long id;

		private Double amount;

		private Double rate;

		private boolean active;

		@Column(table = "account_details")
		private boolean deleted;

		//Getters and setters omitted for brevity

	//end::pc-filter-sql-fragment-alias-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Double getAmount() {
			return amount;
		}

		public void setAmount(Double amount) {
			this.amount = amount;
		}

		public Double getRate() {
			return rate;
		}

		public void setRate(Double rate) {
			this.rate = rate;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		//tag::pc-filter-sql-fragment-alias-example[]
	}
	//end::pc-filter-sql-fragment-alias-example[]
}
