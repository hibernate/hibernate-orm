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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Tests bidirectional @ManyToMany auditing.
 * The owning side's collection changes are tracked; the inverse (mappedBy) side
 * does NOT get extra MOD revisions for relationship changes.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditBidirectionalManyToManyTest.OwningEntity.class,
		AuditBidirectionalManyToManyTest.OwnedEntity.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.collection.AuditBidirectionalManyToManyTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditBidirectionalManyToManyTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	// Shared lifecycle: ing1 + ing2 with ed1 + ed2, add/remove references, clear
	private int revCreate;     // ed1, ed2, ing1, ing2
	private int revLink;       // ing1={ed1}, ing2={ed1,ed2}
	private int revAddRef;     // ing1.add(ed2)
	private int revRemoveRef;  // ing1.remove(ed1)
	private int revClear;      // ing1.clear()

	// Recreate scenario: ing(10) with ed(10)+ed(11), then recreate
	private int revRecCreate;  // ing(10) with ed(10)+ed(11)
	private int revRecReplace; // clear, re-add ed(11)+new ed(12)

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		currentTxId = 0;
		final var sf = scope.getSessionFactory();

		// --- Shared lifecycle ---

		// Rev 1: create all entities, no collection changes
		sf.inTransaction( session -> {
			session.persist( new OwnedEntity( 1, "ed1" ) );
			session.persist( new OwnedEntity( 2, "ed2" ) );
			session.persist( new OwningEntity( 3, "ing1" ) );
			session.persist( new OwningEntity( 4, "ing2" ) );
		} );
		revCreate = currentTxId;

		// Rev 2: ing1={ed1}, ing2={ed1,ed2}
		sf.inTransaction( session -> {
			var ing1 = session.find( OwningEntity.class, 3 );
			var ing2 = session.find( OwningEntity.class, 4 );
			var ed1 = session.find( OwnedEntity.class, 1 );
			var ed2 = session.find( OwnedEntity.class, 2 );
			ing1.references.add( ed1 );
			ing2.references.add( ed1 );
			ing2.references.add( ed2 );
		} );
		revLink = currentTxId;

		// Rev 3: ing1.add(ed2)
		sf.inTransaction( session -> {
			var ing1 = session.find( OwningEntity.class, 3 );
			var ed2 = session.find( OwnedEntity.class, 2 );
			ing1.references.add( ed2 );
		} );
		revAddRef = currentTxId;

		// Rev 4: ing1.remove(ed1)
		sf.inTransaction( session -> {
			var ing1 = session.find( OwningEntity.class, 3 );
			ing1.references.removeIf( e -> e.id == 1 );
		} );
		revRemoveRef = currentTxId;

		// Rev 5: ing1 clears all references
		sf.inTransaction( session -> {
			var ing1 = session.find( OwningEntity.class, 3 );
			ing1.references.clear();
		} );
		revClear = currentTxId;

		// --- Recreate scenario ---

		// Rev 6: ing with ed1 + ed2
		sf.inTransaction( session -> {
			session.persist( new OwnedEntity( 10, "rec ed1" ) );
			session.persist( new OwnedEntity( 11, "rec ed2" ) );
			var ing = new OwningEntity( 12, "rec ing" );
			ing.references.add( session.find( OwnedEntity.class, 10 ) );
			ing.references.add( session.find( OwnedEntity.class, 11 ) );
			session.persist( ing );
		} );
		revRecCreate = currentTxId;

		// Rev 7: recreate: clear and re-add ed2 + new ed3
		sf.inTransaction( session -> {
			session.persist( new OwnedEntity( 13, "rec ed3" ) );
			var ing = session.find( OwningEntity.class, 12 );
			ing.references.clear();
			ing.references.add( session.find( OwnedEntity.class, 11 ) );
			ing.references.add( session.find( OwnedEntity.class, 13 ) );
		} );
		revRecReplace = currentTxId;
	}

	// --- Write side verification ---

	@Test
	@Order(1)
	void testWriteSide(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Owning side: ing1 at [1, 2, 3, 4, 5], ing2 at [1, 2]
			assertEquals( 5, auditLog.getChangesets( OwningEntity.class, 3 ).size(),
					"ing1 should have 5 revisions" );
			assertEquals( 2, auditLog.getChangesets( OwningEntity.class, 4 ).size(),
					"ing2 should have 2 revisions" );

			// Inverse side: only ADD revisions, no MOD from relationship changes
			assertEquals( 1, auditLog.getChangesets( OwnedEntity.class, 1 ).size(),
					"ed1 should have 1 revision (ADD only)" );
			assertEquals( 1, auditLog.getChangesets( OwnedEntity.class, 2 ).size(),
					"ed2 should have 1 revision (ADD only)" );
		}
	}

	// --- Point-in-time reads ---

	@Test
	@Order(2)
	void testPointInTimeRead(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// At revLink: ing1 has 1 reference (ed1)
		try (var s = sf.withOptions().atChangeset( revLink ).openSession()) {
			var ing = s.find( OwningEntity.class, 3 );
			assertNotNull( ing );
			assertEquals( 1, ing.references.size(), "At revLink, ing1 should have 1 reference" );
		}

		// At revAddRef: ing1 has 2 references (ed1 + ed2)
		try (var s = sf.withOptions().atChangeset( revAddRef ).openSession()) {
			var ing = s.find( OwningEntity.class, 3 );
			assertNotNull( ing );
			assertEquals( 2, ing.references.size(), "At revAddRef, ing1 should have 2 references" );
		}

		// At revRemoveRef: ing1 has 1 reference (ed2 only)
		try (var s = sf.withOptions().atChangeset( revRemoveRef ).openSession()) {
			var ing = s.find( OwningEntity.class, 3 );
			assertNotNull( ing );
			assertEquals( 1, ing.references.size(), "At revRemoveRef, ing1 should have 1 reference" );
			assertEquals( "ed2", ing.references.iterator().next().data );
		}
	}

	// --- getHistory ---

	@Test
	@Order(3)
	void testGetHistory(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// ing1: 5 revisions (ADD + 3 collection changes + clear)
			var history = auditLog.getHistory( OwningEntity.class, 3 );
			assertEquals( 5, history.size() );
			assertEquals( "ing1", history.get( 0 ).entity().data );

			// Owned entities: only 1 revision each (ADD)
			assertEquals( 1, auditLog.getHistory( OwnedEntity.class, 1 ).size() );
			assertEquals( 1, auditLog.getHistory( OwnedEntity.class, 2 ).size() );
		}
	}

	// --- Recreate scenario ---

	@Test
	@Order(4)
	void testPointInTimeReadAfterRecreate(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// Owning entity: ADD + recreate = 2 revisions (not more)
		try (var auditLog = AuditLogFactory.create( sf )) {
			assertEquals( 2, auditLog.getChangesets( OwningEntity.class, 12 ).size(),
					"Owning entity should have exactly 2 revisions (ADD + recreate)" );
		}

		// At revRecCreate: 2 references
		try (var s = sf.withOptions().atChangeset( revRecCreate ).openSession()) {
			var ing = s.find( OwningEntity.class, 12 );
			assertNotNull( ing );
			assertEquals( 2, ing.references.size(), "At revRecCreate, should have 2 references" );
		}

		// At revRecReplace: 2 references (ed2 + ed3, ed1 dropped)
		try (var s = sf.withOptions().atChangeset( revRecReplace ).openSession()) {
			var ing = s.find( OwningEntity.class, 12 );
			assertNotNull( ing );
			assertEquals( 2, ing.references.size(), "At revRecReplace, should have 2 references" );
			var names = ing.references.stream().map( e -> e.data ).sorted().toList();
			assertEquals( List.of( "rec ed2", "rec ed3" ), names );
		}
	}

	// --- ALL_REVISIONS collection isolation ---

	@Test
	@Order(5)
	void testCollectionRevisionIsolation(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();
		try (var s = sf.withOptions().atChangeset( AuditLog.ALL_CHANGESETS ).openSession()) {
			var entities = s.createSelectionQuery( "from OwningEntity where id = :id", OwningEntity.class )
					.setParameter( "id", 3 )
					.getResultList();
			// revCreate(0) + revLink(1) + revAddRef(2) + revRemoveRef(1) + revClear(0) = 5 revisions
			assertEquals( 5, entities.size(), "Expected 5 revisions" );

			// Find revisions with different collection sizes
			OwningEntity entityWith2 = null;
			OwningEntity entityWith1 = null;
			for ( var e : entities ) {
				int size = e.references.size();
				if ( size == 2 && entityWith2 == null ) {
					entityWith2 = e;
				}
				else if ( size == 1 && entityWith1 == null ) {
					entityWith1 = e;
				}
			}
			assertNotNull( entityWith2, "Should find a revision with 2 references" );
			assertNotNull( entityWith1, "Should find a revision with 1 reference" );

			// Collections must be distinct instances across revisions
			assertNotSame( entityWith1.references, entityWith2.references,
					"Collections at different revisions must not be the same instance" );

			// Verify contents
			assertEquals( 2, entityWith2.references.size() );
			assertEquals( 1, entityWith1.references.size() );
		}
	}

	// ---- Entity classes ----

	@Audited
	@Entity(name = "OwningEntity")
	static class OwningEntity {
		@Id
		int id;
		String data;
		@ManyToMany
		Set<OwnedEntity> references = new HashSet<>();

		OwningEntity() {
		}

		OwningEntity(int id, String data) {
			this.id = id;
			this.data = data;
		}
	}

	@Audited
	@Entity(name = "OwnedEntity")
	static class OwnedEntity {
		@Id
		int id;
		String data;
		@ManyToMany(mappedBy = "references")
		Set<OwningEntity> referencing = new HashSet<>();

		OwnedEntity() {
		}

		OwnedEntity(int id, String data) {
			this.id = id;
			this.data = data;
		}
	}
}
