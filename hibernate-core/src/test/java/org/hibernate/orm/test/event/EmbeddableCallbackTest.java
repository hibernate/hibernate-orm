/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.event;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12326")
public class EmbeddableCallbackTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Employee.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void test() {
		sessionFactoryScope().inTransaction( session -> {
			Employee employee = new Employee();
			employee.id = 1;

			session.persist( employee );
		} );

		sessionFactoryScope().inTransaction( session -> {
			Employee employee = session.find( Employee.class, 1 );

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
