/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Where;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

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

	@Test
	public void testLifecycle() {
		//tag::mapping-where-persistence-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {

			Client client = new Client();
			client.setId( 1L );
			client.setName( "John Doe" );
			entityManager.persist( client );

			Account account1 = new Account( );
			account1.setId( 1L );
			account1.setType( AccountType.CREDIT );
			account1.setAmount( 5000d );
			account1.setRate( 1.25 / 100 );
			account1.setActive( true );
			account1.setClient( client );
			client.getCreditAccounts().add( account1 );
			entityManager.persist( account1 );

			Account account2 = new Account( );
			account2.setId( 2L );
			account2.setType( AccountType.DEBIT );
			account2.setAmount( 0d );
			account2.setRate( 1.05 / 100 );
			account2.setActive( false );
			account2.setClient( client );
			client.getDebitAccounts().add( account2 );
			entityManager.persist( account2 );

			Account account3 = new Account( );
			account3.setType( AccountType.DEBIT );
			account3.setId( 3L );
			account3.setAmount( 250d );
			account3.setRate( 1.05 / 100 );
			account3.setActive( true );
			account3.setClient( client );
			client.getDebitAccounts().add( account3 );
			entityManager.persist( account3 );
		} );
		//end::mapping-where-persistence-example[]


		//tag::mapping-where-entity-query-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Account> accounts = entityManager.createQuery(
				"select a from Account a", Account.class)
			.getResultList();
			assertEquals( 2, accounts.size());
		} );
		//end::mapping-where-entity-query-example[]

		//tag::mapping-where-collection-query-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {
			Client client = entityManager.find( Client.class, 1L );
			assertEquals( 1, client.getCreditAccounts().size() );
			assertEquals( 1, client.getDebitAccounts().size() );
		} );
		//end::mapping-where-collection-query-example[]
	}

	//tag::mapping-where-example[]
	public enum AccountType {
		DEBIT,
		CREDIT
	}

	@Entity(name = "Client")
	public static class Client {

		@Id
		private Long id;

		private String name;

		@Where( clause = "account_type = 'DEBIT'")
		@OneToMany(mappedBy = "client")
		private List<Account> debitAccounts = new ArrayList<>( );

		@Where( clause = "account_type = 'CREDIT'")
		@OneToMany(mappedBy = "client")
		private List<Account> creditAccounts = new ArrayList<>( );

		//Getters and setters omitted for brevity

		//end::mapping-where-example[]
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
		//tag::mapping-where-example[]
	}

	@Entity(name = "Account")
	@Where( clause = "active = true" )
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

	//end::mapping-where-example[]
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

		//tag::mapping-where-example[]
	}
	//end::mapping-where-example[]
}
