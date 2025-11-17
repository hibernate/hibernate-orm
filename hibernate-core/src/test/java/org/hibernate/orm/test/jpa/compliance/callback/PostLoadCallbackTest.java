/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.callback;

import java.math.BigDecimal;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Query;

import static org.junit.jupiter.api.Assertions.fail;

@Jpa(
		annotatedClasses = {
				PostLoadCallbackTest.Account.class,
				PostLoadCallbackTest.DebitAccount.class,
				PostLoadCallbackTest.CreditAccount.class,
				PostLoadCallbackTest.Person.class
		}
)
public class PostLoadCallbackTest {

	@Test
	public void testEntityListenerPostLoadMethodIsCalled(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CreditAccount account = new CreditAccount(
							1,
							"drea",
							BigDecimal.valueOf( 190 ),
							BigDecimal.valueOf( 10000 )
					);

					entityManager.persist( account );
					entityManager.flush();
					entityManager.refresh( account );

					Query query = entityManager.createQuery( "select ca from CreditAccount ca" );
					query.getResultList();

					if ( !account.isPostLoadCalled() ) {
						fail( "PostLoad has not been called" );
					}
				}
		);
	}

	@Test
	public void testPostLoadMethodIsCalled(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person person = new Person( 1, "Fab" );
					entityManager.persist( person );
					entityManager.flush();
					entityManager.refresh( person );

					Query query = entityManager
							.createQuery( "select p from Person p" );
					query.getResultList();
					if ( person.isPostLoadCalled() ) {
						fail( "PostLoad has not been called" );
					}
				}
		);
	}

	@MappedSuperclass
	public static class PersonCallback {

		boolean isPrePersistCalled;
		boolean isPostPersistCalled;

		@PrePersist
		public void prePersist() {
			isPrePersistCalled = true;
		}

		@PostPersist
		public void postPersist() {
			if ( !isPrePersistCalled ) {
				throw new IllegalStateException(
						"The prePersist method has not been called." );
			}
			this.isPostPersistCalled = true;
		}

		public boolean isPostLoadCalled() {
			return isPostPersistCalled;
		}
	}

	@Entity(name = "Person")
	public static class Person extends PersonCallback {

		Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		private Integer id;

		private String name;
	}

	@Entity(name = "Account")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "ACCOUNT_TYPE", discriminatorType = DiscriminatorType.STRING)
	@DiscriminatorValue("Account")
	@EntityListeners(AccountListener.class)
	public static class Account extends CallbackStatus {
		@Id
		private Integer id;

		private String owner;

		private BigDecimal balance;

		public Account() {
		}

		public Account(Integer id, String owner, BigDecimal balance) {
			this.id = id;
			this.owner = owner;
			this.balance = balance;
		}

		public Integer getId() {
			return id;
		}

	}

	@Entity(name = "DebitAccount")
	@DiscriminatorValue("DebitAccount")
	@EntityListeners(AccountListener.class)
	public static class DebitAccount extends Account {
		private BigDecimal overdraftFee;

		public DebitAccount(Integer id, String owner, BigDecimal balance, BigDecimal overdraftFee) {
			super( id, owner, balance );
			this.overdraftFee = overdraftFee;
		}

	}

	@Entity(name = "CreditAccount")
	@DiscriminatorValue("CreditAccount")
	@EntityListeners(AccountListener.class)
	public static class CreditAccount extends Account {
		private BigDecimal creditLimit;

		public CreditAccount(Integer id, String owner, BigDecimal balance, BigDecimal creditLimit) {
			super( id, owner, balance );
			this.creditLimit = creditLimit;
		}
	}

	public static class AccountListener {

		@PostPersist
		public void postPersist(CallbackStatus callbackStatus) {
			callbackStatus.setPostPersistCall();
		}

		@PostLoad
		public void postLoad(CallbackStatus callbackStatus) {
			callbackStatus.setPostLoadCalled();
		}

	}

	public static class CallbackStatus {

		private boolean postPersistCalled;

		private boolean postLoadCalled;

		public boolean isPostLoadCalled() {
			return postLoadCalled;
		}

		public void setPostLoadCalled() {
			postLoadCalled = true;
		}

		public void setPostPersistCall() {
			postPersistCalled = true;
		}
	}

}
