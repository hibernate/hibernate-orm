/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.collection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
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
 * Tests unidirectional @ManyToMany auditing (join table).
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditManyToManyTest.Student.class,
		AuditManyToManyTest.Course.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.collection.AuditManyToManyTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditManyToManyTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	// Shared lifecycle: Student(1) + courses, add/remove/delete
	private int revCreate;  // Student(1) + Course(1, "Math")
	private int revAdd;     // add Course(2, "Physics")
	private int revDrop;    // drop Course(1)
	private int revDelete;  // delete Student(1)

	// Recreate scenario (IDs 10-19)
	private int revRecCreate;  // Student(10) + Course(10)+Course(11)
	private int revRecReplace; // clear, re-add Course(11) + new Course(12)

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		currentTxId = 0;
		final var sf = scope.getSessionFactory();

		// --- Shared lifecycle ---

		// Rev 1: student enrolled in one course
		sf.inTransaction( session -> {
			var course = new Course( 1L, "Math" );
			session.persist( course );
			var student = new Student( 1L, "Alice" );
			student.courses.add( course );
			session.persist( student );
		} );
		revCreate = currentTxId;

		// Rev 2: enroll in second course
		sf.inTransaction( session -> {
			var course = new Course( 2L, "Physics" );
			session.persist( course );
			var student = session.find( Student.class, 1L );
			student.courses.add( course );
		} );
		revAdd = currentTxId;

		// Rev 3: drop first course
		sf.inTransaction( session -> {
			var student = session.find( Student.class, 1L );
			student.courses.removeIf( c -> c.id == 1L );
		} );
		revDrop = currentTxId;

		// Rev 4: delete student (bulk removal of collection)
		sf.inTransaction( session -> {
			var student = session.find( Student.class, 1L );
			session.remove( student );
		} );
		revDelete = currentTxId;

		// --- Recreate scenario ---

		// Rev 5: student enrolled in Math + Physics
		sf.inTransaction( session -> {
			var c1 = new Course( 10L, "Rec Math" );
			var c2 = new Course( 11L, "Rec Physics" );
			session.persist( c1 );
			session.persist( c2 );
			var student = new Student( 10L, "Rec Alice" );
			student.courses.add( c1 );
			student.courses.add( c2 );
			session.persist( student );
		} );
		revRecCreate = currentTxId;

		// Rev 6: recreate: clear and re-add only Physics + new Chemistry
		sf.inTransaction( session -> {
			var c3 = new Course( 12L, "Rec Chemistry" );
			session.persist( c3 );
			var student = session.find( Student.class, 10L );
			student.courses.clear();
			student.courses.add( session.find( Course.class, 11L ) );
			student.courses.add( c3 );
		} );
		revRecReplace = currentTxId;
	}

	// --- Write side verification ---

	@Test
	@Order(1)
	void testWriteSide(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Student: ADD + 2 collection changes + DEL = 4 revisions
			assertEquals( 4, auditLog.getRevisions( Student.class, 1L ).size(),
					"Student should have 4 revisions (ADD + 2 collection changes + DEL)" );
			assertEquals( 1, auditLog.getRevisions( Course.class, 1L ).size() );
			assertEquals( 1, auditLog.getRevisions( Course.class, 2L ).size() );
		}
	}

	// --- Point-in-time reads ---

	@Test
	@Order(2)
	void testPointInTimeRead(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// At revCreate: 1 course
		try (var s = sf.withOptions().atChangeset( revCreate ).openSession()) {
			var student = s.find( Student.class, 1L );
			assertNotNull( student );
			assertEquals( 1, student.courses.size(), "At revCreate, student should have 1 course" );
			assertEquals( "Math", student.courses.get( 0 ).name );
		}

		// At revAdd: 2 courses
		try (var s = sf.withOptions().atChangeset( revAdd ).openSession()) {
			var student = s.find( Student.class, 1L );
			assertNotNull( student );
			assertEquals( 2, student.courses.size(), "At revAdd, student should have 2 courses" );
		}

		// At revDrop: 1 course (Physics only)
		try (var s = sf.withOptions().atChangeset( revDrop ).openSession()) {
			var student = s.find( Student.class, 1L );
			assertNotNull( student );
			assertEquals( 1, student.courses.size(), "At revDrop, student should have 1 course" );
			assertEquals( "Physics", student.courses.get( 0 ).name );
		}
	}

	// --- getHistory ---

	@Test
	@Order(3)
	void testGetHistory(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Student.class, 1L );
			assertEquals( 4, history.size(), "Student has ADD + 2 collection changes + DEL" );
			assertEquals( "Alice", history.get( 0 ).entity().name );
		}
	}

	// --- Recreate scenario ---

	@Test
	@Order(4)
	void testPointInTimeReadAfterRecreate(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// Student: ADD + recreate = 2 revisions (not more)
		try (var auditLog = AuditLogFactory.create( sf )) {
			assertEquals( 2, auditLog.getRevisions( Student.class, 10L ).size(),
					"Student should have exactly 2 revisions (ADD + recreate)" );
		}

		// At revRecCreate: 2 courses (Math + Physics)
		try (var s = sf.withOptions().atChangeset( revRecCreate ).openSession()) {
			var student = s.find( Student.class, 10L );
			assertNotNull( student );
			assertEquals( 2, student.courses.size(), "At revRecCreate, student should have 2 courses" );
		}

		// At revRecReplace: 2 courses (Physics + Chemistry, Math dropped)
		try (var s = sf.withOptions().atChangeset( revRecReplace ).openSession()) {
			var student = s.find( Student.class, 10L );
			assertNotNull( student );
			assertEquals( 2, student.courses.size(), "At revRecReplace, student should have 2 courses" );
			var names = student.courses.stream().map( c -> c.name ).sorted().toList();
			assertEquals( List.of( "Rec Chemistry", "Rec Physics" ), names );
		}
	}

	// --- ALL_REVISIONS collection isolation ---

	@Test
	@Order(5)
	void testCollectionRevisionIsolation(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();
		try (var s = sf.withOptions().atChangeset( AuditLog.ALL_CHANGESETS ).openSession()) {
			var students = s.createSelectionQuery( "from Student where id = :id", Student.class )
					.setParameter( "id", 1L )
					.getResultList();
			// revCreate(1 course) + revAdd(2 courses) + revDrop(1 course) + revDelete(DEL) = 4 revisions
			assertEquals( 4, students.size(), "Expected 4 revisions including DEL" );

			// Find revisions with different collection sizes
			Student studentWith2 = null;
			Student studentWith1 = null;
			for ( var st : students ) {
				int size = st.courses.size();
				if ( size == 2 && studentWith2 == null ) {
					studentWith2 = st;
				}
				else if ( size == 1 && studentWith1 == null ) {
					studentWith1 = st;
				}
			}
			assertNotNull( studentWith2, "Should find a revision with 2 courses" );
			assertNotNull( studentWith1, "Should find a revision with 1 course" );

			// Collections must be distinct instances across revisions
			assertNotSame( studentWith1.courses, studentWith2.courses,
					"Collections at different revisions must not be the same instance" );

			// Verify contents
			assertEquals( 2, studentWith2.courses.size() );
			assertEquals( 1, studentWith1.courses.size() );
		}
	}

	// ---- Entity classes ----

	@Audited
	@Entity(name = "Student")
	static class Student {
		@Id
		long id;
		String name;
		@ManyToMany
		List<Course> courses = new ArrayList<>();

		Student() {
		}

		Student(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Audited
	@Entity(name = "Course")
	static class Course {
		@Id
		long id;
		String name;

		Course() {
		}

		Course(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
