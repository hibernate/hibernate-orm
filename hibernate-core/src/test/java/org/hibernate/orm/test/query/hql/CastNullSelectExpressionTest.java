/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
@DomainModel( annotatedClasses = CastNullSelectExpressionTest.Person.class )
@SessionFactory
public class CastNullSelectExpressionTest {

	@Test
	@JiraKey(value = "HHH-10757")
	public void testSelectCastNull(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Object[] result = (Object[]) session.createQuery(
							"select firstName, cast( null as string ), lastName from Person where lastName='Munster'"
					).uniqueResult();

					assertEquals( 3, result.length );
					assertEquals( "Herman", result[0] );
					assertNull( result[1] );
					assertEquals( "Munster", result[2] );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10757")
	public void testSelectNewCastNull(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Person result = (Person) session.createQuery(
							"select new Person( id, firstName, cast( null as string ), lastName ) from Person where lastName='Munster'"
					).uniqueResult();
					assertEquals( "Herman", result.firstName );
					assertNull( result.middleName );
					assertEquals( "Munster", result.lastName );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16564")
	public void testSelectNewNull(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Person result = (Person) session.createQuery(
							"select new Person( id, firstName, null, lastName ) from Person where lastName='Munster'"
					).uniqueResult();
					assertEquals( "Herman", result.firstName );
					assertNull( result.middleName );
					assertEquals( "Munster", result.lastName );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16564")
	public void testSelectNull(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Object[] result = (Object[]) session.createQuery(
							"select id, firstName, null, lastName from Person where lastName='Munster'"
					).uniqueResult();
					assertEquals( "Herman", result[1] );
					assertNull( result[2] );
					assertEquals( "Munster", result[3] );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Person person = new Person();
					person.firstName = "Herman";
					person.middleName = "Joseph";
					person.lastName = "Munster";
					session.persist( person );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name= "Person" )
	@Table(name = "PERSON")
	public static class Person {
		@Id
		@GeneratedValue
		private long id;

		private String firstName;

		private String middleName;

		private String lastName;

		private Integer age;

		Person() {
		}

		public Person(long id, String firstName, String middleName, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.middleName = middleName;
			this.lastName = lastName;
		}

		public Person(long id, String firstName, Integer age, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.middleName = null;
			this.lastName = lastName;
			this.age = age;
		}

	}
}
