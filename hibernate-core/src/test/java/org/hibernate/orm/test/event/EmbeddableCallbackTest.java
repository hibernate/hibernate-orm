/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = EmbeddableCallbackTest.Employee.class
)
public class EmbeddableCallbackTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-12326")
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Employee employee = new Employee();
			employee.details = new EmployeeDetails();
			employee.id = 1;

			entityManager.persist( employee );
		} );

		scope.inTransaction( entityManager -> {
			Employee employee = entityManager.find( Employee.class, 1 );

			assertThat( employee.name, is( "Vlad" ) );
			assertThat( employee.details.jobTitle, is( "Developer Advocate" ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13110")
	public void testNullEmbeddable(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Employee employee = new Employee();
			employee.id = 1;

			entityManager.persist( employee );
		} );

		scope.inTransaction( entityManager -> {
			Employee employee = entityManager.find( Employee.class, 1 );

			assertThat( employee.name, is( "Vlad" ) );
			assertNull( employee.details );
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Integer id;

		private String name;

		private EmployeeDetails details;

		@PrePersist
		public void setUp() {
			name = "Vlad";
		}
	}

	@Embeddable
	public static class EmployeeDetails {

		private String jobTitle;

		@PrePersist
		public void setUp() {
			jobTitle = "Developer Advocate";
		}
	}
}
