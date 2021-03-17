/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.locking;

import java.util.Arrays;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Version;

import org.hibernate.testing.TestForIssue;
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

@TestForIssue(jiraKey = "HHH-13492")
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
					session.delete( employee );
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
