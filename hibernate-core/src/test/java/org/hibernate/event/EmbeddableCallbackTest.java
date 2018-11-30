/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class EmbeddableCallbackTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Employee.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12326")
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = new Employee();
			employee.details = new EmployeeDetails();
			employee.id = 1;

			entityManager.persist( employee );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = entityManager.find( Employee.class, 1 );

			assertEquals( "Vlad", employee.name );
			assertEquals( "Developer Advocate", employee.details.jobTitle );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13110")
	public void testNullEmbeddable() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = new Employee();
			employee.id = 1;

			entityManager.persist( employee );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = entityManager.find( Employee.class, 1 );

			assertEquals( "Vlad", employee.name );
			assertNull( employee.details );
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Integer id;

		private String name;

		private EmployeeDetails details;

		@PrePersist
		public void setUp() {
			name = "Vlad";
		}
	}

	@Embeddable
	public static class EmployeeDetails {

		private String jobTitle;

		@PrePersist
		public void setUp() {
			jobTitle = "Developer Advocate";
		}
	}
}
