/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.Session;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.ParamDef;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
			FilterJoinTableTest.Client.class,
				FilterJoinTableTest.Account.class
		}
)
public class FilterJoinTableTest  {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::pc-filter-join-table-persistence-example[]
			Client client = new Client()
			.setId(1L)
			.setName("John Doe");

			client.addAccount(
				new Account()
				.setId(1L)
				.setType(AccountType.CREDIT)
				.setAmount(5000d)
				.setRate(1.25 / 100)
		);

			client.addAccount(
				new Account()
				.setId(2L)
				.setType(AccountType.DEBIT)
				.setAmount(0d)
				.setRate(1.05 / 100)
		);

			client.addAccount(
				new Account()
				.setType(AccountType.DEBIT)
				.setId(3L)
				.setAmount(250d)
				.setRate(1.05 / 100)
		);

			entityManager.persist(client);
			//end::pc-filter-join-table-persistence-example[]
		});

		scope.inTransaction(  entityManager -> {
			//tag::pc-no-filter-join-table-collection-query-example[]
			Client client = entityManager.find(Client.class, 1L);

			assertThat(client.getAccounts()).hasSize( 3 );
			//end::pc-no-filter-join-table-collection-query-example[]
		});

		scope.inTransaction(  entityManager -> {
			//tag::pc-filter-join-table-collection-query-example[]
			Client client = entityManager.find(Client.class, 1L);

			entityManager
				.unwrap(Session.class)
				.enableFilter("firstAccounts")
				.setParameter("maxOrderId", 1);

			assertThat(client.getAccounts()).hasSize( 2 );
			//end::pc-filter-join-table-collection-query-example[]
		});
	}

	public enum AccountType {
		DEBIT,
		CREDIT
	}

	//tag::pc-filter-join-table-example[]
	@Entity(name = "Client")
	@FilterDef(
		name="firstAccounts",
		parameters=@ParamDef(
			name="maxOrderId",
			type=int.class
	)
)
	public static class Client {

		@Id
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL)
		@OrderColumn(name = "order_id")
		@FilterJoinTable(
			name="firstAccounts",
			condition="order_id <= :maxOrderId"
	)
		private List<Account> accounts = new ArrayList<>();

		//Getters and setters omitted for brevity
		//end::pc-filter-join-table-example[]
		public Long getId() {
			return id;
		}

		public Client setId(Long id) {
			this.id = id;
			return this;
		}

		public String getName() {
			return name;
		}

		public Client setName(String name) {
			this.name = name;
			return this;
		}

		public List<Account> getAccounts() {
			return accounts;
		}
		//tag::pc-filter-join-table-example[]

		public void addAccount(Account account) {
			this.accounts.add(account);
		}
	}

	@Entity(name = "Account")
	public static class Account {

		@Id
		private Long id;

		@Column(name = "account_type")
		@Enumerated(EnumType.STRING)
		private AccountType type;

		private Double amount;

		private Double rate;

		//Getters and setters omitted for brevity
	//end::pc-filter-join-table-example[]
		public Long getId() {
			return id;
		}

		public Account setId(Long id) {
			this.id = id;
			return this;
		}

		public AccountType getType() {
			return type;
		}

		public Account setType(AccountType type) {
			this.type = type;
			return this;
		}

		public Double getAmount() {
			return amount;
		}

		public Account setAmount(Double amount) {
			this.amount = amount;
			return this;
		}

		public Double getRate() {
			return rate;
		}

		public Account setRate(Double rate) {
			this.rate = rate;
			return this;
		}

		//tag::pc-filter-join-table-example[]
	}
	//end::pc-filter-join-table-example[]
}
