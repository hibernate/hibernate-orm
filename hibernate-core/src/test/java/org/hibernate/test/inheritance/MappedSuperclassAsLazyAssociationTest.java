/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.legacy.Custom;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class MappedSuperclassAsLazyAssociationTest extends BaseCoreFunctionalTestCase {

	private String taskName;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Custom.class,
				Employee.class,
				InTrainingEmployee.class,
				RemoteEmployee.class,
				Task.class
		};
	}

	@Before
	public void setUp() {
		RemoteEmployee remoteEmployee = new RemoteEmployee();
		remoteEmployee.setName( "another" );
		remoteEmployee.setSecondName( "second" );
		remoteEmployee.setTitle( "Software Engineer" );

		InTrainingEmployee floatingEmployee = new InTrainingEmployee();
		floatingEmployee.setName( "floating" );

		Task task = new Task();
		task.setName( "task" );
		task.setEmployee( remoteEmployee );
		task.setFloatingEmployee( floatingEmployee );

		inTransaction(
				session -> {
					session.save( remoteEmployee );

					session.save( floatingEmployee );

					session.save( task );
				} );
		taskName = task.getName();
	}

	@After
	public void tearDowm() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Task" ).executeUpdate();
					session.createQuery( "delete from Task" ).executeUpdate();
					session.createQuery( "delete from InTrainingEmployee" ).executeUpdate();
					session.createQuery( "delete from RemoteEmployee" ).executeUpdate();
					session.createQuery( "delete from Person" ).executeUpdate();
				}
		);
	}

	@Test
	public void testGet() {
		inTransaction(
				session -> {
					Task task = session.get( Task.class, taskName );
					assertFalse( Hibernate.isInitialized( task.getEmployee() ) );
					assertThat( task.getEmployee().getName(), is( "another" ) );
					assertThat( task.getEmployee().getTitle(), is( "Software Engineer" ) );

					assertTrue( Hibernate.isInitialized( task.getEmployee() ) );

				}
		);
	}

	@Test
	public void testHql() {
		inTransaction(
				session -> {
					List<Task> list = session.createQuery( "select t from Task t", Task.class ).list();

					Task task = list.get( 0 );
					assertFalse( Hibernate.isInitialized( task.getEmployee() ) );
					assertThat( task.getEmployee().getName(), is( "another" ) );
					assertThat( task.getEmployee().getTitle(), is( "Software Engineer" ) );

					assertTrue( Hibernate.isInitialized( task.getEmployee() ) );
				}
		);
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person { // in real scenario, this has many direct and indirect subclasses
		private String name;

		@Id
		public String getName() {
			return name;
		}

		protected void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Customer")
	public static class Customer extends Person {
	}

	@MappedSuperclass
	public static abstract class Employee extends Person {
		private List<Task> tasks;

		private String title;

		@OneToMany
		public List<Task> getTasks() {
			return tasks;
		}

		protected void setTasks(List<Task> tasks) {
			this.tasks = tasks;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	@MappedSuperclass
	public static abstract class FloatingEmployee extends Employee {

	}

	@Entity(name = "InTrainingEmployee")
	public static class InTrainingEmployee extends FloatingEmployee {

	}

	@MappedSuperclass
	public static abstract class NoFloatingEmployee extends Employee {

	}

	@Entity(name = "RemoteEmployee")
	public static class RemoteEmployee extends NoFloatingEmployee {
		private String secondName;

		public String getSecondName() {
			return secondName;
		}

		public void setSecondName(String secondName) {
			this.secondName = secondName;
		}
	}

	@Entity(name = "Task")
	public static class Task {
		private String name;

		private NoFloatingEmployee employee;

		private FloatingEmployee floatingEmployee;

		@Id
		public String getName() {
			return name;
		}

		protected void setName(String name) {
			this.name = name;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public NoFloatingEmployee getEmployee() {
			return employee;
		}

		protected void setEmployee(NoFloatingEmployee employee) {
			this.employee = employee;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public FloatingEmployee getFloatingEmployee() {
			return floatingEmployee;
		}

		public void setFloatingEmployee(FloatingEmployee floatingEmployee) {
			this.floatingEmployee = floatingEmployee;
		}
	}

}
