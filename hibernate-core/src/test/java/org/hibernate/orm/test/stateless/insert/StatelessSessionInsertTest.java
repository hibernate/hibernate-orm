/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless.insert;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.StatelessSession;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Simple "smoke" test of {@linkplain StatelessSession#insert} with associations
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		StatelessSessionInsertTest.Department.class,
		StatelessSessionInsertTest.Employee.class,
})
@SessionFactory
public class StatelessSessionInsertTest {

	@Test
	public void testInsertWithForeignKey(SessionFactoryScope scope) {
		final Department department = scope.fromTransaction( (session) -> {
			final Department dept = new Department( 1, "Marketing" );
			session.persist( dept );
			return dept;
		} );

		scope.inStatelessTransaction( (statelessSession) -> {
			final Employee employee = new Employee( 1, "John Jacobs", department );
			statelessSession.insert( employee );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Entity
	@Table(name = "departments")
	public static class Department {
		@Id
		private Integer id;
		private String name;

		public Department() {
		}

		public Department(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity
	@Table(name = "employees")
	public static class Employee {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		@JoinColumn(name = "msg_fk")
		private Department department;

		public Employee() {
		}

		public Employee(Integer id, String name, Department department) {
			this.id = id;
			this.name = name;
			this.department = department;
		}
	}
}
