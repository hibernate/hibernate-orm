package org.hibernate.orm.test.loading;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@DomainModel(
		annotatedClasses = {
				LoadEagerToManyMaxFetchDepth2Test.LegalEntity.class,
				LoadEagerToManyMaxFetchDepth2Test.BankAccount.class,
				LoadEagerToManyMaxFetchDepth2Test.Ownership.class,
				LoadEagerToManyMaxFetchDepth2Test.Activation.class,
		}
)
@SessionFactory
@ServiceRegistry(
		// A value of exactly 2 seems necessary to reproduce the failure.
		// Neither 1 nor 3 will reproduce it.
		// Incidentally, max_fetch_depth defaults to 2 on MySQL...
		settings = @Setting(name = "hibernate.max_fetch_depth", value = "2")
)
@TestForIssue(jiraKey = "HHH-16199")
public class LoadEagerToManyMaxFetchDepth2Test {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var le = new LegalEntity();
			session.persist( le );
			var bankAccount = new BankAccount( null, le );
			session.persist( bankAccount );
			var ownership = new Ownership( null, le );
			session.persist( ownership );
			Set<BankAccount> bankAccounts = new HashSet<>();
			bankAccounts.add( bankAccount );
			le.setBankAccounts( bankAccounts );
			le.setOwnership( ownership );
			var activation = new Activation( null, le, bankAccount );
			session.persist( activation );
			session.flush();
			session.clear();
			assertNotNull( session.find( Activation.class, activation.getId() ) );
		} );
	}

	@Entity
	public static class Activation {
		@Id
		@GeneratedValue
		Long id;

		@ManyToOne
		LegalEntity legalEntity;

		@ManyToOne
		BankAccount bankAccount;

		public Activation() {
		}

		public Activation(Long id, LegalEntity legalEntity, BankAccount bankAccount) {
			this.id = id;
			this.legalEntity = legalEntity;
			this.bankAccount = bankAccount;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LegalEntity getLegalEntity() {
			return legalEntity;
		}

		public void setLegalEntity(LegalEntity legalEntity) {
			this.legalEntity = legalEntity;
		}

		public BankAccount getBankAccount() {
			return bankAccount;
		}

		public void setBankAccount(BankAccount bankAccount) {
			this.bankAccount = bankAccount;
		}
	}

	@Entity
	public static class BankAccount {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private LegalEntity legalEntity;

		public BankAccount() {
		}

		public BankAccount(Long id, LegalEntity legalEntity) {
			this.id = id;
			this.legalEntity = legalEntity;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LegalEntity getLegalEntity() {
			return legalEntity;
		}

		public void setLegalEntity(LegalEntity legalEntity) {
			this.legalEntity = legalEntity;
		}
	}

	@Entity
	public static class LegalEntity {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne(mappedBy = "legalEntity")
		private Ownership ownership;

		@OneToMany(mappedBy = "legalEntity", fetch = FetchType.EAGER)
		private Set<BankAccount> bankAccounts = new HashSet<>();

		public LegalEntity() {
		}

		public LegalEntity(Long id, Ownership ownership, Set<BankAccount> bankAccounts) {
			this.id = id;
			this.ownership = ownership;
			this.bankAccounts = bankAccounts;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Ownership getOwnership() {
			return ownership;
		}

		public void setOwnership(Ownership ownership) {
			this.ownership = ownership;
		}

		public Set<BankAccount> getBankAccounts() {
			return bankAccounts;
		}

		public void setBankAccounts(Set<BankAccount> bankAccounts) {
			this.bankAccounts = bankAccounts;
		}
	}

	@Entity
	public static class Ownership {
		@Id
		@GeneratedValue
		private Long id;
		@OneToOne
		private LegalEntity legalEntity;

		public Ownership() {
		}

		public Ownership(Long id, LegalEntity legalEntity) {
			this.id = id;
			this.legalEntity = legalEntity;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LegalEntity getLegalEntity() {
			return legalEntity;
		}

		public void setLegalEntity(LegalEntity legalEntity) {
			this.legalEntity = legalEntity;
		}
	}
}
