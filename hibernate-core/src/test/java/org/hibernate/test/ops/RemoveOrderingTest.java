/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class RemoveOrderingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Company.class, Person.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8550" )
	@FailureExpected( jiraKey = "HHH-8550" )
	public void testManyToOne() throws Exception {
		Session session = openSession();
		session.beginTransaction();
		try {
			Company company = new Company( 1, "acme" );
			Person person = new Person( 1, "joe", company );
			session.persist( person );
			session.flush();

			company = person.employer;

			session.delete( company );
			session.delete( person );
			session.flush();

			session.persist( person );
			session.flush();

			session.getTransaction().commit();
		}
		catch (Exception e) {
			session.getTransaction().rollback();
			throw e;
		}
		session.close();
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
