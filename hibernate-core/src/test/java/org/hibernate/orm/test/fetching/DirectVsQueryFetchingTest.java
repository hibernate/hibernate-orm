/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetching;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class DirectVsQueryFetchingTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Department.class,
			Employee.class,
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Department department = new Department();
			department.id = 1L;
			entityManager.persist(department);

			Employee employee1 = new Employee();
			employee1.id = 1L;
			employee1.username = "user1";
			employee1.department = department;
			entityManager.persist(employee1);

		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::fetching-direct-vs-query-direct-fetching-example[]
			Employee employee = entityManager.find(Employee.class, 1L);
			//end::fetching-direct-vs-query-direct-fetching-example[]
		});

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::fetching-direct-vs-query-entity-query-example[]
			Employee employee = entityManager.createQuery(
					"select e " +
					"from Employee e " +
					"where e.id = :id", Employee.class)
			.setParameter("id", 1L)
			.getSingleResult();
			//end::fetching-direct-vs-query-entity-query-example[]
		});
	}

	//tag::fetching-direct-vs-query-domain-model-example[]
	@Entity(name = "Department")
	public static class Department {

		@Id
		private Long id;

		//Getters and setters omitted for brevity
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String username;

		@ManyToOne(fetch = FetchType.EAGER)
		private Department department;

		//Getters and setters omitted for brevity
	}
	//end::fetching-direct-vs-query-domain-model-example[]
}
