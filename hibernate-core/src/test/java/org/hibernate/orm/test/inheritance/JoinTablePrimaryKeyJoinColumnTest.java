/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		JoinTablePrimaryKeyJoinColumnTest.DebitAccount.class,
		JoinTablePrimaryKeyJoinColumnTest.CreditAccount.class,
})
@SessionFactory
public class JoinTablePrimaryKeyJoinColumnTest {
	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
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

			entityManager.persist(debitAccount);
			entityManager.persist(creditAccount);
		});

		factoryScope.inTransaction( entityManager -> {
			//noinspection deprecation
			entityManager.createQuery("select a from Account a").getResultList();
		});
	}

	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	//tag::entity-inheritance-joined-table-primary-key-join-column-example[]
	@Entity(name = "Account")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Account {

		@Id
		private Long id;

		private String owner;

		private BigDecimal balance;

		private BigDecimal interestRate;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-joined-table-primary-key-join-column-example[]

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
	//tag::entity-inheritance-joined-table-primary-key-join-column-example[]
	}

	@Entity(name = "DebitAccount")
	@PrimaryKeyJoinColumn(name = "account_id")
	public static class DebitAccount extends Account {

		private BigDecimal overdraftFee;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-joined-table-primary-key-join-column-example[]

		public BigDecimal getOverdraftFee() {
			return overdraftFee;
		}

		public void setOverdraftFee(BigDecimal overdraftFee) {
			this.overdraftFee = overdraftFee;
		}
	//tag::entity-inheritance-joined-table-primary-key-join-column-example[]
	}

	@Entity(name = "CreditAccount")
	@PrimaryKeyJoinColumn(name = "account_id")
	public static class CreditAccount extends Account {

		private BigDecimal creditLimit;

		//Getters and setters are omitted for brevity

	//end::entity-inheritance-joined-table-primary-key-join-column-example[]

		public BigDecimal getCreditLimit() {
			return creditLimit;
		}

		public void setCreditLimit(BigDecimal creditLimit) {
			this.creditLimit = creditLimit;
		}
	//tag::entity-inheritance-joined-table-primary-key-join-column-example[]
	}
	//end::entity-inheritance-joined-table-primary-key-join-column-example[]
}
