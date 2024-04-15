/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.notfound.ignore;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class IsNullAndNotFoundTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Account.class, Person.class };
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Account account1 = new Account( 1, null, null );
					Account account2 = new Account( 2, "Fab", null );

					Person person1 = new Person( 1, "Luigi", account1 );
					Person person2 = new Person( 2, "Andrea", account2 );
					Person person3 = new Person( 3, "Max", null );

					session.persist( account1 );
					session.persist( account2 );
					session.persist( person1 );
					session.persist( person2 );
					session.persist( person3 );
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Person" ).executeUpdate();
					session.createQuery( "delete from Account" ).executeUpdate();
				}
		);
	}

	@Test
	public void testIsNullInWhereClause() {
		inTransaction(
				session -> {
					final List<Integer> ids = session.createQuery(
							"select p.id from Person p where p.account.code is null" ).getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( 1, (int) ids.get( 0 ) );

				}
		);
	}

	@Test
	public void testIsNullInWhereClause2() {
		inTransaction(
				session -> {
					final List<Integer> ids = session.createQuery(
							"select distinct p.id from Person p where p.account is null" ).getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( 3, (int) ids.get( 0 ) );

				}
		);
	}

	@Test
	public void testIsNullInWhereClause3() {
		inTransaction(
				session -> {
					final List<Integer> ids = session.createQuery(
							"select distinct p.id from Person p where p.account is null" ).getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( 3, (int) ids.get( 0 ) );

				}
		);
	}

	@Test
	public void testIsNullInWhereClause4() {
		inTransaction(
				session -> {
					final List<Integer> ids = session.createQuery(
									"select p.id from Person p where p.account.code is null or p.account.id is null" )
							.getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( 1, (int) ids.get( 0 ) );

				}
		);
	}

	@Test
	public void testWhereClause() {
		inTransaction(
				session -> {
					final List<Integer> ids = session.createQuery(
									"select p.id from Person p where p.account.code = :code and p.account.id = :id" )
							.setParameter( "code", "Fab" )
							.setParameter( "id", 2 )
							.getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( 2, (int) ids.get( 0 ) );

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

		public Account(Integer id, String code, Double amount) {
			this.id = id;
			this.code = code;
			this.amount = amount;
		}

	}
}
