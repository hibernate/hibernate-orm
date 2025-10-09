/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetching;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = {
		GraphFetchingTest.Department.class,
		GraphFetchingTest.Employee.class,
		GraphFetchingTest.Project.class
})
@SessionFactory
public class GraphFetchingTest {
	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			Department department = new Department();
			department.id = 1L;
			entityManager.persist(department);

			Employee employee1 = new Employee();
			employee1.id = 1L;
			employee1.username = "user1";
			employee1.password = "3fabb4de8f1ee2e97d7793bab2db1116";
			employee1.accessLevel = 0;
			employee1.department = department;
			entityManager.persist(employee1);

			Employee employee2 = new Employee();
			employee2.id = 2L;
			employee2.username = "user2";
			employee2.password = "3fabb4de8f1ee2e97d7793bab2db1116";
			employee2.accessLevel = 1;
			employee2.department = department;
			entityManager.persist(employee2);

			Project project = new Project();
			project.id = 1L;
			project.employees.add(employee1);
			entityManager.persist(project);
		});

		factoryScope.inTransaction( entityManager -> {
			Long userId = 1L;

			//tag::fetching-strategies-dynamic-fetching-entity-graph-example[]
			Employee employee = entityManager.find(
				Employee.class,
				userId,
				Collections.singletonMap(
					"jakarta.persistence.fetchgraph",
					entityManager.getEntityGraph("employee.projects")
				)
			);
			//end::fetching-strategies-dynamic-fetching-entity-graph-example[]
			assertNotNull(employee);
		});

		//tag::fetching-strategies-dynamic-fetching-entity-subgraph-example[]
		Project project = factoryScope.fromTransaction(entityManager -> {
			return entityManager.find(
				Project.class,
				1L,
				Collections.singletonMap(
					"jakarta.persistence.fetchgraph",
					entityManager.getEntityGraph("project.employees")
				)
			);
		});
		//end::fetching-strategies-dynamic-fetching-entity-subgraph-example[]
		assertEquals(1, project.getEmployees().size());
		assertEquals(Long.valueOf(1L), project.getEmployees().get(0).getDepartment().getId());
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
			read = "decrypt('AES', '00', pswd )",
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
			attributeNodes = @NamedAttributeNode("department")
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
