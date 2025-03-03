/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound;

import java.util.List;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;


public class IsNullAndNotFoundTest extends BaseNonConfigCoreFunctionalTestCase {
	private final SQLStatementInspector inspector = new SQLStatementInspector();

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.STATEMENT_INSPECTOR, inspector );
	}

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
					session.createMutationQuery( "delete from Person" ).executeUpdate();
					session.createMutationQuery( "delete from Account" ).executeUpdate();
				}
		);
	}

	@Test
	public void testAssociationDereferenceIsNullInWhereClause() {
		inTransaction(
				session -> {
					inspector.clear();

					// should produce an inner join to ACCOUNT_TABLE

					final List<Integer> ids = session.createQuery(
							"select p.id from Person p where p.account.code is null", Integer.class )
							.getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( 1, (int) ids.get( 0 ) );

					assertThat( inspector.getSqlQueries() ).hasSize( 1 );
					assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( " join " );
					assertThat( inspector.getSqlQueries().get( 0 ) ).doesNotContainIgnoringCase( " left " );
					assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( " ACCOUNT_TABLE " );
				}
		);
	}

	@Test
	public void testAssociationIsNullInWhereClause() {
		inTransaction(
				session -> {
					inspector.clear();

					// should produce a left join to ACCOUNT_TABLE and restrict based on the Account's id -
					//
					//	...
					//	from PERSON p
					//		left join ACCOUNT_TABLE a
					//			on p.account_id = a.id
					//	where a.id is null

					final List<Integer> ids = session.createQuery(
							"select distinct p.id from Person p where p.account is null", Integer.class )
							.getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( 3, (int) ids.get( 0 ) );

					assertThat( inspector.getSqlQueries() ).hasSize( 1 );
					assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( " left join " );
					assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( ".id is null" );
				}
		);
	}

	@Test
	public void testFetchedAssociationIsNullInWhereClause() {
		inTransaction(
				session -> {
					inspector.clear();

					// should produce an inner join to ACCOUNT_TABLE since it's explicitly selected
					//
					//	...
					//	from PERSON p
					//		join ACCOUNT_TABLE a
					//			on p.account_id = a.id
					//	where a.id is null

					final List<Account> results = session.createQuery(
									"select p.account from Person p where p.account is null", Account.class )
							.getResultList();

					assertThat( results ).isEmpty();

					assertThat( inspector.getSqlQueries() ).hasSize( 1 );
					assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( "join" );
					assertThat( inspector.getSqlQueries().get( 0 ) ).doesNotContainIgnoringCase( " left join " );
				}
		);
	}

	@Test
	public void testIsNullInWhereClause3() {
		inTransaction(
				session -> {
					inspector.clear();

					final List<Integer> ids = session.createQuery(
							"select distinct p.id from Person p where fk(p.account) is null", Integer.class )
							.getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( 3, (int) ids.get( 0 ) );

					assertThat( inspector.getSqlQueries() ).hasSize( 1 );
					assertThat( inspector.getSqlQueries().get( 0 ) ).doesNotContainIgnoringCase( " join " );
					assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( ".account_id is null" );
				}
		);
	}

	@Test
	public void testAssociationEqualsInWhereClause() {
		inTransaction(
				session -> {
					inspector.clear();

					// at the moment -
					//
					// select
					//        distinct p1_0.id
					//    from
					//        Person p1_0
					//    join
					//        ACCOUNT_TABLE a1_0
					//            on a1_0.id=p1_0.account_id
					//    where
					//        a1_0.id=?

					final List<Integer> ids = session.createQuery(
							"select distinct p.id from Person p where p.account = :acct", Integer.class )
							.setParameter( "acct", new Account( 1, null, null ) )
							.getResultList();

					assertThat( ids ).hasSize( 1 );
					assertThat( ids.get(0) ).isEqualTo( 1 );

					assertThat( inspector.getSqlQueries() ).hasSize( 1 );
					assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( " join " );
					assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( ".id=?" );
				}
		);
	}

	@Test
	public void testIsNullInWhereClause5() {
		inTransaction(
				session -> {
					final List<Integer> ids = session.createQuery(
									"select p.id from Person p where p.account.code is null or p.account.id is null", Integer.class )
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
									"select p.id from Person p where p.account.code = :code and p.account.id = :id", Integer.class )
							.setParameter( "code", "Fab" )
							.setParameter( "id", 2 )
							.getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( 2, (int) ids.get( 0 ) );

				}
		);
	}

	@Test
	public void testDelete() {
		inspector.clear();

		inTransaction( (entityManager) -> {
			entityManager.createQuery( "delete from Person p where p.account is null" ).executeUpdate();

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			// could physically be a join or exists sub-query
			assertThat( inspector.getSqlQueries().get( 0 ) )
					.matches( (sql) -> sql.contains( "left join" ) || sql.contains( "not exists" ) );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17384" )
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "left join cannot be used inside exists clause")
	public void testDeleteAdditionalPredicate() {
		inspector.clear();

		inTransaction( (entityManager) -> {
			entityManager.createQuery( "delete from Person p where p.account is null and p.lazyAccount.code <>'aaa'" ).executeUpdate();

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			// could physically be a join or exists sub-query
			assertThat( inspector.getSqlQueries().get( 0 ) )
					.matches( (sql) -> sql.contains( "left join" ) || sql.contains( "not exists" ) );
		} );
	}

	@Test
	public void testHqlUpdate() {
		inspector.clear();

		inTransaction( (entityManager) -> {
			entityManager.createQuery( "update Person p set p.name = 'abc' where p.account is null" ).executeUpdate();

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			// could physically be a join or exists sub-query
			assertThat( inspector.getSqlQueries().get( 0 ) )
					.matches( (sql) -> sql.contains( "left join" ) || sql.contains( "not exists" ) );
		} );
	}

	@Test
	public void testHqlUpdateSet() {
		inspector.clear();

		inTransaction( (entityManager) -> {
			entityManager.createQuery( "update Person p set p.account = null" ).executeUpdate();

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			assertThat( inspector.getSqlQueries().get(0) ).doesNotContainIgnoringCase( " join " );
			assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( "account_id=null" );
		} );
	}


	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		@OneToOne
		@NotFound(action = NotFoundAction.IGNORE)
		private Account account;

		@OneToOne(fetch = FetchType.LAZY)
		private Account lazyAccount;

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

	@SuppressWarnings({ "FieldCanBeLocal", "unused" })
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
