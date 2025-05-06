/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import java.util.Arrays;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */

@JiraKey(value = "HHH-13492")
@RunWith(CustomParameterized.class)
public class LockRefreshTest extends BaseNonConfigCoreFunctionalTestCase {
	private final LockModeType lockModeType;

	@Parameterized.Parameters(name = "JpaComplianceCachingSetting={0}")
	public static Iterable<Object[]> parameters() {
		return Arrays.asList(
				new Object[][] {
						{ LockModeType.OPTIMISTIC },
						{ LockModeType.OPTIMISTIC_FORCE_INCREMENT }
				}
		);
	}

	public LockRefreshTest(LockModeType lockModeType) {
		this.lockModeType = lockModeType;
	}

	@Test
	public void testLockRefreshUpdate() {
		doInHibernate(
				this::sessionFactory, session -> {
					final Employee employee = session.get( Employee.class, "Jane" );
					session.lock( employee, lockModeType );
					session.refresh( employee );
					employee.department = "Finance";
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					final Employee employee = session.get( Employee.class, "Jane" );
					assertEquals( "Finance", employee.department );
				}
		);
	}

	@Test
	public void testLockRefreshMerge() {
		doInHibernate(
				this::sessionFactory, session -> {
					final Employee employee = session.get( Employee.class, "Jane" );
					session.lock( employee, lockModeType );
					session.refresh( employee );
					employee.department = "Finance";
					session.merge( employee );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					final Employee employee = session.get( Employee.class, "Jane" );
					assertEquals( "Finance", employee.department );
				}
		);
	}

	@Test
	public void testLockRefreshDelete() {
		doInHibernate(
				this::sessionFactory, session -> {
					final Employee employee = session.get( Employee.class, "Jane" );
					session.lock( employee, lockModeType );
					session.refresh( employee );
					session.remove( employee );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					assertNull( session.get( Employee.class, "Jane" ) );
				}
		);
	}

	@Test
	public void testLockRefreshEvict() {
		doInHibernate(
				this::sessionFactory, session -> {
					final Employee employee = session.get( Employee.class, "Jane" );
					session.lock( employee, lockModeType );
					session.refresh( employee );
					employee.department = "Finance";
					session.evict( employee );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					final Employee employee = session.get( Employee.class, "Jane" );
					assertEquals( "Software Engineering", employee.department );
				}
		);
	}

	@Override
	public void prepareTest() {
		final Employee employee = new Employee();
		employee.name = "Jane";
		employee.department = "Software Engineering";

		doInHibernate(
				this::sessionFactory, session -> {
					session.persist( employee );
				}
		);
	}

	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class };
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private String name;

		private String department;

		@Version
		@Column(name = "ver")
		private int version;
	}
}
