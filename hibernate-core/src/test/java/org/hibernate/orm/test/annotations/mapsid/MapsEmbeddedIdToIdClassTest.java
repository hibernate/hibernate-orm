/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.mapsid;

import jakarta.persistence.*;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

@Jpa(annotatedClasses =
		{MapsEmbeddedIdToIdClassTest.Employee.class,
		MapsEmbeddedIdToIdClassTest.Dependent.class})
class MapsEmbeddedIdToIdClassTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			Employee employee = new Employee();
			employee.id = 1;
			employee.id2 = 2;
			employee.name = "X";
			session.persist( employee );
			Dependent dependent = new Dependent();
			dependent.name = "Y";
			dependent.employee = employee;
			session.persist( dependent );
		} );
	}

	static class EmployeeId {
		long id;
		long id2;
	}

	@Entity
	@IdClass(EmployeeId.class)
	static class Employee {
		@Id
		long id;

		@Id
		long id2;

		String name;
	}

	static class DependentId {
		String name;
		EmployeeId empId;
	}

	@Entity
	@IdClass(DependentId.class)
	static class Dependent {
		@Id
		String name;

		@EmbeddedId
		EmployeeId empId;

		@MapsId("empId")
		@ManyToOne
		Employee employee;
	}
}
