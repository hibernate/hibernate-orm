/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.collection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Tests unidirectional @OneToMany with @JoinColumn auditing.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditUnidirectionalOneToManyJoinColumnTest.Department.class,
		AuditUnidirectionalOneToManyJoinColumnTest.Employee.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.collection.AuditUnidirectionalOneToManyJoinColumnTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditUnidirectionalOneToManyJoinColumnTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	// Shared lifecycle: Department(1) + employees, add/remove/delete
	private int revCreate;  // Department(1) + Employee(1, "Alice")
	private int revAdd;     // add Employee(2, "Bob")
	private int revRemove;  // remove Employee(1)
	private int revDelete;  // delete Department(1)

	// Recreate scenario (IDs 10-19)
	private int revRecCreate;  // Department(10) + Employee(10)+Employee(11)
	private int revRecReplace; // clear, re-add Employee(11) + new Employee(12)

	// Property update scenario (IDs 20-29)
	private int revUpdCreate; // Department(20) + Employee(20)
	private int revUpdMod;    // update Employee(20) name only

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		currentTxId = 0;
		final var sf = scope.getSessionFactory();

		// --- Shared lifecycle ---

		// Rev 1: department + one employee
		sf.inTransaction( session -> {
			var emp = new Employee( 1L, "Alice" );
			session.persist( emp );
			var dept = new Department( 1L, "Engineering" );
			dept.employees.add( emp );
			session.persist( dept );
		} );
		revCreate = currentTxId;

		// Rev 2: add second employee
		sf.inTransaction( session -> {
			var emp = new Employee( 2L, "Bob" );
			session.persist( emp );
			var dept = session.find( Department.class, 1L );
			dept.employees.add( emp );
		} );
		revAdd = currentTxId;

		// Rev 3: remove first employee from department
		sf.inTransaction( session -> {
			var dept = session.find( Department.class, 1L );
			dept.employees.removeIf( e -> e.id == 1L );
		} );
		revRemove = currentTxId;

		// Rev 4: delete department
		sf.inTransaction( session -> {
			var dept = session.find( Department.class, 1L );
			session.remove( dept );
		} );
		revDelete = currentTxId;

		// --- Recreate scenario ---

		// Rev 5: department with Alice + Bob
		sf.inTransaction( session -> {
			var e1 = new Employee( 10L, "Rec Alice" );
			var e2 = new Employee( 11L, "Rec Bob" );
			session.persist( e1 );
			session.persist( e2 );
			var dept = new Department( 10L, "Rec Engineering" );
			dept.employees.add( e1 );
			dept.employees.add( e2 );
			session.persist( dept );
		} );
		revRecCreate = currentTxId;

		// Rev 6: recreate: clear and re-add Bob + new Charlie
		sf.inTransaction( session -> {
			var e3 = new Employee( 12L, "Rec Charlie" );
			session.persist( e3 );
			var dept = session.find( Department.class, 10L );
			dept.employees.clear();
			dept.employees.add( session.find( Employee.class, 11L ) );
			dept.employees.add( e3 );
		} );
		revRecReplace = currentTxId;

		// --- Property update scenario ---

		// Rev 7: department + employee
		sf.inTransaction( session -> {
			var emp = new Employee( 20L, "Upd Alice" );
			session.persist( emp );
			var dept = new Department( 20L, "Upd Engineering" );
			dept.employees.add( emp );
			session.persist( dept );
		} );
		revUpdCreate = currentTxId;

		// Rev 8: update employee name (no collection change)
		sf.inTransaction( session -> {
			var emp = session.find( Employee.class, 20L );
			emp.name = "Upd Alice v2";
		} );
		revUpdMod = currentTxId;
	}

	// --- Write side verification ---

	@Test
	@Order(1)
	void testWriteSide(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Department: REV 1 (ADD) + REV 2 (collection change) + REV 3 (collection change) + REV 4 (DEL)
			var deptRevs = auditLog.getRevisions( Department.class, 1L );
			assertEquals( 4, deptRevs.size(),
					"Department should have 4 revisions (ADD + 2 collection changes + DEL)" );

			// Employees: only ADD revisions (FK changes tracked on parent side, not child)
			assertEquals( 1, auditLog.getRevisions( Employee.class, 1L ).size(),
					"Employee 1 should have 1 revision (ADD only)" );
			assertEquals( 1, auditLog.getRevisions( Employee.class, 2L ).size(),
					"Employee 2 should have 1 revision (ADD only)" );
		}
	}

	// --- Point-in-time reads ---

	@Test
	@Order(2)
	void testPointInTimeRead(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// At revCreate: department should have 1 employee (Alice)
		try (var s = sf.withOptions().atChangeset( revCreate ).openSession()) {
			var dept = s.find( Department.class, 1L );
			assertNotNull( dept );
			assertEquals( 1, dept.employees.size(), "At revCreate, department should have 1 employee" );
			assertEquals( "Alice", dept.employees.get( 0 ).name );
		}

		// At revAdd: department should have 2 employees
		try (var s = sf.withOptions().atChangeset( revAdd ).openSession()) {
			var dept = s.find( Department.class, 1L );
			assertNotNull( dept );
			assertEquals( 2, dept.employees.size(), "At revAdd, department should have 2 employees" );
			var names = dept.employees.stream().map( e -> e.name ).sorted().toList();
			assertEquals( List.of( "Alice", "Bob" ), names );
		}

		// At revRemove: department should have 1 employee (Bob only)
		try (var s = sf.withOptions().atChangeset( revRemove ).openSession()) {
			var dept = s.find( Department.class, 1L );
			assertNotNull( dept );
			assertEquals( 1, dept.employees.size(), "At revRemove, department should have 1 employee" );
			assertEquals( "Bob", dept.employees.get( 0 ).name );
		}
	}

	// --- getHistory ---

	@Test
	@Order(3)
	void testGetHistory(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Department.class, 1L );
			// Department: ADD + MOD (collection change) + MOD (collection change) + DEL = 4 revisions
			assertEquals( 4, history.size(), "Department should have 4 history entries" );
			assertEquals( "Engineering", history.get( 0 ).entity().name );
		}
	}

	// --- Recreate scenario ---

	@Test
	@Order(4)
	void testPointInTimeReadAfterRecreate(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// Department: ADD + recreate = 2 revisions (not more)
		try (var auditLog = AuditLogFactory.create( sf )) {
			assertEquals( 2, auditLog.getRevisions( Department.class, 10L ).size(),
					"Department should have exactly 2 revisions (ADD + recreate)" );
		}

		// At revRecCreate: 2 employees
		try (var s = sf.withOptions().atChangeset( revRecCreate ).openSession()) {
			var dept = s.find( Department.class, 10L );
			assertNotNull( dept );
			assertEquals( 2, dept.employees.size(), "At revRecCreate, department should have 2 employees" );
		}

		// At revRecReplace: 2 employees (Bob + Charlie, Alice dropped)
		try (var s = sf.withOptions().atChangeset( revRecReplace ).openSession()) {
			var dept = s.find( Department.class, 10L );
			assertNotNull( dept );
			assertEquals( 2, dept.employees.size(), "At revRecReplace, department should have 2 employees" );
			var names = dept.employees.stream().map( e -> e.name ).sorted().toList();
			assertEquals( List.of( "Rec Bob", "Rec Charlie" ), names );
		}
	}

	// --- Property update ---

	@Test
	@Order(5)
	void testChildPropertyUpdate(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// Employee should have 2 revisions (ADD + MOD)
		try (var auditLog = AuditLogFactory.create( sf )) {
			assertEquals( 2, auditLog.getRevisions( Employee.class, 20L ).size(),
					"Employee should have 2 revisions (ADD + property update)" );
			// Department: only 1 revision (initial persist), no collection change
			assertEquals( 1, auditLog.getRevisions( Department.class, 20L ).size(),
					"Department should still have 1 revision" );
		}

		// Point-in-time: employee name should reflect the update
		try (var s = sf.withOptions().atChangeset( revUpdMod ).openSession()) {
			var dept = s.find( Department.class, 20L );
			assertNotNull( dept );
			assertEquals( 1, dept.employees.size() );
			assertEquals( "Upd Alice v2", dept.employees.get( 0 ).name );
		}
	}

	// --- ALL_REVISIONS collection isolation ---

	@Test
	@Order(6)
	void testCollectionRevisionIsolation(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();
		try (var s = sf.withOptions().atChangeset( AuditLog.ALL_REVISIONS ).openSession()) {
			var departments = s.createSelectionQuery( "from Department where id = :id", Department.class )
					.setParameter( "id", 1L )
					.getResultList();
			// revCreate(1 emp) + revAdd(2 emps) + revRemove(1 emp) + revDelete(DEL) = 4 revisions
			assertEquals( 4, departments.size(), "Expected 4 revisions including DEL" );

			// Find revisions with different collection sizes
			Department deptWith2 = null;
			Department deptWith1 = null;
			for ( var d : departments ) {
				int size = d.employees.size();
				if ( size == 2 && deptWith2 == null ) {
					deptWith2 = d;
				}
				else if ( size == 1 && deptWith1 == null ) {
					deptWith1 = d;
				}
			}
			assertNotNull( deptWith2, "Should find a revision with 2 employees" );
			assertNotNull( deptWith1, "Should find a revision with 1 employee" );

			// Collections must be distinct instances across revisions
			assertNotSame( deptWith1.employees, deptWith2.employees,
					"Collections at different revisions must not be the same instance" );

			// Verify contents
			assertEquals( 2, deptWith2.employees.size() );
			assertEquals( 1, deptWith1.employees.size() );
		}
	}

	// ---- Entity classes ----

	@Audited
	@Entity(name = "Department")
	static class Department {
		@Id
		long id;
		String name;
		@OneToMany
		@JoinColumn(name = "department_id")
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
