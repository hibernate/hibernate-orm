/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.mapsid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

@Jpa(annotatedClasses =
		{MapsSimpleIdToIdClassTest.Employee.class,
		MapsSimpleIdToIdClassTest.Dependent.class})
class MapsSimpleIdToIdClassTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			Employee employee = new Employee();
			employee.id = 1;
			employee.name = "X";
			session.persist( employee );
			Dependent dependent = new Dependent();
			dependent.name = "Y";
			dependent.employee = employee;
			session.persist( dependent );
		} );
	}

	@Entity
	static class Employee {
		@Id
		long id;

		String name;
	}

	static class DependentId {
		String name;
		long empId;
	}

	@Entity
	@IdClass(DependentId.class)
	static class Dependent {
		@Id
		String name;

		@Id
		long empId;

		@MapsId("empId")
		@ManyToOne
		Employee employee;
	}
}
