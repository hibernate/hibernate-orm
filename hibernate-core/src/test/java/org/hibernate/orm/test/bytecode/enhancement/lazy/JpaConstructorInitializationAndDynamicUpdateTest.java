package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.List;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.DirtyCheckEnhancementContext;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@JiraKey("HHH-17049")
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ NoDirtyCheckingContext.class, DirtyCheckEnhancementContext.class })
public class JpaConstructorInitializationAndDynamicUpdateTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				LoginAccount.class,
				AccountPreferences.class
		};
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, em -> {
					 Person person = new Person( 1l, "Henry" );
					 LoginAccount loginAccount = new LoginAccount();
					 loginAccount.setOwner( person );
					 person.setLoginAccount( loginAccount );
					 em.persist( person );
				 }
		);

		doInJPA( this::entityManagerFactory, em -> {
					 List<LoginAccount> accounts = em.createQuery(
							 "select la from LoginAccount la",
							 LoginAccount.class
					 ).getResultList();
					 assertThat( accounts.size() ).isEqualTo( 1 );

					 List<AccountPreferences> preferences = em.createQuery(
							 "select ap from AccountPreferences ap",
							 AccountPreferences.class
					 ).getResultList();
					 assertThat( preferences.size() ).isEqualTo( 1 );
				 }
		);
	}

	@After
	public void tearDown() {
		doInJPA( this::entityManagerFactory, em -> {
					 em.createQuery( "delete from Person" ).executeUpdate();
					 em.createQuery( "delete from LoginAccount" ).executeUpdate();
					 em.createQuery( "delete from AccountPreferences" ).executeUpdate();
				 }
		);
	}

	@Test
	public void findTest() {
		doInJPA( this::entityManagerFactory, em -> {
					 em.clear();
					 Person person = em.find( Person.class, 1L );
					 person.setFirstName( "Liza" );
				 }
		);

		doInJPA( this::entityManagerFactory, em -> {
					 List<LoginAccount> accounts = em.createQuery(
							 "select la from LoginAccount la",
							 LoginAccount.class
					 ).getResultList();
					 assertThat( accounts.size() ).isEqualTo( 1 );

					 List<AccountPreferences> preferences = em.createQuery(
							 "select ap from AccountPreferences ap",
							 AccountPreferences.class
					 ).getResultList();
					 assertThat( preferences.size() ).isEqualTo( 1 );
				 }
		);
	}

	@Test
	public void getReferenceTest() {
		doInJPA( this::entityManagerFactory, em -> {
					 em.clear();
					 Person person = em.getReference( Person.class, 1L );
					 person.setFirstName( "Liza" );
				 }
		);

		doInJPA( this::entityManagerFactory, em -> {
					 List<LoginAccount> accounts = em.createQuery(
							 "select la from LoginAccount la",
							 LoginAccount.class
					 ).getResultList();
					 assertThat( accounts.size() ).isEqualTo( 1 );

					 List<AccountPreferences> preferences = em.createQuery(
							 "select ap from AccountPreferences ap",
							 AccountPreferences.class
					 ).getResultList();
					 assertThat( preferences.size() ).isEqualTo( 1 );
				 }
		);
	}

	@Test
	public void findTest2() {
		doInJPA( this::entityManagerFactory, em -> {
					 em.clear();
					 Person person = em.find( Person.class, 1L );
					 person.setFirstName( "Liza" );

					 LoginAccount loginAccount = person.getLoginAccount();
					 loginAccount.setName( "abc" );
				 }
		);

		doInJPA( this::entityManagerFactory, em -> {
					 Person person = em.find( Person.class, 1L );
					 assertThat( person.getFirstName() ).isEqualTo( "Liza" );

					 LoginAccount loginAccount = person.getLoginAccount();
					 assertThat( loginAccount ).isNotNull();
					 assertThat( loginAccount.getName() ).isEqualTo( "abc" );

					 List<LoginAccount> accounts = em.createQuery(
							 "select la from LoginAccount la",
							 LoginAccount.class
					 ).getResultList();
					 assertThat( accounts.size() ).isEqualTo( 1 );

					 List<AccountPreferences> preferences = em.createQuery(
							 "select ap from AccountPreferences ap",
							 AccountPreferences.class
					 ).getResultList();
					 assertThat( preferences.size() ).isEqualTo( 1 );
				 }
		);
	}

	@Test
	public void getReferenceTest2() {
		doInJPA( this::entityManagerFactory, em -> {
					 em.clear();
					 Person person = em.getReference( Person.class, 1L );
					 person.setFirstName( "Liza" );

					 LoginAccount loginAccount = person.getLoginAccount();
					 loginAccount.setName( "abc" );
				 }
		);

		doInJPA( this::entityManagerFactory, em -> {
					 Person person = em.find( Person.class, 1L );
					 assertThat( person.getFirstName() ).isEqualTo( "Liza" );

					 LoginAccount loginAccount = person.getLoginAccount();
					 assertThat( loginAccount ).isNotNull();
					 assertThat( loginAccount.getName() ).isEqualTo( "abc" );

					 List<LoginAccount> accounts = em.createQuery(
							 "select la from LoginAccount la",
							 LoginAccount.class
					 ).getResultList();
					 assertThat( accounts.size() ).isEqualTo( 1 );

					 List<AccountPreferences> preferences = em.createQuery(
							 "select ap from AccountPreferences ap",
							 AccountPreferences.class
					 ).getResultList();
					 assertThat( preferences.size() ).isEqualTo( 1 );
				 }
		);
	}

	@Entity(name = "Person")
	@DynamicUpdate
	public static class Person {
		@Id
		private long id;

		private String firstName;

		@OneToOne(orphanRemoval = true, cascade = { CascadeType.PERSIST, CascadeType.REMOVE }, fetch = FetchType.LAZY)
		private LoginAccount loginAccount = new LoginAccount();

		public Person() {
		}

		public Person(long id, String firstName) {
			this.id = id;
			this.firstName = firstName;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public LoginAccount getLoginAccount() {
			return loginAccount;
		}

		public void setLoginAccount(LoginAccount loginAccount) {
			this.loginAccount = loginAccount;
		}
	}

	@Entity(name = "LoginAccount")
	@DynamicUpdate
	public static class LoginAccount {
		@Id
		@GeneratedValue
		private long id;

		private String name;

		@OneToOne(orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private AccountPreferences accountPreferences = new AccountPreferences();

		@OneToOne(mappedBy = "loginAccount")
		private Person owner;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public AccountPreferences getAccountPreferences() {
			return accountPreferences;
		}

		public void setAccountPreferences(AccountPreferences accountPreferences) {
			this.accountPreferences = accountPreferences;
		}

		public Person getOwner() {
			return owner;
		}

		public void setOwner(Person owner) {
			this.owner = owner;
		}
	}

	@Entity(name = "AccountPreferences")
	@DynamicUpdate
	public static class AccountPreferences {
		@Id
		@GeneratedValue
		private long id;

		@Column(name = "open_col")
		private boolean open = false;
	}


}
