/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.loading.filter;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				FilterBasicsTests.Client.class,
				FilterBasicsTests.Account.class
		}
)
@ServiceRegistry(
		settings = @ServiceRegistry.Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@SessionFactory
public class FilterBasicsTests implements SessionFactoryScopeAware {

	private SessionFactoryScope scope;

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Client client = new Client()
					.setId( 1L )
					.setName( "John Doe" );

			client.addAccount(
					new Account()
							.setId( 1L )
							.setType( AccountType.CREDIT )
							.setAmount( 5000d )
							.setRate( 1.25 / 100 )
							.setActive( true )
			);

			client.addAccount(
					new Account()
							.setId( 2L )
							.setType( AccountType.DEBIT )
							.setAmount( 0d )
							.setRate( 1.05 / 100 )
							.setActive( false )
			);

			client.addAccount(
					new Account()
							.setType( AccountType.DEBIT )
							.setId( 3L )
							.setAmount( 250d )
							.setRate( 1.05 / 100 )
							.setActive( true )
			);
			session.persist( client );
		} );
	}

	@ParameterizedTest
	@ValueSource( strings = { "true", "false" } )
	void testLoadFilterOnEntity(boolean enableFilter) {
		scope.inTransaction( session -> {
			if ( enableFilter ) {
				session.enableFilter( "activeAccount" )
						.setParameter( "active", true );
			}
			Account account1 = session.find( Account.class, 1L );
			Account account2 = session.find( Account.class, 2L );
			assertThat( account1, notNullValue() );
			assertThat( account2, notNullValue() );
		} );
	}

	@ParameterizedTest
	@ValueSource( strings = { "true", "false" } )
	void testLoadFilterOnCollectionField(boolean enableFilter) {
		scope.inTransaction( session -> {
			if ( enableFilter ) {
				session.enableFilter( "activeAccount" )
						.setParameter( "active", true );
			}
			Client client = session.find( Client.class, 1L );

			if ( enableFilter ) {
				assertThat( client.getAccounts().size(), is( 2 ) );
			}
			else {
				assertThat( client.getAccounts().size(), is( 3 ) );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from Account" ).executeUpdate();
			session.createQuery( "delete from Client" ).executeUpdate();
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

		@OneToMany(
				mappedBy = "client",
				cascade = CascadeType.ALL
		)
		@Filter(
				name="activeAccount",
				condition="active_status = :active"
		)
		private List<Account> accounts = new ArrayList<>();

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

		public void addAccount(Account account) {
			account.setClient( this );
			this.accounts.add( account );
		}
	}

	@Entity(name = "Account")
	@FilterDef(
			name="activeAccount",
			parameters = @ParamDef(
					name="active",
					type="boolean"
			)
	)
	@Filter(
			name="activeAccount",
			condition="active_status = :active"
	)
	public static class Account {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Client client;

		@Column(name = "account_type")
		@Enumerated(EnumType.STRING)
		private AccountType type;

		private Double amount;

		private Double rate;

		@Column(name = "active_status")
		private boolean active;

		public Long getId() {
			return id;
		}

		public Account setId(Long id) {
			this.id = id;
			return this;
		}

		public Client getClient() {
			return client;
		}

		public Account setClient(Client client) {
			this.client = client;
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

		public boolean isActive() {
			return active;
		}

		public Account setActive(boolean active) {
			this.active = active;
			return this;
		}
	}

}
