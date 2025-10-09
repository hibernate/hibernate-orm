/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = {
		DiscriminatorNotNullSingleTableTest.DebitAccount.class,
		DiscriminatorNotNullSingleTableTest.CreditAccount.class,
		DiscriminatorNotNullSingleTableTest.OtherAccount.class
})
@SessionFactory
public class DiscriminatorNotNullSingleTableTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::entity-inheritance-single-table-discriminator-value-persist-example[]
			DebitAccount debitAccount = new DebitAccount();
			debitAccount.setId(1L);
			debitAccount.setOwner("John Doe");
			debitAccount.setBalance(BigDecimal.valueOf(100));
			debitAccount.setInterestRate(BigDecimal.valueOf(1.5d));
			debitAccount.setOverdraftFee(BigDecimal.valueOf(25));

			CreditAccount creditAccount = new CreditAccount();
			creditAccount.setId(2L);
			creditAccount.setOwner("John Doe");
			creditAccount.setBalance(BigDecimal.valueOf(1000));
			creditAccount.setInterestRate(BigDecimal.valueOf(1.9d));
			creditAccount.setCreditLimit(BigDecimal.valueOf(5000));

			Account account = new Account();
			account.setId(3L);
			account.setOwner("John Doe");
			account.setBalance(BigDecimal.valueOf(1000));
			account.setInterestRate(BigDecimal.valueOf(1.9d));

			entityManager.persist(debitAccount);
			entityManager.persist(creditAccount);
			entityManager.persist(account);

			entityManager.unwrap(Session.class).doWork(connection -> {
				try(Statement statement = connection.createStatement()) {
					statement.executeUpdate(
						"insert into Account (DTYPE, active, balance, interestRate, owner, id) " +
						"values ('Other', true, 25, 0.5, 'Vlad', 4)"
					);
				}
			});
			//end::entity-inheritance-single-table-discriminator-value-persist-example[]
		});

		factoryScope.inTransaction( entityManager -> {
			//tag::entity-inheritance-single-table-discriminator-value-persist-example[]

			Map<Long, Account> accounts = entityManager.createQuery(
				"select a from Account a", Account.class)
			.getResultList()
			.stream()
			.collect(Collectors.toMap(Account::getId, Function.identity()));

			Assertions.assertEquals( 4, accounts.size() );
			Assertions.assertEquals( DebitAccount.class, accounts.get(1L).getClass() );
			Assertions.assertEquals( CreditAccount.class, accounts.get(2L).getClass() );
			Assertions.assertEquals( Account.class, accounts.get(3L).getClass() );
			Assertions.assertEquals( OtherAccount.class, accounts.get(4L).getClass() );
			//end::entity-inheritance-single-table-discriminator-value-persist-example[]
		});
	}

	//tag::entity-inheritance-single-table-discriminator-value-example[]
	@Entity(name = "Account")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorValue("null")
	public static class Account {

		@Id
		private Long id;

		private String owner;

		private BigDecimal balance;

		private BigDecimal interestRate;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-single-table-discriminator-value-example[]

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
	//tag::entity-inheritance-single-table-discriminator-value-example[]
	}

	@Entity(name = "DebitAccount")
	@DiscriminatorValue("Debit")
	public static class DebitAccount extends Account {

		private BigDecimal overdraftFee;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-single-table-discriminator-value-example[]

		public BigDecimal getOverdraftFee() {
			return overdraftFee;
		}

		public void setOverdraftFee(BigDecimal overdraftFee) {
			this.overdraftFee = overdraftFee;
		}
	//tag::entity-inheritance-single-table-discriminator-value-example[]
	}

	@Entity(name = "CreditAccount")
	@DiscriminatorValue("Credit")
	public static class CreditAccount extends Account {

		private BigDecimal creditLimit;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-single-table-discriminator-value-example[]

		public BigDecimal getCreditLimit() {
			return creditLimit;
		}

		public void setCreditLimit(BigDecimal creditLimit) {
			this.creditLimit = creditLimit;
		}
	//tag::entity-inheritance-single-table-discriminator-value-example[]
	}

	@Entity(name = "OtherAccount")
	@DiscriminatorValue("not null")
	public static class OtherAccount extends Account {

		private boolean active;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-single-table-discriminator-value-example[]

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}
	//tag::entity-inheritance-single-table-discriminator-value-example[]
	}
	//end::entity-inheritance-single-table-discriminator-value-example[]
}
