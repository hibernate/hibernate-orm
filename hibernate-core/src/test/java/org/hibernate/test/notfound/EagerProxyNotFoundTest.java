/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.notfound;

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
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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
			task.employeeEagerNotFoundIgnore = session.load( Employee.class, 2 );
			session.persist( task );
		});

		doInHibernate( this::sessionFactory, session -> {
			final Task task = session.createQuery( "from Task", Task.class ).getSingleResult();
			assertNotNull( task );
			assertNull( task.employeeEagerNotFoundIgnore );
		});
	}

	@Test
	public void testNonExistingProxyInSession() {
		doInHibernate( this::sessionFactory, session -> {
			final Task task = new Task();
			task.id = 1;
			task.employeeEagerNotFoundIgnore = session.load( Employee.class, 2 );
			session.persist( task );
		});

		doInHibernate( this::sessionFactory, session -> {
			session.load( Employee.class, 2 );
			final Task task = session.createQuery( "from Task", Task.class ).getSingleResult();
			assertNotNull( task );
			assertNull( task.employeeEagerNotFoundIgnore );
		});
	}

	@Test
	public void testEagerIgnoreLazyProxy() {
		doInHibernate( this::sessionFactory, session -> {
			final Task task = new Task();
			task.id = 1;
			task.employeeLazy = session.load( Employee.class, 2 );
			task.employeeEagerNotFoundIgnore = task.employeeLazy;
			session.persist( task );
		});

		doInHibernate( this::sessionFactory, session -> {
			final Task task = session.createQuery( "from Task", Task.class ).getSingleResult();
			assertNotNull( task );
			assertNull( task.employeeEagerNotFoundIgnore );
			assertNotNull( task.employeeLazy );
			assertTrue( HibernateProxy.class.isInstance( task.employeeLazy ) );
			assertEquals( 2, task.employeeLazy.getId() );
		});
	}

	@Test
	public void testProxyInSessionEagerIgnoreLazyProxy() {
		doInHibernate( this::sessionFactory, session -> {
			final Task task = new Task();
			task.id = 1;
			task.employeeLazy = session.load( Employee.class, 2 );
			task.employeeEagerNotFoundIgnore = task.employeeLazy;
			session.persist( task );
		});

		doInHibernate( this::sessionFactory, session -> {
			final Employee employeeProxy = session.load( Employee.class, 2 );
			final Task task = session.createQuery( "from Task", Task.class ).getSingleResult();
			assertNotNull( task );
			assertNull( task.employeeEagerNotFoundIgnore );
			assertNotNull( task.employeeLazy );
			assertTrue( HibernateProxy.class.isInstance( task.employeeLazy ) );
			assertEquals( 2, task.employeeLazy.getId() );
			assertSame( employeeProxy, task.employeeLazy );
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
			task.employeeEagerNotFoundIgnore = employee;
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
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@NotFound(action = NotFoundAction.IGNORE)
		private Employee employeeEagerNotFoundIgnore;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(
				name = "lazyEmployeeId",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		private Employee employeeLazy;
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private int id;

		private String name;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "locationId", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		private Location location;

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
	}

	@Entity(name = "Location")
	public static class Location {
		@Id
		private int id;

		private String description;
	}
}
