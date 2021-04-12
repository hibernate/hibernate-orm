/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-14549")
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
public class LoadUninitializedCollectionTest extends BaseEntityManagerFunctionalTestCase {

	boolean skipTest;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Bank.class,
				BankAccount.class,
				BankDepartment.class
		};
	}

	@Override
	protected void addMappings(Map settings) {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		if ( byteCodeProvider != null && !Environment.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals( byteCodeProvider ) ) {
			// skip the test if the bytecode provider is Javassist
			skipTest = true;
		}
	}


	@Before
	public void setUp() {
		if ( skipTest ) {
			return;
		}
		doInJPA(
				this::entityManagerFactory, entityManager -> {
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
	public void testLoadAfterNativeQueryExecution() {
		if ( skipTest ) {
			return;
		}
		doInJPA( this::entityManagerFactory, entityManager -> {
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
	public void testLoadAfterFlush() {
		if ( skipTest ) {
			return;
		}
		doInJPA( this::entityManagerFactory, entityManager -> {
					 BankAccount account = entityManager.find( BankAccount.class, 1L );

					 entityManager.flush();

					 Bank bank = account.getBank();
					 List<BankDepartment> deps = bank.getDepartments();

					 assertEquals( deps.size(), 3 );
				 }
		);
	}

	@After
	public void tearDown() {
		if ( skipTest ) {
			return;
		}
		doInJPA( this::entityManagerFactory, entityManager -> {
					 Bank bank = entityManager.find( Bank.class, 1L );
					 bank.getDepartments().forEach(
							 department -> entityManager.remove( department )
					 );
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
