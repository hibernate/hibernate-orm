/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jeroen Stiekema (jeroen@stiekema.eu)
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		JoinedInheritanceOptimisticForceIncrementTest.Person.class,
		JoinedInheritanceOptimisticForceIncrementTest.Employee.class
})
@SessionFactory
public class JoinedInheritanceOptimisticForceIncrementTest {

	@BeforeEach
	public void prepare(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Employee( 1L, "John Doe", 10000 ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey(value = "HHH-11979")
	public void testForceIncrement(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var employeeToLock = session.find( Employee.class, 1L );
			assertEquals( 0, employeeToLock.getVersion() );
			session.lock( employeeToLock, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
		} );

		factoryScope.inTransaction( (session) -> {
			var employee = session.find( Employee.class, 1L );
			assertEquals( 1, employee.getVersion() );
		} );
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {
		@Id
		@Column(name = "PERSON_ID")
		private Long id;

		@Version
		@Column(name = "ver")
		private Integer version;

		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getVersion() {
			return version;
		}
	}

	@Entity(name = "Employee")
	@PrimaryKeyJoinColumn(name = "EMPLOYEE_ID", referencedColumnName = "PERSON_ID")
	public static class Employee extends Person {

		private Integer salary;

		public Employee() {
		}

		public Employee(Long id, String name, Integer salary) {
			super(id, name);
			this.salary = salary;
		}
	}

}
