package org.hibernate.test.inheritance;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQL81Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

@RequiresDialect(PostgreSQL81Dialect.class)
@TestForIssue( jiraKey = "HHH-15115")
public class JoinedInheritanceDeletionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.DEFAULT_SCHEMA, "public" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Employee.class,
				Customer.class
		};
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Person person = new Person( 1, "Bob" );
					Employee employee = new Employee( 2, "Chris", "Software Engineer" );
					Customer customer = new Customer( 3, "Miriam", "" );

					session.save( person );
					session.save( employee );
					session.save( customer );
				}
		);
	}

	@Test
	public void testDelete() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Person" ).executeUpdate();
				}
		);
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		private Integer id;

		private String name;

		public Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}

	@Entity(name = "Customer")
	public static class Customer extends Person {

		private String comments;

		public Customer() {
		}

		public Customer(Integer id, String name, String comments) {
			super( id, name );
			this.comments = comments;
		}

		public String getComments() {
			return comments;
		}

	}

	@Entity(name = "Employee")
	public static class Employee extends Person {

		private String title;

		public Employee() {
		}

		public Employee(Integer id, String name, String title) {
			super( id, name );
			this.title = title;
		}

		public String getTitle() {
			return title;
		}
	}

}
