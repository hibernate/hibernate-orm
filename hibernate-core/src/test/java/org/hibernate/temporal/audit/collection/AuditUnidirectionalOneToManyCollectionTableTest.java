/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.collection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.mapping.Table;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests unidirectional @OneToMany without @JoinColumn auditing.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditUnidirectionalOneToManyCollectionTableTest.Department.class,
		AuditUnidirectionalOneToManyCollectionTableTest.Employee.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.collection.AuditUnidirectionalOneToManyJoinColumnTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditUnidirectionalOneToManyCollectionTableTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}


	@Test
	void testWriteSide(SessionFactoryScope scope, DomainModelScope domainModelScope) {
		var tablesNames = domainModelScope.getDomainModel().collectTableMappings().stream().map( Table::getName ).collect( Collectors.toSet());
		assertTrue( tablesNames.contains( "my_custom_employees_audited" ) );
	}

	// ---- Entity classes ----

	@Audited
	@Entity(name = "Department")
	static class Department {
		@Id
		long id;
		String name;
		@OneToMany
//		@JoinColumn(name = "department_id")
		@Audited.CollectionTable( name = "my_custom_employees_audited" )
		List<Employee> employees = new ArrayList<>();

		Department() {
		}

		Department(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Audited
	@Entity(name = "Employee")
	static class Employee {
		@Id
		long id;
		String name;

		Employee() {
		}

		Employee(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
