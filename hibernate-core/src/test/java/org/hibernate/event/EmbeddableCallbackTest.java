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

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12326")
public class EmbeddableCallbackTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Employee.class };
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = new Employee();
			employee.id = 1;

			entityManager.persist( employee );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Employee employee = entityManager.find( Employee.class, 1 );

			assertEquals( "Vlad", employee.name );
			assertEquals( "Developer Advocate", employee.details.jobTitle );
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Integer id;

		private String name;

		private EmployeeDetails details = new EmployeeDetails();

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
