/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria.filter;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.annotations.Where;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				WhereTests.Client.class,
				WhereTests.Account.class
		}
)
@SessionFactory
public class WhereTests {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			Client client = new Client();
			client.setId( 1L );
			client.setName( "John Doe" );
			session.persist( client );

			Account account1 = new Account( );
			account1.setId( 1L );
			account1.setType( AccountType.CREDIT );
			account1.setAmount( 5000d );
			account1.setRate( 1.25 / 100 );
			account1.setActive( true );
			account1.setClient( client );
			client.getCreditAccounts().add( account1 );
			session.persist( account1 );

			Account account2 = new Account( );
			account2.setId( 2L );
			account2.setType( AccountType.DEBIT );
			account2.setAmount( 0d );
			account2.setRate( 1.05 / 100 );
			account2.setActive( false );
			account2.setClient( client );
			client.getDebitAccounts().add( account2 );
			session.persist( account2 );

			Account account3 = new Account( );
			account3.setType( AccountType.DEBIT );
			account3.setId( 3L );
			account3.setAmount( 250d );
			account3.setRate( 1.05 / 100 );
			account3.setActive( true );
			account3.setClient( client );
			client.getDebitAccounts().add( account3 );
			session.persist( account3 );
		} );
	}

	@Test
	void testWhere(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder criteriaBuilder = scope.getSessionFactory().getCriteriaBuilder();
			final CriteriaQuery<Client> criteriaQuery = createCriteriaQuery( criteriaBuilder, Client.class, "id", 1L );
			final Client client = session.createQuery( criteriaQuery ).uniqueResult();

			assertThat( client.getCreditAccounts().size(), is( 1 ) );
			assertThat( client.getDebitAccounts().size(), is( 1 ) );
		} );
	}

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
		private List<Account> debitAccounts = new ArrayList<>();

		@Where( clause = "account_type = 'CREDIT'")
		@OneToMany(mappedBy = "client")
		private List<Account> creditAccounts = new ArrayList<>();

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
	}

	private static <T> CriteriaQuery<T> createCriteriaQuery(CriteriaBuilder criteriaBuilder, Class<T> entityClass, String idFieldName, Object idValue) {
		final CriteriaQuery<T> criteria = criteriaBuilder.createQuery( entityClass );
		Root<T> root = criteria.from( entityClass );
		criteria.select( root );
		criteria.where( criteriaBuilder.equal( root.get( idFieldName ), criteriaBuilder.literal( idValue ) ) );
		return criteria;
	}
}
