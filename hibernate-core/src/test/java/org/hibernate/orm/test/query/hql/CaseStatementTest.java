/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = CaseStatementTest.Person.class )
@SessionFactory
public class CaseStatementTest {

	@Test
	public void testSimpleCaseStatementFixture(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					s.createQuery( "select case p.name when 'Steve' then 'x' else 'y' end from Person p" )
							.list();
				}
		);
	}

	@Test
	public void testSimpleCaseStatementWithParamResult(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {

					s.createQuery( "select case p.name when 'Steve' then :opt1 else p.name end from Person p" )
							.setParameter( "opt1", "x" )
							.list();
				}
		);
	}

	@Test
	public void testSimpleCaseStatementWithParamAllResults(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.createQuery( "select case p.name when 'Steve' then :opt1 else :opt2 end from Person p" )
							.setParameter( "opt1", "x" )
							.setParameter( "opt2", "y" )
							.list();

					session.createQuery( "select case p.name when 'Steve' then cast( :opt1 as string ) else cast( :opt2 as string) end from Person p" )
							.setParameter( "opt1", "x" )
							.setParameter( "opt2", "y" )
							.list();

					session.createQuery( "select case p.name when 'Steve' then :opt1 else :opt2 end from Person p" )
							.setParameter( "opt1", "x" )
							.setParameter( "opt2", "y" )
							.list();
				}
		);
	}

	@Test
	public void testSearchedCaseStatementFixture(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					s.createQuery( "select case when p.name = 'Steve' then 'x' else 'y' end from Person p" )
							.list();
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-13095" )
	public void testSearchedCaseStatementArithmeticExpression(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Person steve = new Person();
					steve.id = 1;
					steve.name = "Steve";
					session.persist( steve );

					Person brian = new Person();
					brian.id = 2;
					brian.name = "Brian";
					session.persist( brian );

					List<Integer> values = session.createQuery(
							"select case when p.name = 'Steve' then (p.id * 10) else p.id end from Person p order by p.id" )
							.getResultList();

					assertEquals( 10, (int) values.get( 0 ) );
					assertEquals( 2, (int) values.get( 1 ) );
				}
		);
	}

	@Test
	public void testSearchedCaseStatementWithParamResult(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					s.createQuery( "select case when p.name = 'Steve' then :opt1 else p.name end from Person p" )
							.setParameter( "opt1", "x" )
							.list();
				}
		);
	}

	@Test
	public void testSearchedCaseStatementWithAllParamResults(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.persist( new Person( 1, "Steve" ) )
		);

		scope.inTransaction(
				(session) -> {
					final List list = session.createQuery( "select case when p.name = 'Steve' then :opt1 else :opt2 end from Person p" )
							.setParameter( "opt1", "x" )
							.setParameter( "opt2", "y" )
							.list();
					assertThat( list.size(), is( 1 ) );
					assertThat( list.get( 0 ), is( "x" )  );
				}
		);

		scope.inTransaction(
				(session) -> {
					final List list = session.createQuery( "select case when p.name = 'Steve' then cast( :opt1 as string) else :opt2 end from Person p" )
							.setParameter( "opt1", "x" )
							.setParameter( "opt2", "y" )
							.list();
					assertThat( list.size(), is( 1 ) );
					assertThat( list.get( 0 ), is( "x" )  );
				}
		);
	}


	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;
		private String name;

		private Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
