/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetching;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		FetchModeSubselectTest.Department.class,
		FetchModeSubselectTest.Employee.class,
})
@SessionFactory
public class FetchModeSubselectTest {
	private final Logger log = Logger.getLogger( FetchModeSubselectTest.class );

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			for (long i = 0; i < 2; i++) {
				Department department = new Department();
				department.id = i + 1;
				department.name = String.format("Department %d", department.id);
				entityManager.persist(department);

				for (long j = 0; j < 3; j++) {
					Employee employee1 = new Employee();
					employee1.username = String.format("user %d_%d", i, j);
					employee1.department = department;
					entityManager.persist(employee1);
				}
			}
		});

		factoryScope.inTransaction( entityManager -> {
			//tag::fetching-strategies-fetch-mode-subselect-example[]
			List<Department> departments = entityManager.createQuery(
				"select d " +
				"from Department d " +
				"where d.name like :token", Department.class)
			.setParameter("token", "Department%")
			.getResultList();

			log.infof("Fetched %d Departments", departments.size());

			for (Department department : departments) {
				assertEquals( 3, department.getEmployees().size() );
			}
			//end::fetching-strategies-fetch-mode-subselect-example[]
		});
	}

	@Entity(name = "Department")
	public static class Department {

		@Id
		private Long id;

		private String name;

		//tag::fetching-strategies-fetch-mode-subselect-mapping-example[]
		@OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
		@Fetch(FetchMode.SUBSELECT)
		private List<Employee> employees = new ArrayList<>();
		//end::fetching-strategies-fetch-mode-subselect-mapping-example[]

		//Getters and setters omitted for brevity

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
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
