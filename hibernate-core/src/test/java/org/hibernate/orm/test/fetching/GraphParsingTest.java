/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetching;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.TypedQuery;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.graph.EntityGraphs;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.orm.test.fetching.GraphFetchingTest.Department;
import org.hibernate.orm.test.fetching.GraphFetchingTest.Employee;
import org.hibernate.orm.test.fetching.GraphFetchingTest.Project;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = {
		Project.class,
		Employee.class,
		Department.class,
		GraphParsingTest.Person.class,
		GraphParsingTest.Movie.class,
		GraphParsingTest.Theater.class,
		GraphParsingTest.Showing.class,
		GraphParsingTest.Ticket.class
})
@SessionFactory
public class GraphParsingTest {
	@Test
	public void testParsingExample1(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::fetching-strategies-dynamic-fetching-entity-graph-parsing-example-1[]
			final EntityGraph<Project> graph = GraphParser.parse(
					Project.class,
					"employees(department)",
					entityManager
			);
			//end::fetching-strategies-dynamic-fetching-entity-graph-parsing-example-1[]
			assertNotNull(graph);
		} );
	}

	@Test
	public void testParsingExample2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::fetching-strategies-dynamic-fetching-entity-graph-parsing-example-2[]
			final EntityGraph<Project> graph = GraphParser.parse(
					Project.class,
					"employees(username, password, accessLevel, department(employees(username)))",
					entityManager
			);
			//end::fetching-strategies-dynamic-fetching-entity-graph-parsing-example-2[]
			assertNotNull(graph);
		} );
	}

	@Test
	public void testMapKeyParsing(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::fetching-strategies-dynamic-fetching-entity-graph-parsing-key-example-1[]
			final EntityGraph<Movie> graph = GraphParser.parse(
					Movie.class,
					"cast.key(name)",
					entityManager
			);
			//end::fetching-strategies-dynamic-fetching-entity-graph-parsing-key-example-1[]
			assertNotNull(graph);
		} );
	}

	@Test
	public void testEntityKeyParsing(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::fetching-strategies-dynamic-fetching-entity-graph-parsing-key-example-2[]
			final EntityGraph<Ticket> graph = GraphParser.parse(
					Ticket.class,
					"showing(id(movie(cast)))",
					entityManager
			);
			//end::fetching-strategies-dynamic-fetching-entity-graph-parsing-key-example-2[]
			assertNotNull(graph);
		} );
	}

	@Test
	public void testMergingExample(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::fetching-strategies-dynamic-fetching-entity-graph-merging-example[]
			final EntityGraph<Project> a = GraphParser.parse(
					Project.class, "employees(username)", entityManager
			);

			final EntityGraph<Project> b = GraphParser.parse(
					Project.class, "employees(password, accessLevel)", entityManager
			);

			final EntityGraph<Project> c = GraphParser.parse(
					Project.class, "employees(department(employees(username)))", entityManager
			);

			final EntityGraph<Project> all = EntityGraphs.merge(entityManager, Project.class, a, b, c);
			//end::fetching-strategies-dynamic-fetching-entity-graph-merging-example[]

			final EntityGraph<Project> expected = GraphParser.parse(
					Project.class,
					"employees(username, password, accessLevel, department(employees(username)))",
					entityManager
			);
			assertTrue(EntityGraphs.areEqual(expected, all));
		} );
	}

	@Test
	public void testFindExample(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			Long userId = 1L;

			//tag::fetching-strategies-dynamic-fetching-entity-graph-apply-example-find[]
			entityManager.find(
					Employee.class,
					userId,
					Collections.singletonMap(
							GraphSemantic.FETCH.getJakartaHintName(),
							GraphParser.parse(Employee.class, "username, accessLevel, department", entityManager)
					)
			);
			//end::fetching-strategies-dynamic-fetching-entity-graph-apply-example-find[]
		});
	}

	@Test
	public void testQueryExample(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::fetching-strategies-dynamic-fetching-entity-graph-apply-example-query[]
			final String graphString = "username, accessLevel";
			final String queryString = "select e from Employee e where e.id = 1";

			final EntityGraph<Employee> graph = GraphParser.parse(
					Employee.class,
					graphString,
					entityManager
			);

			TypedQuery<Employee> query1 = entityManager.createQuery(queryString, Employee.class);
			query1.setHint(
					GraphSemantic.FETCH.getJakartaHintName(),
					graph
			);
			//end::fetching-strategies-dynamic-fetching-entity-graph-apply-example-query[]
		});
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Some entities for illustrating key subgraphs
	//
	// NOTE : for the moment I do not add named (sub)graph annotations because
	//		this is only used for discussing graph parsing

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;

		String name;
	}

	@Entity(name = "Movie")
	public static class Movie {
		@Id
		private Integer id;

		String title;

		@ElementCollection
//		@OneToMany
		Map<Person,String> cast;
	}

	@Entity(name = "Theater")
	public static class Theater {
		@Id
		private Integer id;

		int seatingCapacity;
		boolean foodService;
	}

	@Entity(name = "Showing")
	public static class Showing {
		@Embeddable
		public static class Id implements Serializable {
			@ManyToOne
			@JoinColumn
			private Movie movie;

			@ManyToOne
			@JoinColumn
			private Theater theater;
		}

		@EmbeddedId
		private Id id;

		private LocalDateTime startTime;
		private LocalDateTime endTime;
	}

	@Entity(name = "Ticket")
	public static class Ticket {
		@Id
		private Integer id;

		@ManyToOne
		Showing showing;
	}
}
