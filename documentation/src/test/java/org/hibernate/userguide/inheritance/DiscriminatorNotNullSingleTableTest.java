/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.inheritance;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( H2Dialect.class )
public class DiscriminatorNotNullSingleTableTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DebitAccount.class,
				CreditAccount.class,
				OtherAccount.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).doWork( connection -> {
				try(Statement statement = connection.createStatement()) {
					statement.executeUpdate( "ALTER TABLE Account ALTER COLUMN DTYPE SET NULL" );
				}
			} );

			//tag::entity-inheritance-single-table-discriminator-value-persist-example[]
			DebitAccount debitAccount = new DebitAccount();
			debitAccount.setId( 1L );
			debitAccount.setOwner( "John Doe" );
			debitAccount.setBalance( BigDecimal.valueOf( 100 ) );
			debitAccount.setInterestRate( BigDecimal.valueOf( 1.5d ) );
			debitAccount.setOverdraftFee( BigDecimal.valueOf( 25 ) );

			CreditAccount creditAccount = new CreditAccount();
			creditAccount.setId( 2L );
			creditAccount.setOwner( "John Doe" );
			creditAccount.setBalance( BigDecimal.valueOf( 1000 ) );
			creditAccount.setInterestRate( BigDecimal.valueOf( 1.9d ) );
			creditAccount.setCreditLimit( BigDecimal.valueOf( 5000 ) );

			Account account = new Account();
			account.setId( 3L );
			account.setOwner( "John Doe" );
			account.setBalance( BigDecimal.valueOf( 1000 ) );
			account.setInterestRate( BigDecimal.valueOf( 1.9d ) );

			entityManager.persist( debitAccount );
			entityManager.persist( creditAccount );
			entityManager.persist( account );

			entityManager.unwrap( Session.class ).doWork( connection -> {
				try(Statement statement = connection.createStatement()) {
					statement.executeUpdate(
						"insert into Account (DTYPE, active, balance, interestRate, owner, id) " +
						"values ('Other', true, 25, 0.5, 'Vlad', 4)"
					);
				}
			} );
			//end::entity-inheritance-single-table-discriminator-value-persist-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::entity-inheritance-single-table-discriminator-value-persist-example[]

			Map<Long, Account> accounts = entityManager.createQuery(
				"select a from Account a", Account.class )
			.getResultList()
			.stream()
			.collect( Collectors.toMap( Account::getId, Function.identity()));

			assertEquals(4, accounts.size());
			assertEquals( DebitAccount.class, accounts.get( 1L ).getClass() );
			assertEquals( CreditAccount.class, accounts.get( 2L ).getClass() );
			assertEquals( Account.class, accounts.get( 3L ).getClass() );
			assertEquals( OtherAccount.class, accounts.get( 4L ).getClass() );
			//end::entity-inheritance-single-table-discriminator-value-persist-example[]
		} );
	}

	//tag::entity-inheritance-single-table-discriminator-value-example[]
	@Entity(name = "Account")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorValue( "null" )
	public static class Account {

		@Id
		private Long id;

		private String owner;

		private BigDecimal balance;

		private BigDecimal interestRate;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getOwner() {
			return owner;
		}

		public void setOwner(String owner) {
			this.owner = owner;
		}

		public BigDecimal getBalance() {
			return balance;
		}

		public void setBalance(BigDecimal balance) {
			this.balance = balance;
		}

		public BigDecimal getInterestRate() {
			return interestRate;
		}

		public void setInterestRate(BigDecimal interestRate) {
			this.interestRate = interestRate;
		}
	}

	@Entity(name = "DebitAccount")
	@DiscriminatorValue( "Debit" )
	public static class DebitAccount extends Account {

		private BigDecimal overdraftFee;

		public BigDecimal getOverdraftFee() {
			return overdraftFee;
		}

		public void setOverdraftFee(BigDecimal overdraftFee) {
			this.overdraftFee = overdraftFee;
		}
	}

	@Entity(name = "CreditAccount")
	@DiscriminatorValue( "Credit" )
	public static class CreditAccount extends Account {

		private BigDecimal creditLimit;

		public BigDecimal getCreditLimit() {
			return creditLimit;
		}

		public void setCreditLimit(BigDecimal creditLimit) {
			this.creditLimit = creditLimit;
		}
	}

	@Entity(name = "OtherAccount")
	@DiscriminatorValue( "not null" )
	public static class OtherAccount extends Account {

		private boolean active;

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}
	}
	//end::entity-inheritance-single-table-discriminator-value-example[]
}
