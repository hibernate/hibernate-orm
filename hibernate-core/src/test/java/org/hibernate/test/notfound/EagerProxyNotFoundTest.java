/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.notfound;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.EntityNotFoundException;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-14537")
public class EagerProxyNotFoundTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testNoProxyInSession() {
		doInHibernate( this::sessionFactory, session -> {
			final Task task = new Task();
			task.id = 1;
			task.employeeId = 2;
			session.persist( task );
		});

		doInHibernate( this::sessionFactory, session -> {
			final Task task = session.createQuery( "from Task", Task.class ).getSingleResult();
			assertNotNull( task );
			assertEquals( 2, task.employeeId );
			assertNull( task.employee );
		});
	}

	@Test
	public void testNonExistingProxyInSession() {
		doInHibernate( this::sessionFactory, session -> {
			final Task task = new Task();
			task.id = 1;
			task.employeeId = 2;
			session.persist( task );
		});

		doInHibernate( this::sessionFactory, session -> {
			session.load( Employee.class, 2 );
			final Task task = session.createQuery( "from Task", Task.class ).getSingleResult();
			assertNotNull( task );
			assertEquals( 2, task.employeeId );
			assertNull( task.employee );
		});
	}

	@Test
	public void testExistingProxyWithNonExistingAssociation() {
		doInHibernate( this::sessionFactory, session -> {
			final Employee employee = new Employee();
			employee.id = 1;
			session.persist( employee );

			final Task task = new Task();
			task.id = 2;
			task.employee = employee;
			task.employeeId = 1;
			session.persist( task );

			session.flush();

			session.createNativeQuery( "update Employee set locationId = 3 where id = 1" )
					.executeUpdate();
		});

		try {
			doInHibernate( this::sessionFactory, session -> {
				session.load( Employee.class, 1 );
				session.createQuery( "from Task", Task.class ).getSingleResult();
			});
			fail( "EntityNotFoundException should have been thrown because Task.employee.location is not found " +
					"and is not mapped with @NotFound(IGNORE)" );
		}
		catch (EntityNotFoundException expected) {
		}
	}

	@After
	public void deleteData() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "delete from Task" ).executeUpdate();
			session.createQuery( "delete from Employee" ).executeUpdate();
			session.createQuery( "delete from Location" ).executeUpdate();
		});
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Task.class,
				Employee.class,
				Location.class
		};
	}

	@Entity(name = "Task")
	public static class Task  {

		@Id
		private int id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(
				name = "employeeId",
				insertable = false,
				updatable = false,
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@NotFound(action = NotFoundAction.IGNORE)
		private Employee employee;

		@Column(name = "employeeId")
		private int employeeId;
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private int id;

		private String name;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "locationId", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		private Location location;
	}

	@Entity(name = "Location")
	public static class Location {
		@Id
		private int id;

		private String description;
	}
}
