/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;


import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Query;
import jakarta.persistence.Table;

import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@JiraKey("HHH-14549")
@DomainModel(
		annotatedClasses = {
				LoadUninitializedCollectionTest.Bank.class,
				LoadUninitializedCollectionTest.BankAccount.class,
				LoadUninitializedCollectionTest.BankDepartment.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
public class LoadUninitializedCollectionTest {

	@BeforeAll
	static void beforeAll() {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		assumeFalse( byteCodeProvider != null && !BytecodeProviderInitiator.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals(
				byteCodeProvider ) );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					Bank bank = new Bank( 1L, "International" );
					BankAccount bankAccount = new BankAccount( 1L, bank, "1234567890" );
					BankDepartment bankDepartmentA = new BankDepartment( 1L, "A" );
					BankDepartment bankDepartmentB = new BankDepartment( 2L, "B" );
					BankDepartment bankDepartmentC = new BankDepartment( 3L, "C" );

					bank.addDepartment( bankDepartmentA );
					bank.addDepartment( bankDepartmentB );
					bank.addDepartment( bankDepartmentC );

					entityManager.persist( bank );
					entityManager.persist( bankAccount );
					entityManager.persist( bankDepartmentA );
					entityManager.persist( bankDepartmentB );
					entityManager.persist( bankDepartmentC );
				}
		);
	}

	@Test
	public void testLoadAfterNativeQueryExecution(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					 BankAccount account = entityManager.find( BankAccount.class, 1L );

					 Query nativeQuery = entityManager.createNativeQuery( "SELECT ID FROM BANK" );
					 nativeQuery.getResultList();

					 Bank bank = account.getBank();
					 List<BankDepartment> deps = bank.getDepartments();

					 assertEquals( deps.size(), 3 );
				 }
		);
	}

	@Test
	public void testLoadAfterFlush(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					 BankAccount account = entityManager.find( BankAccount.class, 1L );

					 entityManager.flush();

					 Bank bank = account.getBank();
					 List<BankDepartment> deps = bank.getDepartments();

					 assertEquals( deps.size(), 3 );
				 }
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					 Bank bank = entityManager.find( Bank.class, 1L );
					 bank.getDepartments().forEach(
							 department -> entityManager.remove( department )
					 );
					 bank.getDepartments().clear();
					 List<BankAccount> accounts = entityManager.createQuery( "from BankAccount" ).getResultList();

					 accounts.forEach(
							 account -> entityManager.remove( account )
					 );

					 entityManager.remove( bank );
				 }
		);
	}


	@Entity(name = "Bank")
	@Table(name = "BANK")
	public static class Bank {

		@Id
		@Column(name = "ID")
		private Long id;

		private String name;

		public Bank() {
		}

		public Bank(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@OneToMany
		private List<BankDepartment> departments = new ArrayList<>();

		public List<BankDepartment> getDepartments() {
			return departments;
		}

		public void addDepartment(BankDepartment department) {
			this.departments.add( department );
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "BankAccount")
	public static class BankAccount {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Bank bank;

		private String serialNumber;

		public BankAccount() {
		}

		public BankAccount(Long id, Bank bank, String serialNumber) {
			this.id = id;
			this.bank = bank;
			this.serialNumber = serialNumber;
		}

		public Bank getBank() {
			return bank;
		}

		public String getSerialNumber() {
			return serialNumber;
		}
	}

	@Entity(name = "BankDepartment")
	public static class BankDepartment {

		@Id
		private Long id;

		private String name;

		public BankDepartment() {
		}

		public BankDepartment(Long id, String name) {
			this.id = id;
		}
	}


}
