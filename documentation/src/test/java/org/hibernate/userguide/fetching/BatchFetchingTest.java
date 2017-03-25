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
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class BatchFetchingTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( BatchFetchingTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Department.class,
				Employee.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( long i = 0; i < 10; i++ ) {
				Department department = new Department();
				department.id = i;
				entityManager.persist( department );

				for ( int j = 0; j < Math.random() * 5; j++ ) {
					Employee employee = new Employee();
					employee.id = (i * 5) + j;
					employee.name = String.format( "John %d", employee.getId() );
					employee.department = department;
					entityManager.persist( employee );
					department.employees.add( employee );
				}
				entityManager.flush();
			}
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::fetching-batch-fetching-example[]
			List<Department> departments = entityManager.createQuery(
				"select d " +
				"from Department d " +
				"inner join d.employees e " +
				"where e.name like 'John%'", Department.class)
			.getResultList();

			for ( Department department : departments ) {
				log.infof(
					"Department %d has {} employees",
					department.getId(),
					department.getEmployees().size()
				);
			}
			//end::fetching-batch-fetching-example[]
		} );
	}

	//tag::fetching-batch-mapping-example[]
	@Entity(name = "Department")
	public static class Department {

		@Id
		private Long id;

		@OneToMany(mappedBy = "department")
		//@BatchSize(size = 5)
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity

	//end::fetching-batch-mapping-example[]
		public Long getId() {
			return id;
		}

		public List<Employee> getEmployees() {
			return employees;
		}
	//tag::fetching-batch-mapping-example[]
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Department department;

		//Getters and setters omitted for brevity
	//end::fetching-batch-mapping-example[]

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Department getDepartment() {
			return department;
		}
	//tag::fetching-batch-mapping-example[]
	}
	//end::fetching-batch-mapping-example[]
}
