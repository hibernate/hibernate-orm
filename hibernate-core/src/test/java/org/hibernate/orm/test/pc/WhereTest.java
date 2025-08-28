/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class WhereTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Client.class,
			Account.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG.name() );
	}

	@Test
	public void testLifecycle() {
		//tag::pc-where-persistence-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {

			Client client = new Client();
			client.setId(1L);
			client.setName("John Doe");
			entityManager.persist(client);

			Account account1 = new Account();
			account1.setId(1L);
			account1.setType(AccountType.CREDIT);
			account1.setAmount(5000d);
			account1.setRate(1.25 / 100);
			account1.setActive(true);
			account1.setClient(client);
			client.getCreditAccounts().add(account1);
			entityManager.persist(account1);

			Account account2 = new Account();
			account2.setId(2L);
			account2.setType(AccountType.DEBIT);
			account2.setAmount(0d);
			account2.setRate(1.05 / 100);
			account2.setActive(false);
			account2.setClient(client);
			client.getDebitAccounts().add(account2);
			entityManager.persist(account2);

			Account account3 = new Account();
			account3.setType(AccountType.DEBIT);
			account3.setId(3L);
			account3.setAmount(250d);
			account3.setRate(1.05 / 100);
			account3.setActive(true);
			account3.setClient(client);
			client.getDebitAccounts().add(account3);
			entityManager.persist(account3);
		});
		//end::pc-where-persistence-example[]


		//tag::pc-where-entity-query-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			List<Account> accounts = entityManager.createQuery(
				"select a from Account a", Account.class)
			.getResultList();
			assertEquals(2, accounts.size());
		});
		//end::pc-where-entity-query-example[]

		//tag::pc-where-collection-query-example[]
		doInJPA(this::entityManagerFactory, entityManager -> {
			Client client = entityManager.find(Client.class, 1L);
			assertEquals(1, client.getCreditAccounts().size());
			assertEquals(1, client.getDebitAccounts().size());
		});
		//end::pc-where-collection-query-example[]
	}

	//tag::pc-where-example[]
	public enum AccountType {
		DEBIT,
		CREDIT
	}

	@Entity(name = "Client")
	public static class Client {

		@Id
		private Long id;

		private String name;

		@SQLRestriction("account_type = 'DEBIT'")
		@OneToMany(mappedBy = "client")
		private List<Account> debitAccounts = new ArrayList<>();

		@SQLRestriction("account_type = 'CREDIT'")
		@OneToMany(mappedBy = "client")
		private List<Account> creditAccounts = new ArrayList<>();

		//Getters and setters omitted for brevity

		//end::pc-where-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Account> getDebitAccounts() {
			return debitAccounts;
		}

		public List<Account> getCreditAccounts() {
			return creditAccounts;
		}
		//tag::pc-where-example[]
	}

	@Entity(name = "Account")
	@SQLRestriction("active = true")
	public static class Account {

		@Id
		private Long id;

		@ManyToOne
		private Client client;

		@Column(name = "account_type")
		@Enumerated(EnumType.STRING)
		private AccountType type;

		private Double amount;

		private Double rate;

		private boolean active;

		//Getters and setters omitted for brevity

	//end::pc-where-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Client getClient() {
			return client;
		}

		public void setClient(Client client) {
			this.client = client;
		}

		public AccountType getType() {
			return type;
		}

		public void setType(AccountType type) {
			this.type = type;
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

		//tag::pc-where-example[]
	}
	//end::pc-where-example[]
}
