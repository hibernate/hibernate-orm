/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = {
				JoinedInheritanceDeletionTest.Person.class,
				JoinedInheritanceDeletionTest.Employee.class,
				JoinedInheritanceDeletionTest.Customer.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15115")
public class JoinedInheritanceDeletionTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person person = new Person( 1, "Bob" );
					Employee employee = new Employee( 2, "Chris", "Software Engineer" );
					Customer customer = new Customer( 3, "Miriam", "" );

					session.persist( person );
					session.persist( employee );
					session.persist( customer );
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.dropData();
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
