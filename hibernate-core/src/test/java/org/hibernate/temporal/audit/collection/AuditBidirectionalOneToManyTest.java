/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit.collection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Audited;
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

/**
 * Tests bidirectional @OneToMany (mappedBy) auditing.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditBidirectionalOneToManyTest.Parent.class,
		AuditBidirectionalOneToManyTest.Child.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.collection.AuditBidirectionalOneToManyTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditBidirectionalOneToManyTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	// Shared lifecycle: Parent(1) + Child(1) + Child(2), update, remove
	private int revCreate;   // Parent(1) + Child(1, "Child A")
	private int revAddChild; // add Child(2, "Child B") + update Child(1) name
	private int revRemove;   // remove Child(1)

	// Recreate scenario: Parent(10) + children, bulk recreate
	private int revRecCreate;  // Parent(10) + Child(10, "A") + Child(11, "B")
	private int revRecReplace; // remove Child(10), add Child(12, "C")

	// Property update scenario
	private int revUpdCreate; // Parent(20) + Child(20)
	private int revUpdMod;    // update Child(20) name only

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		currentTxId = 0;
		final var sf = scope.getSessionFactory();

		// --- Shared lifecycle ---

		// Rev 1: parent + child A
		sf.inTransaction( session -> {
			var parent = new Parent( 1L, "Parent" );
			var child = new Child( 1L, "Child A", parent );
			parent.children.add( child );
			session.persist( parent );
			session.persist( child );
		} );
		revCreate = currentTxId;

		// Rev 2: add child B + update child A name
		sf.inTransaction( session -> {
			var parent = session.find( Parent.class, 1L );
			session.find( Child.class, 1L ).name = "Child A v2";
			var childB = new Child( 2L, "Child B", parent );
			parent.children.add( childB );
			session.persist( childB );
		} );
		revAddChild = currentTxId;

		// Rev 3: remove child A
		sf.inTransaction( session -> {
			var parent = session.find( Parent.class, 1L );
			var child = session.find( Child.class, 1L );
			parent.children.remove( child );
			session.remove( child );
		} );
		revRemove = currentTxId;

		// --- Recreate scenario ---

		// Rev 4: parent with child A + B
		sf.inTransaction( session -> {
			var parent = new Parent( 10L, "Rec Parent" );
			var childA = new Child( 10L, "Rec Child A", parent );
			var childB = new Child( 11L, "Rec Child B", parent );
			parent.children.add( childA );
			parent.children.add( childB );
			session.persist( parent );
			session.persist( childA );
			session.persist( childB );
		} );
		revRecCreate = currentTxId;

		// Rev 5: drop A, add C
		sf.inTransaction( session -> {
			var parent = session.find( Parent.class, 10L );
			var childA = session.find( Child.class, 10L );
			parent.children.remove( childA );
			session.remove( childA );
			var childC = new Child( 12L, "Rec Child C", parent );
			parent.children.add( childC );
			session.persist( childC );
		} );
		revRecReplace = currentTxId;

		// --- Property update scenario ---

		// Rev 6: parent + child
		sf.inTransaction( session -> {
			var parent = new Parent( 20L, "Upd Parent" );
			var child = new Child( 20L, "Upd Child A", parent );
			parent.children.add( child );
			session.persist( parent );
			session.persist( child );
		} );
		revUpdCreate = currentTxId;

		// Rev 7: update child name only (no collection membership change)
		sf.inTransaction( session ->
				session.find( Child.class, 20L ).name = "Upd Child A v2"
		);
		revUpdMod = currentTxId;
	}

	// --- Write side verification ---

	@Test
	@Order(1)
	void testWriteSideRevisionCounts(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Parent is inverse side: only 1 revision (initial persist)
			assertEquals( 1, auditLog.getChangesets( Parent.class, 1L ).size() );
			// Child A: ADD + MOD (name update) + DEL
			assertEquals( 3, auditLog.getChangesets( Child.class, 1L ).size() );
			// Child B: ADD only
			assertEquals( 1, auditLog.getChangesets( Child.class, 2L ).size() );
		}
	}

	// --- Point-in-time reads ---

	@Test
	@Order(2)
	void testPointInTimeRead(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// At revCreate: parent has 1 child (A)
		try (var s = sf.withOptions().atChangeset( revCreate ).openSession()) {
			var parent = s.find( Parent.class, 1L );
			assertNotNull( parent );
			assertEquals( 1, parent.children.size() );
			assertEquals( "Child A", parent.children.get( 0 ).name );
		}

		// At revAddChild: parent has 2 children (A v2 + B)
		try (var s = sf.withOptions().atChangeset( revAddChild ).openSession()) {
			var parent = s.find( Parent.class, 1L );
			assertNotNull( parent );
			assertEquals( 2, parent.children.size() );
			var names = parent.children.stream().map( c -> c.name ).sorted().toList();
			assertEquals( List.of( "Child A v2", "Child B" ), names );
		}

		// At revRemove: parent has 1 child (B only)
		try (var s = sf.withOptions().atChangeset( revRemove ).openSession()) {
			var parent = s.find( Parent.class, 1L );
			assertNotNull( parent );
			assertEquals( 1, parent.children.size() );
			assertEquals( "Child B", parent.children.get( 0 ).name );
		}
	}

	// --- getHistory ---

	@Test
	@Order(3)
	void testGetHistory(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Child A: 3 revisions (ADD, MOD, DEL)
			var history = auditLog.getHistory( Child.class, 1L );
			assertEquals( 3, history.size() );
			assertEquals( "Child A", history.get( 0 ).entity().name );
			assertNotNull( history.get( 0 ).entity().parent );
			assertEquals( "Parent", history.get( 0 ).entity().parent.name );
			assertEquals( "Child A v2", history.get( 1 ).entity().name );
			assertNotNull( history.get( 2 ).entity() );

			// Parent: only 1 revision (inverse side)
			assertEquals( 1, auditLog.getHistory( Parent.class, 1L ).size() );
		}
	}

	// --- Recreate scenario ---

	@Test
	@Order(4)
	void testPointInTimeReadAfterRecreate(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var auditLog = AuditLogFactory.create( sf )) {
			assertEquals( 1, auditLog.getChangesets( Parent.class, 10L ).size(),
					"Parent should have 1 revision (inverse side)" );
		}

		// At revRecCreate: 2 children (A + B)
		try (var s = sf.withOptions().atChangeset( revRecCreate ).openSession()) {
			var parent = s.find( Parent.class, 10L );
			assertNotNull( parent );
			assertEquals( 2, parent.children.size() );
		}

		// At revRecReplace: 2 children (B + C, A removed)
		try (var s = sf.withOptions().atChangeset( revRecReplace ).openSession()) {
			var parent = s.find( Parent.class, 10L );
			assertNotNull( parent );
			assertEquals( 2, parent.children.size() );
			var names = parent.children.stream().map( c -> c.name ).sorted().toList();
			assertEquals( List.of( "Rec Child B", "Rec Child C" ), names );
		}
	}

	// --- Property update ---

	@Test
	@Order(5)
	void testChildPropertyUpdate(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var auditLog = AuditLogFactory.create( sf )) {
			assertEquals( 2, auditLog.getChangesets( Child.class, 20L ).size(),
					"Child should have 2 revisions (ADD + property update)" );
			assertEquals( 1, auditLog.getChangesets( Parent.class, 20L ).size(),
					"Parent should still have 1 revision" );
		}

		try (var s = sf.withOptions().atChangeset( revUpdMod ).openSession()) {
			var parent = s.find( Parent.class, 20L );
			assertNotNull( parent );
			assertEquals( 1, parent.children.size() );
			assertEquals( "Upd Child A v2", parent.children.get( 0 ).name );
		}
	}

	// ---- Entity classes ----

	@Audited
	@Entity(name = "Parent")
	static class Parent {
		@Id
		long id;
		String name;
		@OneToMany(mappedBy = "parent")
		List<Child> children = new ArrayList<>();

		Parent() {
		}

		Parent(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Audited
	@Entity(name = "Child")
	static class Child {
		@Id
		long id;
		String name;
		@ManyToOne
		Parent parent;

		Child() {
		}

		Child(long id, String name, Parent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
		}
	}
}
