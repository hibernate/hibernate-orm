/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.legacy.Custom;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
public class MappedSuperclassAsAssociationTest extends BaseCoreFunctionalTestCase {

	private String taskName;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Custom.class,
				Employee.class,
				FloatingEmployee.class,
				RemoteEmployee.class,
				Task.class
		};
	}

	@Before
	public void setUp() {
		RemoteEmployee remoteEmployee = new RemoteEmployee();
		remoteEmployee.setName( "another" );
		remoteEmployee.setSecondName( "second" );

		FloatingEmployee floatingEmployee = new FloatingEmployee();
		floatingEmployee.setName( "floating" );

		Task task = new Task();
		task.setName( "task" );

		inTransaction(
				session -> {
					session.save( remoteEmployee );

					session.save( floatingEmployee );

					task.setEmployee( remoteEmployee );
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
					session.createQuery( "delete from FloatingEmployee" ).executeUpdate();
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
					assertThat( task.getEmployee(), CoreMatchers.instanceOf( RemoteEmployee.class ) );
				}
		);
	}

	@Test
	public void testHql() {
		inTransaction(
				session -> {
					List<Task> list = session.createQuery( "select t from Task t join t.employee", Task.class ).list();

					Task task = list.get( 0 );
					String name = task.getName();
					assertThat( task.getEmployee(), CoreMatchers.instanceOf( RemoteEmployee.class ) );
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

		@OneToMany
		public List<Task> getTasks() {
			return tasks;
		}

		protected void setTasks(List<Task> tasks) {
			this.tasks = tasks;
		}
	}

	@Entity(name = "FloatingEmployee")
	public static class FloatingEmployee extends Employee {
	}

	@Entity(name = "RemoteEmployee")
	public static class RemoteEmployee extends Employee {
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

		private Employee employee;

		@Id
		public String getName() {
			return name;
		}

		@ManyToOne
		public Employee getEmployee() {
			return employee;
		}

		protected void setName(String name) {
			this.name = name;
		}

		protected void setEmployee(Employee employee) {
			this.employee = employee;
		}
	}

}
