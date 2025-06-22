/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				SubselectTest.Client.class,
				SubselectTest.Account.class,
				SubselectTest.AccountTransaction.class,
				SubselectTest.AccountSummary.class
		}
)
@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby doesn't support a CONCAT function")
@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase doesn't support a CONCAT function")
@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Firebird doesn't support a CONCAT function")
public class SubselectTest {

	@Test
	public void testNormalLifecycle(EntityManagerFactoryScope scope) {
		// same as `#testRefreshLifecycle` except that here we do not rely on
		// `Session#refresh` which atm in 6.0 dev does not appear to work properly
		// at least in this scenario

		// create the entity
		scope.inTransaction(
				(entityManager) -> {
					Client client = new Client();
					client.setId(1L);
					client.setFirstName("John");
					client.setLastName("Doe");
					entityManager.persist(client);

					Account account = new Account();
					account.setId(1L);
					account.setClient(client);
					account.setDescription("Checking account");
					entityManager.persist(account);

					AccountTransaction transaction = new AccountTransaction();
					transaction.setAccount(account);
					transaction.setDescription("Salary");
					transaction.setCents(100 * 7000);
					entityManager.persist(transaction);
				}
		);

		// load the entity, verify its state and add a new transaction (thereby updating the calculated balance)
		scope.inTransaction(
				(entityManager) -> {
					AccountSummary summary = entityManager.createQuery(
							"select s " +
									"from AccountSummary s " +
									"where s.id = :id", AccountSummary.class)
							.setParameter("id", 1L)
							.getSingleResult();

					assertEquals("John Doe", summary.getClientName());
					assertEquals(100 * 7000, summary.getBalance());

					AccountTransaction transaction = new AccountTransaction();
					transaction.setAccount(entityManager.getReference(Account.class, 1L));
					transaction.setDescription("Shopping");
					transaction.setCents(-100 * 2200);
					entityManager.persist(transaction);
				}
		);

		// load the AccountSummary and verify the updated balance
		scope.inTransaction(
				(entityManager) -> {
					AccountSummary summary = entityManager.find(AccountSummary.class, 1L);

					assertEquals("John Doe", summary.getClientName());
					assertEquals(100 * 4800, summary.getBalance());
				}
		);
	}

	@Test
	public void testRefreshLifecycle(EntityManagerFactoryScope scope) {
		//tag::mapping-Subselect-entity-find-example[]
		scope.inTransaction(
				(entityManager) -> {
					Client client = new Client();
					client.setId(1L);
					client.setFirstName("John");
					client.setLastName("Doe");
					entityManager.persist(client);

					Account account = new Account();
					account.setId(1L);
					account.setClient(client);
					account.setDescription("Checking account");
					entityManager.persist(account);

					AccountTransaction transaction = new AccountTransaction();
					transaction.setAccount(account);
					transaction.setDescription("Salary");
					transaction.setCents(100 * 7000);
					entityManager.persist(transaction);

					AccountSummary summary = entityManager.createQuery(
						"select s " +
						"from AccountSummary s " +
						"where s.id = :id", AccountSummary.class)
					.setParameter("id", account.getId())
					.getSingleResult();

					assertEquals("John Doe", summary.getClientName());
					assertEquals(100 * 7000, summary.getBalance());
				}
		);
		//end::mapping-Subselect-entity-find-example[]

		//tag::mapping-Subselect-entity-refresh-example[]
		scope.inTransaction(
				(entityManager) -> {
					AccountSummary summary = entityManager.find(AccountSummary.class, 1L);
					assertEquals("John Doe", summary.getClientName());
					assertEquals(100 * 7000, summary.getBalance());

					AccountTransaction transaction = new AccountTransaction();
					transaction.setAccount(entityManager.getReference(Account.class, 1L));
					transaction.setDescription("Shopping");
					transaction.setCents(-100 * 2200);
					entityManager.persist(transaction);
					entityManager.flush();

					entityManager.refresh(summary);
					assertEquals(100 * 4800, summary.getBalance());
				}
		);
		//end::mapping-Subselect-entity-refresh-example[]
	}

	@AfterEach
	public void dropTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	//tag::mapping-Subselect-example[]
	@Entity(name = "Client")
	@Table(name = "client")
	public static class Client {

		@Id
		private Long id;

		@Column(name = "first_name")
		private String firstName;

		@Column(name = "last_name")
		private String lastName;

		//Getters and setters omitted for brevity

		//end::mapping-Subselect-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		//tag::mapping-Subselect-example[]
	}

	@Entity(name = "Account")
	@Table(name = "account")
	public static class Account {

		@Id
		private Long id;

		@ManyToOne
		private Client client;

		private String description;

		//Getters and setters omitted for brevity

	//end::mapping-Subselect-example[]

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

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
		//tag::mapping-Subselect-example[]
	}

	@Entity(name = "AccountTransaction")
	@Table(name = "account_transaction")
	public static class AccountTransaction {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Account account;

		private Integer cents;

		private String description;

		//Getters and setters omitted for brevity

	//end::mapping-Subselect-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Account getAccount() {
			return account;
		}

		public void setAccount(Account account) {
			this.account = account;
		}

		public Integer getCents() {
			return cents;
		}

		public void setCents(Integer cents) {
			this.cents = cents;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		//tag::mapping-Subselect-example[]
	}

	@Entity(name = "AccountSummary")
	@Subselect(
		"select " +
		"	a.id as id, " +
		"	concat(concat(c.first_name, ' '), c.last_name) as clientName, " +
		"	sum(atr.cents) as balance " +
		"from account a " +
		"join client c on c.id = a.client_id " +
		"join account_transaction atr on a.id = atr.account_id " +
		"group by a.id, concat(concat(c.first_name, ' '), c.last_name)"
	)
	@Synchronize({"client", "account", "account_transaction"})
	public static class AccountSummary {

		@Id
		private Long id;

		private String clientName;

		private int balance;

		//Getters and setters omitted for brevity

	//end::mapping-Subselect-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getClientName() {
			return clientName;
		}

		public void setClientName(String clientName) {
			this.clientName = clientName;
		}

		public int getBalance() {
			return balance;
		}

		public void setBalance(int balance) {
			this.balance = balance;
		}
	//tag::mapping-Subselect-example[]
	}
	//end::mapping-Subselect-example[]
}
