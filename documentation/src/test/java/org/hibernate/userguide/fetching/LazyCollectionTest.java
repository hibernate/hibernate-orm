/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.fetching;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class LazyCollectionTest extends BaseEntityManagerFunctionalTestCase {

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
			//tag::fetching-LazyCollection-persist-example[]
			Department department = new Department();
			department.setId( 1L );
			entityManager.persist( department );

			for (long i = 1; i <= 3; i++ ) {
				Employee employee = new Employee();
				employee.setId( i );
				employee.setUsername( String.format( "user_%d", i ) );
				department.addEmployee(employee);
			}
			//end::fetching-LazyCollection-persist-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::fetching-LazyCollection-select-example[]
			Department department = entityManager.find(Department.class, 1L);

			int employeeCount = department.getEmployees().size();

			for(int i = 0; i < employeeCount; i++ ) {
				log.infof( "Fetched employee: %s", department.getEmployees().get( i ).getUsername());
			}
			//end::fetching-LazyCollection-select-example[]
		} );
	}

	//tag::fetching-LazyCollection-domain-model-example[]
	@Entity(name = "Department")
	public static class Department {

		@Id
		private Long id;

		@OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
		@OrderColumn(name = "order_id")
		@LazyCollection( LazyCollectionOption.EXTRA )
		private List<Employee> employees = new ArrayList<>();

		//Getters and setters omitted for brevity

	//end::fetching-LazyCollection-domain-model-example[]

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

		public void addEmployee(Employee employee) {
			this.employees.add( employee );
			employee.setDepartment( this );
		}
	//tag::fetching-LazyCollection-domain-model-example[]
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String username;

		@ManyToOne(fetch = FetchType.LAZY)
		private Department department;

		//Getters and setters omitted for brevity

	//end::fetching-LazyCollection-domain-model-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Department getDepartment() {
			return department;
		}

		public void setDepartment(Department department) {
			this.department = department;
		}
	//tag::fetching-LazyCollection-domain-model-example[]
	}
	//end::fetching-LazyCollection-domain-model-example[]
}
