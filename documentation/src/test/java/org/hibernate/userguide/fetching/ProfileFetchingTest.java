/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.fetching;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Session;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class ProfileFetchingTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( ProfileFetchingTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Department.class,
				Employee.class,
				Project.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Department department = new Department();
			department.id = 1L;
			entityManager.persist( department );

			Employee employee1 = new Employee();
			employee1.id = 1L;
			employee1.username = "user1";
			employee1.password = "3fabb4de8f1ee2e97d7793bab2db1116";
			employee1.accessLevel = 0;
			employee1.department = department;
			entityManager.persist( employee1 );

			Employee employee2 = new Employee();
			employee2.id = 2L;
			employee2.username = "user2";
			employee2.password = "3fabb4de8f1ee2e97d7793bab2db1116";
			employee2.accessLevel = 1;
			employee2.department = department;
			entityManager.persist( employee2 );

		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			String username = "user1";
			String password = "3fabb4de8f1ee2e97d7793bab2db1116";

			Session session = entityManager.unwrap( Session.class );

			//tag::fetching-strategies-dynamic-fetching-profile-example[]
			session.enableFetchProfile( "employee.projects" );
			Employee employee = session.bySimpleNaturalId( Employee.class ).load( username );
			//end::fetching-strategies-dynamic-fetching-profile-example[]
			assertNotNull(employee);
		} );

	}

	@Entity(name = "Department")
	public static class Department {

		@Id
		private Long id;

		@OneToMany(mappedBy = "department")
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity
	}

	//tag::fetching-strategies-dynamic-fetching-profile-mapping-example[]
	@Entity(name = "Employee")
	@FetchProfile(
		name = "employee.projects",
		fetchOverrides = {
			@FetchProfile.FetchOverride(
				entity = Employee.class,
				association = "projects",
				mode = FetchMode.JOIN
			)
		}
	)
	//end::fetching-strategies-dynamic-fetching-profile-mapping-example[]
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String username;

		@Column(name = "pswd")
		@ColumnTransformer(
			read = "decrypt( 'AES', '00', pswd  )",
			write = "encrypt('AES', '00', ?)"
		)
		private String password;

		private int accessLevel;

		@ManyToOne(fetch = FetchType.LAZY)
		private Department department;

		@ManyToMany(mappedBy = "employees")
		private List<Project> projects = new ArrayList<>();

		//Getters and setters omitted for brevity
	}

	@Entity(name = "Project")
	public class Project {

		@Id
		private Long id;

		@ManyToMany
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity
	}
}
