/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetching;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {
		FetchingTest.Department.class,
		FetchingTest.Employee.class,
		FetchingTest.Project.class
})
@RequiresDialect(H2Dialect.class)
public class FetchingTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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

		});

		scope.inTransaction( entityManager -> {
			String username = "user1";
			String password = "3fabb4de8f1ee2e97d7793bab2db1116";
			//tag::fetching-strategies-no-fetching-example[]
			Employee employee = entityManager.createQuery(
				"select e " +
				"from Employee e " +
				"where " +
				"	e.username = :username and " +
				"	e.password = :password",
				Employee.class)
			.setParameter("username", username)
			.setParameter("password", password)
			.getSingleResult();
			//end::fetching-strategies-no-fetching-example[]
			assertNotNull(employee);
		});

		scope.inTransaction( entityManager -> {
			String username = "user1";
			String password = "3fabb4de8f1ee2e97d7793bab2db1116";
			//tag::fetching-strategies-no-fetching-scalar-example[]
			Integer accessLevel = entityManager.createQuery(
				"select e.accessLevel " +
				"from Employee e " +
				"where " +
				"	e.username = :username and " +
				"	e.password = :password",
				Integer.class)
			.setParameter("username", username)
			.setParameter("password", password)
			.getSingleResult();
			//end::fetching-strategies-no-fetching-scalar-example[]
			assertEquals(Integer.valueOf(0), accessLevel);
		});

		scope.inTransaction( entityManager -> {
			String username = "user1";
			String password = "3fabb4de8f1ee2e97d7793bab2db1116";
			//tag::fetching-strategies-dynamic-fetching-jpql-example[]
			Employee employee = entityManager.createQuery(
				"select e " +
				"from Employee e " +
				"left join fetch e.projects " +
				"where " +
				"	e.username = :username and " +
				"	e.password = :password",
				Employee.class)
			.setParameter("username", username)
			.setParameter("password", password)
			.getSingleResult();
			//end::fetching-strategies-dynamic-fetching-jpql-example[]
			assertNotNull(employee);
		});

		scope.inTransaction( entityManager -> {
			String username = "user1";
			String password = "3fabb4de8f1ee2e97d7793bab2db1116";
			//tag::fetching-strategies-dynamic-fetching-criteria-example[]

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Employee> query = builder.createQuery(Employee.class);
			Root<Employee> root = query.from(Employee.class);
			root.fetch("projects", JoinType.LEFT);
			query.select(root).where(
				builder.and(
					builder.equal(root.get("username"), username),
					builder.equal(root.get("password"), password)
				)
			);
			Employee employee = entityManager.createQuery(query).getSingleResult();
			//end::fetching-strategies-dynamic-fetching-criteria-example[]
			assertNotNull(employee);
		});
	}

	//tag::fetching-strategies-domain-model-example[]
	@Entity(name = "Department")
	public static class Department {

		@Id
		private Long id;

		@OneToMany(mappedBy = "department")
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity
	}

	//tag::mapping-column-read-and-write-example[]
	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String username;

		@Column(name = "pswd", columnDefinition = "varbinary")
		@ColumnTransformer(
			read = "trim(trailing u&'\\0000' from cast(decrypt('AES', '00', pswd ) as character varying))",
			write = "encrypt('AES', '00', ?)"
		)
		private String password;

		private int accessLevel;

		@ManyToOne(fetch = FetchType.LAZY)
		private Department department;

		@ManyToMany(mappedBy = "employees")
		private List<Project> projects = new ArrayList<>();

		//Getters and setters omitted for brevity
	}
	//end::mapping-column-read-and-write-example[]

	@Entity(name = "Project")
	public class Project {

		@Id
		private Long id;

		@ManyToMany
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity
	}
	//end::fetching-strategies-domain-model-example[]
}
