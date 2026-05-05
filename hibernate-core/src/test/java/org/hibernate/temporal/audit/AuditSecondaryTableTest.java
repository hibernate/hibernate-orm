/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditSecondaryTableTest.Employee.class,
		AuditSecondaryTableTest.Department.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditSecondaryTableTest$TxIdSupplier"))
class AuditSecondaryTableTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	@Test
	void testWriteAndPointInTimeRead(SessionFactoryScope scope) {
		currentTxId = 0;

		scope.getSessionFactory().inTransaction( session -> {
			session.persist( new Department( 1L, "Engineering" ) );
			var emp = new Employee();
			emp.id = 1L;
			emp.name = "Alice";
			emp.address = "123 Main St";
			emp.phone = "555-0100";
			emp.department = session.getReference( Department.class, 1L );
			session.persist( emp );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var emp = session.find( Employee.class, 1L );
			emp.name = "Alice B.";
			emp.address = "456 Oak Ave";
			emp.phone = "555-0200";
		} );

		scope.getSessionFactory().inTransaction( session ->
				session.remove( session.find( Employee.class, 1L ) )
		);

		try (var s = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( 1 ).openStatelessSession()) {
			var emp = s.get( Employee.class, 1L );
			assertEquals( "Alice", emp.name );
			assertEquals( "123 Main St", emp.address );
			assertEquals( "555-0100", emp.phone );
			assertNotNull( emp.department );
			assertEquals( "Engineering", emp.department.name );
		}

		try (var s = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( 2 ).openStatelessSession()) {
			var emp = s.get( Employee.class, 1L );
			assertEquals( "Alice B.", emp.name );
			assertEquals( "456 Oak Ave", emp.address );
			assertEquals( "555-0200", emp.phone );
		}

		try (var s = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( 3 ).openStatelessSession()) {
			assertNull( s.get( Employee.class, 1L ) );
		}
	}

	@Test
	void testHistory(SessionFactoryScope scope) {
		currentTxId = 100;

		scope.getSessionFactory().inTransaction( session -> {
			session.persist( new Department( 10L, "Engineering" ) );
			var emp = new Employee();
			emp.id = 10L;
			emp.name = "Bob";
			emp.address = "100 First Ave";
			emp.department = session.getReference( Department.class, 10L );
			session.persist( emp );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var emp = session.find( Employee.class, 10L );
			emp.address = "200 Second Ave";
		} );

		scope.getSessionFactory().inTransaction( session ->
				session.remove( session.find( Employee.class, 10L ) )
		);

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Employee.class, 10L );
			assertEquals( 3, history.size() );

			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertEquals( "Bob", history.get( 0 ).entity().name );
			assertEquals( "100 First Ave", history.get( 0 ).entity().address );

			assertEquals( ModificationType.MOD, history.get( 1 ).modificationType() );
			assertEquals( "200 Second Ave", history.get( 1 ).entity().address );

			assertEquals( ModificationType.DEL, history.get( 2 ).modificationType() );
		}
	}

	@Test
	void testAssociationOnSecondaryTable(SessionFactoryScope scope) {
		currentTxId = 200;

		scope.getSessionFactory().inTransaction( session -> {
			session.persist( new Department( 20L, "Engineering" ) );
			session.persist( new Department( 21L, "Marketing" ) );
			var emp = new Employee();
			emp.id = 20L;
			emp.name = "Carol";
			emp.department = session.getReference( Department.class, 20L );
			session.persist( emp );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var emp = session.find( Employee.class, 20L );
			emp.department = session.getReference( Department.class, 21L );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var emp = session.find( Employee.class, 20L );
			emp.department = null;
		} );

		try (var s = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( 201 ).openStatelessSession()) {
			assertEquals( "Engineering", s.get( Employee.class, 20L ).department.name );
		}

		try (var s = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( 202 ).openStatelessSession()) {
			assertEquals( "Marketing", s.get( Employee.class, 20L ).department.name );
		}

		try (var s = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( 203 ).openStatelessSession()) {
			assertNull( s.get( Employee.class, 20L ).department );
		}

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Employee.class, 20L );
			assertEquals( 3, history.size() );

			assertEquals( "Engineering",
					history.get( 0 ).entity().department.name );
			assertEquals( "Marketing",
					history.get( 1 ).entity().department.name );
			assertNull( history.get( 2 ).entity().department );
		}
	}

	@Test
	void testGetRevisions(SessionFactoryScope scope) {
		currentTxId = 300;

		scope.getSessionFactory().inTransaction( session -> {
			var emp = new Employee();
			emp.id = 30L;
			emp.name = "Dave";
			session.persist( emp );
		} );

		scope.getSessionFactory().inTransaction( session ->
				session.find( Employee.class, 30L ).address = "123 Main St"
		);

		scope.getSessionFactory().inTransaction( session ->
				session.find( Employee.class, 30L ).phone = "555-0100"
		);

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var revisions = auditLog.getChangesets( Employee.class, 30L );
			assertEquals( 3, revisions.size() );
			assertEquals( 301, revisions.get( 0 ) );
			assertEquals( 302, revisions.get( 1 ) );
			assertEquals( 303, revisions.get( 2 ) );
		}
	}

	@Entity(name = "Employee")
	@Audited
	@SecondaryTable(name = "employee_contact")
	@SecondaryTable(name = "employee_org")
	static class Employee {
		@Id
		Long id;
		String name;
		@Column(table = "employee_contact")
		String address;
		@Column(table = "employee_contact")
		String phone;
		@ManyToOne
		@JoinColumn(table = "employee_org")
		Department department;
	}

	@Entity(name = "Department")
	@Audited
	static class Department {
		@Id
		Long id;
		String name;

		Department() {
		}

		Department(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
