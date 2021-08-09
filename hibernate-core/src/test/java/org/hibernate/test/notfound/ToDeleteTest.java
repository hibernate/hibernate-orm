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

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(
		annotatedClasses = {
				ToDeleteTest.Task.class,
				ToDeleteTest.Employee.class,
				ToDeleteTest.Location.class
		}
)
@SessionFactory
public class ToDeleteTest {
	@Test
	public void testExistingProxyWithNonExistingAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Employee employee = new Employee();
					employee.id = 1;
					session.persist( employee );

					final Task task = new Task();
					task.id = 2;
					task.employee = employee;
					session.persist( task );

					session.flush();

					session.createNativeQuery( "update Employee set locationId = 3 where id = 1" )
							.executeUpdate();
				} );

		try {
			scope.inTransaction(
					session -> {
						session.load( Employee.class, 1 );
						session.createQuery( "from Task", Task.class ).getSingleResult();
					} );
			fail( "EntityNotFoundException should have been thrown because Task.employee.location is not found " +
						  "and is not mapped with @NotFound(IGNORE)" );
		}
		catch (EntityNotFoundException expected) {
		}
	}

	@Entity(name = "Task")
	public static class Task {

		@Id
		private int id;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(
				name = "employeeId",
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
		)
		@NotFound(action = NotFoundAction.IGNORE)
		private Employee employee;

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
