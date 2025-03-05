/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				CriteriaPredicateTest.Person.class,
				CriteriaPredicateTest.Account.class,
				CriteriaPredicateTest.Deposit.class
		},
		properties = {
		@Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "true"),
}
)
public class CriteriaPredicateTest {

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
	public void joinOnPredicateArrayTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Person> query = criteriaBuilder.createQuery( Person.class );
					final Root<Person> person = query.from( Person.class );
					final EntityType<Person> personEntity = entityManager.getMetamodel().entity( Person.class );
					final Join<Person, Account> accounts = person.join( personEntity.getCollection(
							"accounts",
							Account.class
					) );

					final EntityType<Account> accountEntity = entityManager.getMetamodel().entity( Account.class );
					final CollectionJoin<Account, Deposit> deposits = accounts
							.join( accountEntity.getCollection( "deposits", Deposit.class ), JoinType.INNER );

					Predicate pred = deposits.getOn();
					assertThat( pred, nullValue() );

					Predicate[] predArray = { criteriaBuilder.equal( deposits.get( "id" ), "1" ) };

					deposits.on( predArray );

					pred = deposits.getOn();
					assertThat( pred, notNullValue() );


					query.select( person );

					TypedQuery<Person> tquery = entityManager.createQuery( query );
					List<Person> persons = tquery.getResultList();

					assertEquals( 1, persons.size() );
					assertEquals( 1, persons.get( 0 ).getId() );
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
