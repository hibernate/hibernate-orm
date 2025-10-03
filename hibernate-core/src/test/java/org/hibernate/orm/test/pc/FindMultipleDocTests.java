/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.RemovalsMode;
import org.hibernate.OrderingMode;
import org.hibernate.SessionCheckMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.hibernate.OrderingMode.ORDERED;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = FindMultipleDocTests.Person.class)
@SessionFactory
public class FindMultipleDocTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(  (session) -> {
			session.persist(  new Person( 1, "Paul" ) );
			session.persist(  new Person( 2, "John" ) );
			session.persist(  new Person( 3, "Ringo" ) );
			session.persist(  new Person( 4, "George" ) );
			session.persist(  new Person( 5, "David" ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testBasicUsage(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			//tag::pc-find-multiple-example[]
			List<Person> persons = session.findMultiple(
					Person.class,
					List.of(1,2,3),
					PESSIMISTIC_WRITE,
					ORDERED
			);
			//end::pc-find-multiple-example[]
		} );
	}

	@Test
	void testReplaceRemovals(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.remove( session.find( Person.class, 5 ) );

			List<Person> persons = session.findMultiple(
					Person.class,
					List.of(1,2,3,4,5),
					SessionCheckMode.ENABLED,
					RemovalsMode.REPLACE,
					OrderingMode.UNORDERED
			);
			assertThat( persons ).hasSize( 5 );
			assertThat( persons ).containsNull();
		} );
	}

	@Test
	void testIncludeRemovals(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.remove( session.find( Person.class, 5 ) );

			List<Person> persons = session.findMultiple(
					Person.class,
					List.of(1,2,3,4,5),
					SessionCheckMode.ENABLED,
					RemovalsMode.INCLUDE,
					OrderingMode.UNORDERED
			);
			assertThat( persons ).hasSize( 5 );
			assertThat( persons ).doesNotContainNull();
		} );
	}

	@Test
	void testOrderedRemovals(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.remove( session.find( Person.class, 5 ) );

			List<Person> persons = session.findMultiple(
					Person.class,
					List.of(1,2,3,4,5),
					SessionCheckMode.ENABLED,
					RemovalsMode.INCLUDE,
					OrderingMode.ORDERED
			);
			assertThat( persons ).hasSize( 5 );
			assertThat( persons ).doesNotContainNull();
			assertThat( persons ).map( Person::getId ).containsExactly( 1, 2, 3, 4, 5 );
		} );
	}

	@Test
	void testOrderedReplacedRemovals(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.remove( session.find( Person.class, 5 ) );

			List<Person> persons = session.findMultiple(
					Person.class,
					List.of(1,2,3,4,5),
					SessionCheckMode.ENABLED,
					RemovalsMode.REPLACE,
					OrderingMode.ORDERED
			);
			assertThat( persons ).hasSize( 5 );
			assertThat( persons ).containsNull();
		} );
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;

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
	}
}
