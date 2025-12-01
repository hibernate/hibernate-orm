/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy;

import org.hibernate.Hibernate;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DomainModel(
		annotatedClasses = {
				DetachedProxyAsQueryParameterTest.Department.class,
				DetachedProxyAsQueryParameterTest.BasicDepartment.class,
				DetachedProxyAsQueryParameterTest.SpecialDepartment.class,
				DetachedProxyAsQueryParameterTest.Employee.class
		}
)
@SessionFactory
@JiraKey("HHH-17704")
public class DetachedProxyAsQueryParameterTest {

	private static final Integer EMPLOYEE_ID = 1;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SpecialDepartment department = new SpecialDepartment( 2, "dep1" );
					Employee employee = new Employee( EMPLOYEE_ID, "Fab", department );
					session.persist( department );
					session.persist( employee );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		Employee employee = scope.fromTransaction(
				session ->
						session.find( Employee.class, EMPLOYEE_ID )
		);
		assertFalse( Hibernate.isInitialized( employee.getDepartment() ) );
		scope.inSession(
				session -> {
					Query<Employee> query = session.createQuery(
							"select e from Employee e where e.department = :department",
							Employee.class
					);
					query.setParameter( "department", employee.getDepartment() );
					query.list();
				}
		);
		scope.inSession(
				session -> {
					session.createQuery(
							"select d from Department d where d = :department" )
							.setParameter( "department", employee.getDepartment() ).list();
				}
		);


		scope.inSession(
				session -> {
					session.createQuery(
							"select d from SpecialDepartment d where d = :department" )
							.setParameter( "department", employee.getDepartment() ).list();
				}
		);
	}

	@Entity(name = "Department")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Department {
		@Id
		private Integer id;

		public Department() {
		}

		public Department(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = "BasicDepartment")
	public static class BasicDepartment extends Department {


		private String name;

		public BasicDepartment() {
		}

		public BasicDepartment(Integer id, String name) {
			super( id );
			this.name = name;
		}


		public String getName() {
			return name;
		}
	}

	@Entity(name = "SpecialDepartment")
	public static class SpecialDepartment extends BasicDepartment {
		public SpecialDepartment() {
		}

		public SpecialDepartment(Integer id, String name) {
			super( id, name );
		}
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Integer id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Department department;

		public Employee() {
		}

		public Employee(Integer id, String name, Department department) {
			this.id = id;
			this.name = name;
			this.department = department;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Department getDepartment() {
			return department;
		}
	}
}
