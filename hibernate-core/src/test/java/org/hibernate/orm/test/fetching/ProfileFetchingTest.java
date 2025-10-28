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
import jakarta.persistence.OneToMany;
import org.hibernate.Session;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = {
		ProfileFetchingTest.Department.class,
		ProfileFetchingTest.Employee.class,
		ProfileFetchingTest.Project.class
})
@SessionFactory
public class ProfileFetchingTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
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
		});

		factoryScope.inTransaction( entityManager -> {
			String username = "user1";
			String password = "3fabb4de8f1ee2e97d7793bab2db1116";

			Session session = entityManager.unwrap(Session.class);

			//tag::fetching-strategies-dynamic-fetching-profile-example[]
			session.enableFetchProfile("employee.projects");
			Employee employee = session.bySimpleNaturalId(Employee.class).load(username);
			//end::fetching-strategies-dynamic-fetching-profile-example[]
			assertNotNull( employee );
		});

	}

	@Entity(name = "Department")
	public static class Department {

		@Id
		private Long id;

		@OneToMany(mappedBy = "department")
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity
	}

	//tag::fetching-strategies-dynamic-fetching-profile-mapping-example[]
	@Entity(name = "Employee")
	@FetchProfile(
		name = "employee.projects",
		fetchOverrides = {
			@FetchProfile.FetchOverride(
				entity = Employee.class,
				association = "projects",
				mode = FetchMode.JOIN
			)
		}
	)
	//end::fetching-strategies-dynamic-fetching-profile-mapping-example[]
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
	}

	@Entity(name = "Project")
	public class Project {

		@Id
		private Long id;

		@ManyToMany
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity
	}
}
