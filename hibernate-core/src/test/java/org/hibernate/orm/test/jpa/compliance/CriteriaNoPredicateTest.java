/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				CriteriaNoPredicateTest.Person.class,
				CriteriaNoPredicateTest.Account.class,
				CriteriaNoPredicateTest.Deposit.class
		},
		properties = {
		@Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "true"),
}
)
public class CriteriaNoPredicateTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Deposit deposit = new Deposit( 1, new BigDecimal( 100 ) );
					final Deposit deposit2 = new Deposit( 2, new BigDecimal( 100 ) );

					final Collection<Deposit> deposits = new ArrayList<>();
					deposits.add( deposit );
					deposits.add( deposit2 );


					final Account account = new Account( 1, "abc", deposits );
					final Collection<Account> accounts = new ArrayList<>();
					accounts.add( account );

					final Person luigi = new Person( 1, "Luigi", 20, accounts );
					final Person fab = new Person( 2, "Fab", 20, null );
					entityManager.persist( deposit );
					entityManager.persist( deposit2 );
					entityManager.persist( account );
					entityManager.persist( luigi );
					entityManager.persist( fab );
				}
		);
	}

	@Test
	public void nullPredicateTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<Person> query = criteriaBuilder.createQuery(Person.class);
					query.from(Person.class);
					query.where();
					query.orderBy();
					List<Person> persons = entityManager.createQuery(query).getResultList();
					assertEquals(2, persons.size());

					entityManager.getTransaction().begin();
					final CriteriaDelete<Person> delete = criteriaBuilder.createCriteriaDelete(Person.class);
					query.from(Person.class);
					query.where();
					entityManager.createQuery(delete).executeUpdate();
					entityManager.getTransaction().commit();
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {
		@Id
		private Integer id;

		private String name;

		private Integer age;

		@OneToMany
		private Collection<Account> accounts;

		Person() {
		}

		public Person(Integer id, String name, Integer age, Collection<Account> accounts) {
			this.id = id;
			this.name = name;
			this.age = age;
			this.accounts = accounts;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Integer getAge() {
			return age;
		}

		public Collection<Account> getAccounts() {
			return accounts;
		}
	}

	@Entity(name = "Account")
	@Table(name = "ACCOUNT_TABLE")
	public static class Account {
		@Id
		private Integer id;

		private String code;

		@OneToMany
		private Collection<Deposit> deposits;

		public Account() {
		}

		public Account(
				Integer id,
				String code,
				Collection<Deposit> deposits) {
			this.id = id;
			this.code = code;
			this.deposits = deposits;
		}
	}

	@Entity(name = "Deposit")
	@Table(name = "DEPOSIT_TABLE")
	public static class Deposit {
		@Id
		private Integer id;

		private BigDecimal amount;

		public Deposit() {
		}

		public Deposit(Integer id, BigDecimal amount) {
			this.id = id;
			this.amount = amount;
		}
	}

}
