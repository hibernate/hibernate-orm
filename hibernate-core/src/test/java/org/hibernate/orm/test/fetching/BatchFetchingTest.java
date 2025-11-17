/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetching;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		BatchFetchingTest.Department.class,
		BatchFetchingTest.Employee.class
})
@SessionFactory
public class BatchFetchingTest {
	private final Logger log = Logger.getLogger( BatchFetchingTest.class );

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (entityManager) -> {
			for (long i = 0; i < 10; i++) {
				Department department = new Department();
				department.id = i;
				entityManager.persist(department);

				for (int j = 0; j < Math.random() * 5; j++) {
					Employee employee = new Employee();
					employee.id = (i * 5) + j;
					employee.name = String.format("John %d", employee.getId());
					employee.department = department;
					entityManager.persist(employee);
					department.employees.add(employee);
				}
			}
		} );

		factoryScope.inTransaction( (entityManager) -> {
			//tag::fetching-batch-fetching-example[]
			List<Department> departments = entityManager.createQuery(
							"select d " +
							"from Department d " +
							"inner join d.employees e " +
							"where e.name like 'John%'", Department.class)
					.getResultList();

			for (Department department : departments) {
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
		@BatchSize(size = 5)
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
