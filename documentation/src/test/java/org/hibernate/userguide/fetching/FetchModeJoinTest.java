/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.fetching;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class FetchModeJoinTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( FetchModeJoinTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Department.class,
			Employee.class,
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( long i = 0; i < 2; i++ ) {
				Department department = new Department();
				department.id = i + 1;
				entityManager.persist( department );

				for ( long j = 0; j < 3; j++ ) {
					Employee employee1 = new Employee();
					employee1.username = String.format( "user %d_%d", i, j );
					employee1.department = department;
					entityManager.persist( employee1 );
				}
			}
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::fetching-strategies-fetch-mode-join-example[]
			Department department = entityManager.find( Department.class, 1L );

			log.infof( "Fetched department: %s", department.getId());

			assertEquals( 3, department.getEmployees().size() );
			//end::fetching-strategies-fetch-mode-join-example[]
		} );
	}

	@Entity(name = "Department")
	public static class Department {

		@Id
		private Long id;

		//tag::fetching-strategies-fetch-mode-join-mapping-example[]
		@OneToMany(mappedBy = "department")
		@Fetch(FetchMode.JOIN)
		private List<Employee> employees = new ArrayList<>();
		//end::fetching-strategies-fetch-mode-join-mapping-example[]

		//Getters and setters omitted for brevity

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Employee> getEmployees() {
			return employees;
		}

		public void setEmployees(List<Employee> employees) {
			this.employees = employees;
		}
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String username;

		@ManyToOne(fetch = FetchType.LAZY)
		private Department department;

		//Getters and setters omitted for brevity
		
	}
}
