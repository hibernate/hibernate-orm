/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Jpa(
		annotatedClasses = {
				CriteriaIsNullTest.Account.class,
				CriteriaIsNullTest.Person.class
		}
)
public class CriteriaIsNullTest {

	@Test
	public void testIsNullInWhereClause(EntityManagerFactoryScope scope) {


		Account account1 = new Account( 1, null, null );
		Account account2 = new Account( 2, "Fab", null );

		Person person1 = new Person( 1, "Luigi", account1 );
		Person person2 = new Person( 2, "Andrea", account2 );
		Person person3 = new Person( 3, "Andrea", null );

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( account1 );
					entityManager.persist( account2 );
					entityManager.persist( person1 );
					entityManager.persist( person2 );
					entityManager.persist( person3 );
				}
		);

		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
					final Root<Person> person = query.from( Person.class );
					query.where( person.get( "account" ).get( "code" ).isNull() );
					query.select( person.get( "id" ) );
					final List<Integer> people = entityManager.createQuery( query ).getResultList();

					assertEquals( 1, people.size() );
					assertEquals( person1.getId(), people.get( 0 ) );
				}
		);

	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		@OneToOne
		private Account account;

		Person() {
		}

		public Person(Integer id, String name, Account account) {
			this.id = id;
			this.name = name;
			this.account = account;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Account getAccount() {
			return account;
		}
	}

	@Entity(name = "Account")
	@Table(name = "ACCOUNT_TABLE")
	public static class Account {
		@Id
		private Integer id;

		private String code;

		private Double amount;

		public Account() {
		}

		public Account(
				Integer id,
				String code,
				Double amount) {
			this.id = id;
			this.code = code;
			this.amount = amount;
		}

	}
}
