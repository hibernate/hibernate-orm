package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.List;

import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.DirtyCheckEnhancementContext;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-17049")
@DomainModel(
		annotatedClasses = {
				JpaConstructorInitializationTest.Person.class,
				JpaConstructorInitializationTest.LoginAccount.class,
				JpaConstructorInitializationTest.AccountPreferences.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ NoDirtyCheckingContext.class, DirtyCheckEnhancementContext.class })
public class JpaConstructorInitializationTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
					 Person person = new Person( 1l, "Henry" );
					 LoginAccount loginAccount = new LoginAccount();
					 loginAccount.setOwner( person );
					 person.setLoginAccount( loginAccount );
					 em.persist( person );
				 }
		);

		scope.inTransaction( em -> {
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

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
					 em.createQuery( "delete from Person" ).executeUpdate();
					 em.createQuery( "delete from LoginAccount" ).executeUpdate();
					 em.createQuery( "delete from AccountPreferences" ).executeUpdate();
				 }
		);
	}

	@Test
	public void findTest(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
					 em.clear();
					 Person person = em.find( Person.class, 1L );
					 person.setFirstName( "Liza" );
				 }
		);

		scope.inTransaction( em -> {
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
	public void getReferenceTest(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
					 em.clear();
					 Person person = em.getReference( Person.class, 1L );
					 person.setFirstName( "Liza" );
				 }
		);

		scope.inTransaction( em -> {
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
	public void findTest2(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
					 em.clear();
					 Person person = em.find( Person.class, 1L );
					 person.setFirstName( "Liza" );

					 LoginAccount loginAccount = person.getLoginAccount();
					 loginAccount.setName( "abc" );
				 }
		);

		scope.inTransaction( em -> {
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
	public void getReferenceTest2(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
					 em.clear();
					 Person person = em.getReference( Person.class, 1L );
					 person.setFirstName( "Liza" );

					 LoginAccount loginAccount = person.getLoginAccount();
					 loginAccount.setName( "abc" );
				 }
		);

		scope.inTransaction( em -> {
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
	public static class AccountPreferences {
		@Id
		@GeneratedValue
		private long id;

		@Column(name = "open_col")
		private boolean open = false;
	}

}
