/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(
		annotatedClasses = { SelectCaseWhenNullLiteralTest.Person.class }
)
@SessionFactory
@JiraKey(value = "HHH-15343")
public class SelectCaseWhenNullLiteralTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person person = new Person( 1l, "Fab" );
					session.persist( person );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelectCaseWhenNullLiteral(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List result = session.createQuery( "select case when 1=1 then 1 else null end from Person p" ).list();
					assertThat( result.size(), is( 1 ) );
					assertThat( result.get( 0 ), is( 1 ) );
				}
		);
	}

	@Test
	@JiraKey( "HHH-18556" )
	public void testSelectCaseWhenNullLiteralWithParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List result = session.createQuery( "select case when 1=1 then ?1 else null end from Person p" )
							.setParameter( 1, 2 )
							.list();
					assertThat( result.size(), is( 1 ) );
					assertThat( result.get( 0 ), is( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					List result = session.createQuery( "select count(case when 1=1 then ?1 else null end) from Person p" )
							.setParameter( 1, 2 )
							.list();
					assertThat( result.size(), is( 1 ) );
					assertThat( result.get( 0 ), is( 1L ) );
				}
		);
	}

	@Test
	@JiraKey( "HHH-19291" )
	public void testSelectCaseWhenNullLiteralWithParametersWithNamedParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List result = session.createQuery( "select case when 1=1 then :value else null end from Person p" )
							.setParameter( "value", 2 )
							.list();
					assertThat( result.size(), is( 1 ) );
					assertThat( result.get( 0 ), is( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					List result = session.createQuery( "select count(case when 1=1 then :value else null end) from Person p" )
							.setParameter( "value", 2 )
							.list();
					assertThat( result.size(), is( 1 ) );
					assertThat( result.get( 0 ), is( 1L ) );
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {
		@Id
		private Long id;

		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
