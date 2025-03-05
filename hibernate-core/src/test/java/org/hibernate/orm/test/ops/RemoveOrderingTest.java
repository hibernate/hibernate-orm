/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = { RemoveOrderingTest.Company.class, RemoveOrderingTest.Person.class }
)
public class RemoveOrderingTest {

	@Test
	@JiraKey( value = "HHH-8550" )
	@FailureExpected( jiraKey = "HHH-8550" )
	public void testManyToOne(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
				session -> {
					Company company = new Company( 1, "acme" );
					Person person = new Person( 1, "joe", company );
					session.persist( person );
					session.flush();

					company = person.employer;

					session.remove( company );
					session.remove( person );
					session.flush();

					session.persist( person );
					session.flush();

					session.getTransaction().commit();
				}
		);
	}

	@Entity( name="Company" )
	@Table( name = "COMPANY" )
	public static class Company {
		@Id
		public Integer id;
		public String name;

		public Company() {
		}

		public Company(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name="Person" )
	@Table( name = "PERSON" )
	public static class Person {
		@Id
		public Integer id;
		public String name;
		@ManyToOne( cascade= CascadeType.ALL, optional = false )
		@JoinColumn( name = "EMPLOYER_FK" )
		public Company employer;

		public Person() {
		}

		public Person(Integer id, String name, Company employer) {
			this.id = id;
			this.name = name;
			this.employer = employer;
		}
	}

}
