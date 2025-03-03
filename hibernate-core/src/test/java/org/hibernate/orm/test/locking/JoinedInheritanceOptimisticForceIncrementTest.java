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

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Jeroen Stiekema (jeroen@stiekema.eu)
 */
public class JoinedInheritanceOptimisticForceIncrementTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Person.class, Employee.class };
	}

	@Before
	public void prepare() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					Employee employee = new Employee( 1L, "John Doe", 10000 );
					session.persist( employee );
				}
		);
	}

	@After
	public void cleanup() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					session.remove( session.get( Employee.class, 1L ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11979")
	public void testForceIncrement() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					Employee lockedEmployee = session.get( Employee.class, 1L );
					session.lock( lockedEmployee, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
				}
		);
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
