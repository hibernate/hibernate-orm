/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = { NativeQueryLimitOffsetTest.Person.class }
)
@SessionFactory
@JiraKey("HHH-16020")
public class NativeQueryLimitOffsetTest {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Person( 1L, "John" ) );
					session.persist( new Person( 2L, "Jack" ) );
					session.persist( new Person( 3L, "Bob" ) );
					session.persist( new Person( 4L, "Jill" ) );
					session.persist( new Person( 5L, "Jane" ) );
					session.persist( new Person( 6L, "Anne" ) );
					session.persist( new Person( 7L, "Joe" ) );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testFullLimitOffsetOnNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Long> l = session.createNativeQuery( "select id from Person where name like :name", Long.class)
							.setParameter("name", "J%")
							.setFirstResult( 1 )
							.setMaxResults( 4 )
							.getResultList();
					assertEquals( 2, l.get( 0 ) );
					assertEquals( 4, l.size() );
				}
		);
	}

	@Test
	public void testPartialLimitOffsetOnNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Long> l = session.createNativeQuery( "select id from Person where name like :name", Long.class)
							.setParameter("name", "J%")
							.setFirstResult(1)
							.getResultList();
					assertEquals( 2, l.get( 0 ) );
					assertEquals( 4, l.size() );

					l = session.createNativeQuery( "select id from Person where name like :name", Long.class)
							.setParameter("name", "J%")
							.setMaxResults( 3 )
							.getResultList();
					assertEquals( 1, l.get( 0 ) );
					assertEquals( 3, l.size() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18309" )
	public void testLimitOffsetZeroValue(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Long> l = session.createNativeQuery( "select id from Person where name like :name", Long.class )
					.setParameter( "name", "J%" )
					.setFirstResult( 0 )
					.getResultList();
			assertEquals( 5, l.size() );

			l = session.createNativeQuery( "select id from Person where name like :name", Long.class )
					.setParameter( "name", "J%" )
					.setMaxResults( 0 )
					.getResultList();
			assertEquals( 0, l.size() );
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Long id;
		private String name;

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
