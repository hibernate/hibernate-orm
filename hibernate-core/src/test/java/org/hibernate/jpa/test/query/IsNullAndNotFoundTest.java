/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.query;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class IsNullAndNotFoundTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Account.class, Person.class };
	}

	@Test
	public void testIsNullInWhereClause() {


		Account account1 = new Account( 1, null, null );
		Account account2 = new Account( 2, "Fab", null );

		Person person1 = new Person( 1, "Luigi", account1 );
		Person person2 = new Person( 2, "Andrea", account2 );
		Person person3 = new Person( 3, "Andrea", null );

		inTransaction(
				session -> {
					session.persist( account1 );
					session.persist( account2 );
					session.persist( person1 );
					session.persist( person2 );
					session.persist( person3 );
				}
		);

		inTransaction(
				session -> {
					final List<Integer> people = session.createQuery(
							"select p.id from Person p where p.account.code is null" ).getResultList();

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
		@NotFound(action = NotFoundAction.IGNORE)
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
