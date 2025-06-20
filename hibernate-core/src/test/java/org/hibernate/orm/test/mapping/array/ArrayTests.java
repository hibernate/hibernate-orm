/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.array;

import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.query.Query;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(
		annotatedClasses = { ArrayTests.Employee.class }
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class ArrayTests {

	@Test
	public void basicTest(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final Query<Employee> query = session.createQuery(
							"select e from Employee e",
							Employee.class
					);
					final Employee result = query.uniqueResult();
					assertThat( statistics.getPrepareStatementCount(), is(2L) );
					assertThat( result, notNullValue() );
					assertThat( result.getName(), is( "Koen" ) );
					String[] todo = result.getToDoList();
					assertThat( todo.length, is( 3 ) );
					assertThat( todo[0], is( "metro" ) );
					assertThat( todo[1], is( "boulot" ) );
					assertThat( todo[2], is( "dodo" ) );
				}
		);
	}

	@Test
	public void emptyArrayTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employee employee = new Employee( 2, "Andrea" );
					session.persist( employee );
				}
		);

		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final Query<Employee> query = session.createQuery(
							"select e from Employee e where e.id = :id",
							Employee.class
					);
					final Employee result = query.setParameter( "id", 2 ).uniqueResult();
					assertThat( statistics.getPrepareStatementCount(), is(2L) );

					assertThat( result, notNullValue() );
					assertThat( result.getName(), is( "Andrea" ) );
					String[] todo = result.getToDoList();
					assertThat( todo.length, is( 0 ) );
				}
		);

		statistics.clear();
		scope.inTransaction(
				session -> {
					final Query<Employee> query = session.createQuery(
							"select e from Employee e ",
							Employee.class
					);
					final List<Employee> results = query.list();
					assertThat( statistics.getPrepareStatementCount(), is(3L) );

					assertThat( results.size(), is( 2 ) );
					results.forEach( employee -> {
						if ( employee.getId() == 1 ) {
							assertThat( employee.getName(), is( "Koen" ) );
							String[] todo = employee.getToDoList();
							assertThat( todo.length, is( 3 ) );
							assertThat( todo[0], is( "metro" ) );
							assertThat( todo[1], is( "boulot" ) );
							assertThat( todo[2], is( "dodo" ) );
						}
						else {
							assertThat( employee.getName(), is( "Andrea" ) );
							String[] todo = employee.getToDoList();
							assertThat( todo.length, is( 0 ) );
						}
					} );
				}
		);
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employee employee = new Employee( 1, "Koen" );
					employee.setToDoList( new String[] { "metro", "boulot", "dodo" } );
					session.persist( employee );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Employee")
	@Table(name = "employee")
	public static class Employee {
		private Integer id;
		private String name;
		private String[] toDoList;

		public Employee() {
		}

		public Employee(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ElementCollection
		@OrderColumn
		public String[] getToDoList() {
			return toDoList;
		}

		public void setToDoList(String[] toDoList) {
			this.toDoList = toDoList;
		}
	}
}
