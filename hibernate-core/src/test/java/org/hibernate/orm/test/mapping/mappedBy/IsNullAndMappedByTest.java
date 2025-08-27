/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mappedBy;

import java.util.List;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 *
 * @see org.hibernate.orm.test.notfound.IsNullAndNotFoundTest
 */
@DomainModel( annotatedClasses = {
		IsNullAndMappedByTest.Person.class,
		IsNullAndMappedByTest.Account.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17384" )
public class IsNullAndMappedByTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person person1 = new Person( 1, "Luigi" );
			final Person person2 = new Person( 2, "Andrea" );
			final Person person3 = new Person( 3, "Max" );

			final Account account1 = new Account( 1, null, null, person1 );
			final Account account2 = new Account( 2, "Fab", null, person2 );
			final Account account3 = new Account( 3, "And", null, null );

			session.persist( person1 );
			session.persist( person2 );
			session.persist( person3 );
			session.persist( account1 );
			session.persist( account2 );
			session.persist( account3 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Account" ).executeUpdate();
			session.createMutationQuery( "delete from Person" ).executeUpdate();
		} );
	}

	@Test
	public void testAssociationDereferenceIsNullInWhereClause(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			inspector.clear();

			// should produce an inner join to ACCOUNT_TABLE

			final List<Integer> ids = session.createQuery(
					"select p.id from Person p where p.account.code is null",
					Integer.class
			).getResultList();

			assertEquals( 1, ids.size() );
			assertEquals( 1, (int) ids.get( 0 ) );

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( " join " );
			assertThat( inspector.getSqlQueries().get( 0 ) ).doesNotContainIgnoringCase( " left " );
			assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( " ACCOUNT_TABLE " );
		} );
	}

	@Test
	public void testAssociationIsNullInWhereClause(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			inspector.clear();

			// should produce a left join to ACCOUNT_TABLE and restrict based on the Account's id -
			//
			//	...
			//	from PERSON p
			//		left join ACCOUNT_TABLE a
			//			on p.account_id = a.id
			//	where a.id is null

			final List<Integer> ids = session.createQuery(
					"select distinct p.id from Person p where p.account is null",
					Integer.class
			).getResultList();

			assertEquals( 1, ids.size() );
			assertEquals( 3, (int) ids.get( 0 ) );

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( " left join " );
			assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( ".id is null" );
		} );
	}

	@Test
	public void testFetchedAssociationIsNullInWhereClause(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			inspector.clear();

			// should produce an inner join to ACCOUNT_TABLE since it's explicitly selected
			//
			//	...
			//	from PERSON p
			//		join ACCOUNT_TABLE a
			//			on p.account_id = a.id
			//	where a.id is null

			final List<Account> results = session.createQuery(
					"select p.account from Person p where p.account is null",
					Account.class
			).getResultList();

			assertThat( results ).isEmpty();

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( "join" );
			assertThat( inspector.getSqlQueries().get( 0 ) ).doesNotContainIgnoringCase( " left join " );
		} );
	}

	@Test
	public void testIsNullInWhereClause3(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			inspector.clear();

			final List<Integer> ids = session.createQuery(
					"select distinct a.id from Account a where fk(a.person) is null",
					Integer.class
			).getResultList();

			assertEquals( 1, ids.size() );
			assertEquals( 3, (int) ids.get( 0 ) );

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			assertThat( inspector.getSqlQueries().get( 0 ) ).doesNotContainIgnoringCase( " join " );
			assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( ".person_id is null" );
		} );
	}

	@Test
	public void testAssociationEqualsInWhereClause(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
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
					"select distinct p.id from Person p where p.account = :acct",
					Integer.class
			).setParameter( "acct", new Account( 1, null, null, null ) ).getResultList();

			assertThat( ids ).hasSize( 1 );
			assertThat( ids.get( 0 ) ).isEqualTo( 1 );

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( " join " );
			assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( ".id=?" );
		} );
	}

	@Test
	public void testIsNullInWhereClause5(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Integer> ids = session.createQuery(
							"select p.id from Person p where p.account.code is null or p.account.id is null",
							Integer.class
					)
					.getResultList();

			assertEquals( 1, ids.size() );
			assertEquals( 1, (int) ids.get( 0 ) );
		} );
	}

	@Test
	public void testWhereClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Integer> ids = session.createQuery(
							"select p.id from Person p where p.account.code = :code and p.account.id = :id",
							Integer.class
					)
					.setParameter( "code", "Fab" )
					.setParameter( "id", 2 )
					.getResultList();

			assertEquals( 1, ids.size() );
			assertEquals( 2, (int) ids.get( 0 ) );
		} );
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( (entityManager) -> {
			entityManager.createMutationQuery( "delete from Person p where p.account is null" ).executeUpdate();

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			// could physically be a join or exists sub-query
			assertThat( inspector.getSqlQueries()
								.get( 0 ) ).matches( (sql) -> sql.contains( "left join" ) || sql.contains( "not exists" ) );
		} );
	}

	@Test
	public void testHqlUpdate(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( (entityManager) -> {
			entityManager.createMutationQuery( "update Person p set p.name = 'abc' where p.account is null" ).executeUpdate();

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			// could physically be a join or exists sub-query
			assertThat( inspector.getSqlQueries()
								.get( 0 ) ).matches( (sql) -> sql.contains( "left join" ) || sql.contains( "not exists" ) );
		} );
	}

	@Test
	public void testHqlUpdateSet(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( (entityManager) -> {
			entityManager.createMutationQuery( "update Account a set a.person = null where id = 99" ).executeUpdate();

			assertThat( inspector.getSqlQueries() ).hasSize( 1 );
			assertThat( inspector.getSqlQueries().get( 0 ) ).doesNotContainIgnoringCase( " join " );
			assertThat( inspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( "person_id=null" );
		} );
	}


	@Entity( name = "Person" )
	public static class Person {

		@Id
		private Integer id;

		private String name;

		@OneToOne( mappedBy = "person" )
		private Account account;

		public Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
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

	@SuppressWarnings( { "FieldCanBeLocal", "unused" } )
	@Entity( name = "Account" )
	@Table( name = "ACCOUNT_TABLE" )
	public static class Account {
		@Id
		private Integer id;

		private String code;

		private Double amount;

		@OneToOne
		private Person person;

		public Account() {
		}

		public Account(Integer id, String code, Double amount, Person person) {
			this.id = id;
			this.code = code;
			this.amount = amount;
			this.person = person;
		}

	}
}
