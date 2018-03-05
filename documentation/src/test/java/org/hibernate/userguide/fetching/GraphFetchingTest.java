/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.fetching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.QueryHints;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class GraphFetchingTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Department.class,
				Employee.class,
				Project.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Department department = new Department();
			department.id = 1L;
			entityManager.persist( department );

			Employee employee1 = new Employee();
			employee1.id = 1L;
			employee1.username = "user1";
			employee1.password = "3fabb4de8f1ee2e97d7793bab2db1116";
			employee1.accessLevel = 0;
			employee1.department = department;
			entityManager.persist( employee1 );

			Employee employee2 = new Employee();
			employee2.id = 2L;
			employee2.username = "user2";
			employee2.password = "3fabb4de8f1ee2e97d7793bab2db1116";
			employee2.accessLevel = 1;
			employee2.department = department;
			entityManager.persist( employee2 );

			Project project = new Project();
			project.id = 1L;
			project.employees.add( employee1 );
			entityManager.persist( project );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Long userId = 1L;

			//tag::fetching-strategies-dynamic-fetching-entity-graph-example[]
			Employee employee = entityManager.find(
				Employee.class,
				userId,
				Collections.singletonMap(
					"javax.persistence.fetchgraph",
					entityManager.getEntityGraph( "employee.projects" )
				)
			);
			//end::fetching-strategies-dynamic-fetching-entity-graph-example[]
			assertNotNull(employee);
		} );

		//tag::fetching-strategies-dynamic-fetching-entity-subgraph-example[]
		Project project = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.find(
				Project.class,
				1L,
				Collections.singletonMap(
					"javax.persistence.fetchgraph",
					entityManager.getEntityGraph( "project.employees" )
				)
			);
		} );
		//end::fetching-strategies-dynamic-fetching-entity-subgraph-example[]
		assertEquals(1, project.getEmployees().size());
		assertEquals( Long.valueOf( 1L ), project.getEmployees().get( 0 ).getDepartment().getId() );
	}

	@Entity(name = "Department")
	public static class Department {

		@Id
		private Long id;

		@OneToMany(mappedBy = "department")
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Employee> getEmployees() {
			return employees;
		}

		public void setEmployees(List<Employee> employees) {
			this.employees = employees;
		}
	}

	//tag::fetching-strategies-dynamic-fetching-entity-graph-mapping-example[]
	@Entity(name = "Employee")
	@NamedEntityGraph(name = "employee.projects",
		attributeNodes = @NamedAttributeNode("projects")
	)
	//end::fetching-strategies-dynamic-fetching-entity-graph-mapping-example[]
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String username;

		@Column(name = "pswd")
		@ColumnTransformer(
			read = "decrypt( 'AES', '00', pswd  )",
			write = "encrypt('AES', '00', ?)"
		)
		private String password;

		private int accessLevel;

		@ManyToOne(fetch = FetchType.LAZY)
		private Department department;

		@ManyToMany(mappedBy = "employees")
		private List<Project> projects = new ArrayList<>();

		//Getters and setters omitted for brevity

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public int getAccessLevel() {
			return accessLevel;
		}

		public void setAccessLevel(int accessLevel) {
			this.accessLevel = accessLevel;
		}

		public Department getDepartment() {
			return department;
		}

		public void setDepartment(Department department) {
			this.department = department;
		}

		public List<Project> getProjects() {
			return projects;
		}

		public void setProjects(List<Project> projects) {
			this.projects = projects;
		}
	}

	//tag::fetching-strategies-dynamic-fetching-entity-subgraph-mapping-example[]
	@Entity(name = "Project")
	@NamedEntityGraph(name = "project.employees",
		attributeNodes = @NamedAttributeNode(
			value = "employees",
			subgraph = "project.employees.department"
		),
		subgraphs = @NamedSubgraph(
			name = "project.employees.department",
			attributeNodes = @NamedAttributeNode( "department" )
		)
	)
	public static class Project {

		@Id
		private Long id;

		@ManyToMany
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity
	//end::fetching-strategies-dynamic-fetching-entity-subgraph-mapping-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Employee> getEmployees() {
			return employees;
		}

		public void setEmployees(List<Employee> employees) {
			this.employees = employees;
		}
	//tag::fetching-strategies-dynamic-fetching-entity-subgraph-mapping-example[]
	}
	//end::fetching-strategies-dynamic-fetching-entity-subgraph-mapping-example[]
}
