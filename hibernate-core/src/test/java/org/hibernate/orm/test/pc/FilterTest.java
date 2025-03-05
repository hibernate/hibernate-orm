/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.EntityFilterException;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.ParamDef;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class FilterTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Client.class,
			Account.class,
			AccountEager.class,
			AccountNotFound.class,
			AccountNotFoundException.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG.name() );
	}

	@Before
	public void setup() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::pc-filter-persistence-example[]
			Client client = new Client()
					.setId(1L)
					.setName("John Doe")
					.setType(AccountType.DEBIT);

			Account account1;
			client.addAccount(
					account1 = new Account()
							.setId(1L)
							.setType(AccountType.CREDIT)
							.setAmount(5000d)
							.setRate(1.25 / 100)
							.setActive(true)
			);

			client.addAccount(
					new Account()
							.setId(2L)
							.setType(AccountType.DEBIT)
							.setAmount(0d)
							.setRate(1.05 / 100)
							.setActive(false)
							.setParentAccount( account1 )
			);

			client.addAccount(
					new Account()
							.setType(AccountType.DEBIT)
							.setId(3L)
							.setAmount(250d)
							.setRate(1.05 / 100)
							.setActive(true)
			);

			entityManager.persist(client);
			//end::pc-filter-persistence-example[]
			entityManager.persist(
					new AccountEager()
							.setId(2L)
							.setParentAccount( account1 )
			);
			entityManager.persist(
					new AccountNotFound()
							.setId(2L)
							.setParentAccount( account1 )
			);
			entityManager.persist(
					new AccountNotFoundException()
							.setId(2L)
							.setParentAccount( account1 )
			);
		});
	}

	@After
	public void tearDown() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "update Account set parentAccount = null" ).executeUpdate();
			entityManager.createQuery( "delete from AccountEager" ).executeUpdate();
			entityManager.createQuery( "delete from AccountNotFound" ).executeUpdate();
			entityManager.createQuery( "delete from AccountNotFoundException" ).executeUpdate();
			entityManager.createQuery( "delete from Account" ).executeUpdate();
			entityManager.createQuery( "delete from Client" ).executeUpdate();
		});
	}

	@Test
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			log.infof("Activate filter [%s]", "activeAccount");

			entityManager
					.unwrap(Session.class)
					.enableFilter("activeAccount")
					.setParameter("active", true);

			Account account1 = entityManager.find(Account.class, 1L);
			Account account2 = entityManager.find(Account.class, 2L);

			assertNotNull(account1);
			assertNotNull(account2);
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			log.infof("Activate filter [%s]", "activeAccount");

			entityManager
					.unwrap(Session.class)
					.enableFilter("activeAccount")
					.setParameter("active", true);

			Account account1 = entityManager.createQuery(
					"select a from Account a where a.id = :id", Account.class)
					.setParameter("id", 1L)
					.getSingleResult();
			assertNotNull(account1);
			try {
				Account account2 = entityManager.createQuery(
						"select a from Account a where a.id = :id", Account.class)
						.setParameter("id", 2L)
						.getSingleResult();
			}
			catch (NoResultException expected) {
			}
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			log.infof("Activate filter [%s]", "activeAccount");
			//tag::pc-filter-entity-example[]
			entityManager
				.unwrap(Session.class)
				.enableFilter("activeAccount")
				.setParameter("active", true);

			Account account = entityManager.find(Account.class, 2L);

			assertFalse( account.isActive() );
			//end::pc-filter-entity-example[]
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::pc-no-filter-entity-query-example[]
			List<Account> accounts = entityManager.createQuery(
				"select a from Account a", Account.class)
			.getResultList();

			assertEquals(3, accounts.size());
			//end::pc-no-filter-entity-query-example[]
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			log.infof("Activate filter [%s]", "activeAccount");
			//tag::pc-filter-entity-query-example[]
			entityManager
				.unwrap(Session.class)
				.enableFilter("activeAccount")
				.setParameter("active", true);

			List<Account> accounts = entityManager.createQuery(
				"select a from Account a", Account.class)
			.getResultList();

			assertEquals(2, accounts.size());
			//end::pc-filter-entity-query-example[]
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::pc-no-filter-collection-query-example[]
			Client client = entityManager.find(Client.class, 1L);

			assertEquals(3, client.getAccounts().size());
			//end::pc-no-filter-collection-query-example[]
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			log.infof("Activate filter [%s]", "activeAccount");

			//tag::pc-filter-collection-query-example[]
			entityManager
				.unwrap(Session.class)
				.enableFilter("activeAccount")
				.setParameter("active", true);

			Client client = entityManager.find(Client.class, 1L);

			assertEquals(2, client.getAccounts().size());
			//end::pc-filter-collection-query-example[]
		});
	}

	@Test
	@JiraKey("HHH-16830")
	public void testApplyToLoadByKey() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			log.infof("Activate filter [%s]", "minimumAmount");
			//tag::pc-filter-entity-example[]
			entityManager
					.unwrap(Session.class)
					.enableFilter("minimumAmount")
					.setParameter("amount", 9000d);

			Account account = entityManager.find(Account.class, 1L);

			assertNull( account );
			//end::pc-filter-entity-example[]
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			log.infof("Activate filter [%s]", "minimumAmount");
			//tag::pc-filter-entity-example[]
			entityManager
					.unwrap(Session.class)
					.enableFilter("minimumAmount")
					.setParameter("amount", 100d);

			Account account = entityManager.find(Account.class, 1L);

			assertNotNull( account );
			//end::pc-filter-entity-example[]
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			log.infof("Activate filter [%s]", "minimumAmount");
			//tag::pc-filter-entity-query-example[]
			entityManager
					.unwrap(Session.class)
					.enableFilter("minimumAmount")
					.setParameter("amount", 500d);

			List<Account> accounts = entityManager.createQuery(
							"select a from Account a", Account.class)
					.getResultList();

			assertEquals(1, accounts.size());
			//end::pc-filter-entity-query-example[]
		});
	}

	@Test
	@JiraKey("HHH-16830")
	public void testApplyToLoadByKeyAssociationFiltering() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Account account = entityManager.find( Account.class, 2L );
			assertNotNull( account.getParentAccount() );
		} );
	}

	@Test
	@JiraKey("HHH-16830")
	public void testApplyToLoadByKeyAssociationFilteringLazyInitialization() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class )
					.enableFilter( "accountType" )
					.setParameter( "type", "DEBIT" );

			Account account = entityManager.find( Account.class, 2L );
			EntityNotFoundException exception = assertThrows(
					EntityNotFoundException.class,
					() -> Hibernate.initialize( account.getParentAccount() )
			);
			// Account with id 1 does not exist
			assertTrue( exception.getMessage().endsWith( "'1']" ) );
		} );
	}

	@Test
	@JiraKey("HHH-16830")
	public void testApplyToLoadByKeyAssociationFilteringAccountLoadGraphInitializer() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class )
					.enableFilter( "accountType" )
					.setParameter( "type", "DEBIT" );
			EntityGraph<Account> entityGraph = entityManager.createEntityGraph( Account.class );
			entityGraph.addAttributeNodes( "parentAccount" );

			EntityFilterException exception = assertThrows(
					EntityFilterException.class,
					() -> entityManager.find(
							Account.class,
							2L,
							Map.of( AvailableHints.HINT_SPEC_LOAD_GRAPH, entityGraph )
					)
			);
			// Account with id 1 does not exist
			assertTrue( exception.getRole().endsWith( "parentAccount" ) );
			assertEquals( 1L, exception.getIdentifier() );
		} );
	}

	@Test
	@JiraKey("HHH-16830")
	public void testApplyToLoadByKeyAssociationFilteringAccountJoinInitializer() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class )
					.enableFilter( "accountType" )
					.setParameter( "type", "DEBIT" );

			EntityFilterException exception = assertThrows(
					EntityFilterException.class,
					() -> entityManager.createQuery(
							"select a from Account a left join fetch a.parentAccount where a.id = 2",
							Account.class
					).getResultList()
			);
			// Account with id 1 does not exist
			assertTrue( exception.getRole().contains( "parentAccount" ) );
			assertEquals( 1L, exception.getIdentifier() );
		} );
	}

	@Test
	@JiraKey("HHH-16830")
	public void testApplyToLoadByKeyAssociationFilteringAccountSelectInitializer() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class )
					.enableFilter( "accountType" )
					.setParameter( "type", "DEBIT" );

			EntityFilterException exception = assertThrows(
					EntityFilterException.class,
					() -> entityManager.createQuery(
							"select a from AccountEager a where a.id = 2",
							AccountEager.class
					).getResultList()
			);
			// Account with id 1 does not exist
			assertTrue( exception.getRole().contains( "parentAccount" ) );
			assertEquals( 1L, exception.getIdentifier() );
		} );
	}

	@Test
	@JiraKey("HHH-16830")
	public void testApplyToLoadByKeyAssociationFilteringAccountNotFoundException() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.unwrap(Session.class)
					.enableFilter("accountType")
					.setParameter("type", "DEBIT");

			EntityFilterException exception = assertThrows(
					EntityFilterException.class,
					() -> entityManager.createQuery(
							"select a from AccountNotFoundException a where a.id = 2",
							AccountNotFoundException.class
					).getSingleResult()
			);
			// Account with id 1 does not exist
			assertTrue( exception.getRole().contains( "parentAccount" ) );
			assertEquals( 1L, exception.getIdentifier() );
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.unwrap(Session.class)
					.enableFilter("accountType")
					.setParameter("type", "DEBIT");

			EntityFilterException exception = assertThrows(
					EntityFilterException.class,
					() -> entityManager.createQuery(
							"select a from AccountNotFoundException a left join fetch a.parentAccount where a.id = 2",
							AccountNotFoundException.class
					).getSingleResult()
			);
			// Account with id 1 does not exist
			assertTrue( exception.getRole().contains( "parentAccount" ) );
			assertEquals( 1L, exception.getIdentifier() );
		});
	}

	@Test
	@JiraKey("HHH-16830")
	public void testApplyToLoadByKeyAssociationFilteringAccountNotFoundIgnore() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.unwrap(Session.class)
					.enableFilter("accountType")
					.setParameter("type", "DEBIT");

			AccountNotFound account = entityManager.createQuery(
					"select a from AccountNotFound a where a.id = 2",
					AccountNotFound.class
			).getSingleResult();
			// No exception, since we use NotFoundAction.IGNORE
			assertNull( account.getParentAccount() );
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.unwrap(Session.class)
					.enableFilter("accountType")
					.setParameter("type", "DEBIT");

			AccountNotFound account = entityManager.createQuery(
					"select a from AccountNotFound a left join fetch a.parentAccount where a.id = 2",
					AccountNotFound.class
			).getSingleResult();
			// No exception, since we use NotFoundAction.IGNORE
			assertNull( account.getParentAccount() );
		});
	}

	public enum AccountType {
		DEBIT,
		CREDIT
	}

	//tag::pc-filter-Client-example[]
	@Entity(name = "Client")
	@Table(name = "client")
	public static class Client {

		@Id
		private Long id;

		private String name;

		private AccountType type;

		@OneToMany(
			mappedBy = "client",
			cascade = CascadeType.ALL
	)
		@Filter(
			name="activeAccount",
			condition="active_status = :active"
	)
		private List<Account> accounts = new ArrayList<>();

		//Getters and setters omitted for brevity
	//end::pc-filter-Client-example[]
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

		public AccountType getType() {
			return type;
		}

		public Client setType(AccountType type) {
			this.type = type;
			return this;
		}

		public List<Account> getAccounts() {
			return accounts;
		}
	//tag::pc-filter-Client-example[]

		public void addAccount(Account account) {
			account.setClient(this);
			this.accounts.add(account);
		}
	}
	//end::pc-filter-Client-example[]

	//tag::pc-filter-Account-example[]
	@Entity(name = "Account")
	@Table(name = "account")
	@FilterDef(
		name="activeAccount",
		parameters = @ParamDef(
			name="active",
			type=Boolean.class
	)
)
	@Filter(
			name="activeAccount",
			condition="active_status = :active"
	)
	@FilterDef(
			name="minimumAmount",
			parameters = @ParamDef(
					name="amount",
					type=Double.class
			),
			applyToLoadByKey = true
	)
	@Filter(
			name="minimumAmount",
			condition="amount > :amount"
	)
	@FilterDef(
			name="accountType",
			parameters = @ParamDef(
					name="type",
					type=String.class
			),
			applyToLoadByKey = true
	)
	@Filter(
			name="accountType",
			condition="account_type = :type"
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

		//Getters and setters omitted for brevity
	//end::pc-filter-Account-example[]

		@ManyToOne(fetch = FetchType.LAZY)
		private Account parentAccount;

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

		public Account getParentAccount() {
			return parentAccount;
		}

		public Account setParentAccount(Account parentAccount) {
			this.parentAccount = parentAccount;
			return this;
		}
	//tag::pc-filter-Account-example[]
	}
	//end::pc-filter-Account-example[]


	@Entity(name = "AutoFilteredAccount")
	@Table(name = "autofilteredaccount")
	//tag::pc-filter-auto-enabled-Account-example[]
	@FilterDef(
			name="activeAccount",
			parameters = @ParamDef(
					name="active",
					type=Boolean.class
			),
			autoEnabled = true
	)
	//end::pc-filter-auto-enabled-Account-example[]
	@Filter(
			name="activeAccount",
			condition="active_status = :active"
	)
	//tag::pc-filter-resolver-Account-example[]
	@FilterDef(
			name="activeAccountWithResolver",
			parameters = @ParamDef(
					name="active",
					type=Boolean.class,
					resolver = AccountIsActiveResolver.class
			),
			autoEnabled = true
	)
	//end::pc-filter-resolver-Account-example[]
	public static class AutoFilteredAccount {

		@Id
		private Long id;

		@Column(name = "active_status")
		private boolean active;

		public Long getId() {
			return id;
		}

		public AutoFilteredAccount setId(Long id) {
			this.id = id;
			return this;
		}

		public boolean isActive() {
			return active;
		}

		public AutoFilteredAccount setActive(boolean active) {
			this.active = active;
			return this;
		}
	}

	//tag::pc-filter-resolver-Account-example[]

	public static class AccountIsActiveResolver implements Supplier<Boolean> {
		@Override
		public Boolean get() {
			return true;
		}
	}
	//end::pc-filter-resolver-Account-example[]

	@Entity(name = "AccountEager")
	@Table(name = "account_eager")
	public static class AccountEager {

		@Id
		private Long id;

		@ManyToOne
		private Account parentAccount;

		public Long getId() {
			return id;
		}

		public AccountEager setId(Long id) {
			this.id = id;
			return this;
		}

		public Account getParentAccount() {
			return parentAccount;
		}

		public AccountEager setParentAccount(Account parentAccount) {
			this.parentAccount = parentAccount;
			return this;
		}
	}

	@Entity(name = "AccountNotFound")
	@Table(name = "account_not_found")
	public static class AccountNotFound {

		@Id
		private Long id;

		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		private Account parentAccount;

		public Long getId() {
			return id;
		}

		public AccountNotFound setId(Long id) {
			this.id = id;
			return this;
		}

		public Account getParentAccount() {
			return parentAccount;
		}

		public AccountNotFound setParentAccount(Account parentAccount) {
			this.parentAccount = parentAccount;
			return this;
		}
	}

	@Entity(name = "AccountNotFoundException")
	@Table(name = "account_not_found_exception")
	public static class AccountNotFoundException {

		@Id
		private Long id;

		@ManyToOne
		@NotFound
		private Account parentAccount;

		public Long getId() {
			return id;
		}

		public AccountNotFoundException setId(Long id) {
			this.id = id;
			return this;
		}

		public Account getParentAccount() {
			return parentAccount;
		}

		public AccountNotFoundException setParentAccount(Account parentAccount) {
			this.parentAccount = parentAccount;
			return this;
		}
	}
}
