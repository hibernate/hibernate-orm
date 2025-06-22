/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import java.math.BigDecimal;
import java.util.List;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				SingleTableDiscriminatorFormulaTest.DebitAccount.class,
				SingleTableDiscriminatorFormulaTest.CreditAccount.class,
		}
)
public class SingleTableDiscriminatorFormulaTest {

	@Test
	@RequiresDialect(value = PostgreSQLDialect.class)
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			DebitAccount debitAccount = new DebitAccount("123-debit");
			debitAccount.setId(1L);
			debitAccount.setOwner("John Doe");
			debitAccount.setBalance(BigDecimal.valueOf(100));
			debitAccount.setInterestRate(BigDecimal.valueOf(1.5d));
			debitAccount.setOverdraftFee(BigDecimal.valueOf(25));

			CreditAccount creditAccount = new CreditAccount("456-credit");
			creditAccount.setId(2L);
			creditAccount.setOwner("John Doe");
			creditAccount.setBalance(BigDecimal.valueOf(1000));
			creditAccount.setInterestRate(BigDecimal.valueOf(1.9d));
			creditAccount.setCreditLimit(BigDecimal.valueOf(5000));

			entityManager.persist(debitAccount);
			entityManager.persist(creditAccount);
		});

		scope.inTransaction(entityManager -> {
			List<Account> accounts =
					entityManager.createQuery("select a from Account a").getResultList();
			assertEquals(2, accounts.size());
		});
	}

	//tag::entity-inheritance-single-table-discriminator-formula-example[]
	@Entity(name = "Account")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorFormula(
		"case when debitKey is not null " +
		"then 'Debit' " +
		"else (" +
		"   case when creditKey is not null " +
		"   then 'Credit' " +
		"   else 'Unknown' " +
		"   end) " +
		"end "
	)
	public static class Account {

		@Id
		private Long id;

		private String owner;

		private BigDecimal balance;

		private BigDecimal interestRate;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-single-table-discriminator-formula-example[]

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
	//tag::entity-inheritance-single-table-discriminator-formula-example[]
	}

	@Entity(name = "DebitAccount")
	@DiscriminatorValue(value = "Debit")
	public static class DebitAccount extends Account {

		private String debitKey;

		private BigDecimal overdraftFee;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-single-table-discriminator-formula-example[]

		private DebitAccount() {
		}

		public DebitAccount(String debitKey) {
			this.debitKey = debitKey;
		}

		public String getDebitKey() {
			return debitKey;
		}

		public BigDecimal getOverdraftFee() {
			return overdraftFee;
		}

		public void setOverdraftFee(BigDecimal overdraftFee) {
			this.overdraftFee = overdraftFee;
		}
	//tag::entity-inheritance-single-table-discriminator-formula-example[]
	}

	@Entity(name = "CreditAccount")
	@DiscriminatorValue(value = "Credit")
	public static class CreditAccount extends Account {

		private String creditKey;

		private BigDecimal creditLimit;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-single-table-discriminator-formula-example[]

		private CreditAccount() {
		}

		public CreditAccount(String creditKey) {
			this.creditKey = creditKey;
		}

		public String getCreditKey() {
			return creditKey;
		}

		public BigDecimal getCreditLimit() {
			return creditLimit;
		}

		public void setCreditLimit(BigDecimal creditLimit) {
			this.creditLimit = creditLimit;
		}
	//tag::entity-inheritance-single-table-discriminator-formula-example[]
	}
	//end::entity-inheritance-single-table-discriminator-formula-example[]
}
