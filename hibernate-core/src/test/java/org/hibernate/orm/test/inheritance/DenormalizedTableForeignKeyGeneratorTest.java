/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.*;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Paria Hosseiny
 */
@JiraKey("HHH-18470")
@Jpa(
		annotatedClasses = {
				DenormalizedTableForeignKeyGeneratorTest.Employee.class,
				DenormalizedTableForeignKeyGeneratorTest.Manager.class,
				DenormalizedTableForeignKeyGeneratorTest.Address.class,
				DenormalizedTableForeignKeyGeneratorTest.Territory.class
		}
)

@RequiresDialect(H2Dialect.class)
public class DenormalizedTableForeignKeyGeneratorTest {

	@Test
	public void shouldCreateForeignKeyForSubclasses(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					String managerQuery = "select CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
							"WHERE TABLE_NAME='MANAGER' " +
							"AND CONSTRAINT_TYPE='FOREIGN KEY'";
					List<String> managerForeignKeyNames = entityManager.createNativeQuery(managerQuery).getResultList();

					String employeeQuery = "select CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
							"WHERE TABLE_NAME='EMPLOYEE' " +
							"AND CONSTRAINT_TYPE='FOREIGN KEY'";
					String employeeForeignKeyName = entityManager.createNativeQuery(employeeQuery).getSingleResult().toString();

					assertThat(employeeForeignKeyName).isNotNull();
					assertThat(managerForeignKeyNames).isNotNull().hasSize(2);
					assertThat(managerForeignKeyNames).doesNotContain(employeeForeignKeyName);
				}
		);
	}

	@Entity(name = "Employee")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static class Employee implements Serializable {

		@Id
		@GeneratedValue
		Long id;

		@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
		private Address address;

	}

	@Entity(name = "Manager")
	static class Manager extends Employee {

		@OneToOne(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
		private Territory territory;
	}

	@Entity(name = "Address")
	static class Address {

		@Id
		@GeneratedValue
		Long id;

		@Column(nullable = false, columnDefinition = "TEXT")
		private String address = "";
	}

	@Entity(name = "Territory")
	static class Territory {

		@Id
		@GeneratedValue
		Long id;

		@Column
		private String location = "";
	}

}
