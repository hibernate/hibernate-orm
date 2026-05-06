/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;
import org.hibernate.cfg.StateManagementSettings;

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

import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link AuditLog} service API.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditLogTest.AuditedEntity.class,
		AuditLogTest.NonAuditedEntity.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditLogTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditLogTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}

	}

	@Audited
	@Entity(name = "AuditedEntity")
	static class AuditedEntity {
		@Id
		long id;
		String name;
	}

	@Entity(name = "NonAuditedEntity")
	static class NonAuditedEntity {
		@Id
		long id;
		String data;
	}

	private int revCreate1;
	private int revCreate2;
	private int revUpdate;
	private int revDelete;

	@BeforeClassTemplate
	void setupData(SessionFactoryScope scope) {
		currentTxId = 0;

		// Rev 1: create entity id=1
		scope.getSessionFactory().inTransaction( session -> {
			final var e = new AuditedEntity();
			e.id = 1L;
			e.name = "first";
			session.persist( e );
		} );
		revCreate1 = currentTxId;

		// Rev 2: create entity id=2 + update entity id=1
		scope.getSessionFactory().inTransaction( session -> {
			final var e2 = new AuditedEntity();
			e2.id = 2L;
			e2.name = "second";
			session.persist( e2 );

			final var e1 = session.find( AuditedEntity.class, 1L );
			e1.name = "first-updated";
		} );
		revCreate2 = currentTxId;

		// Rev 3: update entity id=2
		scope.getSessionFactory().inTransaction( session -> {
			final var e = session.find( AuditedEntity.class, 2L );
			e.name = "second-updated";
		} );
		revUpdate = currentTxId;

		// Rev 4: delete entity id=1
		scope.getSessionFactory().inTransaction( session -> {
			final var e = session.find( AuditedEntity.class, 1L );
			session.remove( e );
		} );
		revDelete = currentTxId;

		// Also persist a non-audited entity
		scope.getSessionFactory().inTransaction( session -> {
			final var e = new NonAuditedEntity();
			e.id = 100L;
			e.data = "not tracked";
			session.persist( e );
		} );
	}

	// --- isAudited ---

	@Test
	@Order(1)
	void testIsAudited(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertTrue( auditLog.isAudited( AuditedEntity.class ) );
			assertFalse( auditLog.isAudited( NonAuditedEntity.class ) );
		}
	}

	// --- getChangesets ---

	@Test
	@Order(2)
	void testGetRevisionsForEntity1(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getChangesets( AuditedEntity.class, 1L );

			// Entity 1 was: created (rev1), updated (rev2), deleted (rev4)
			assertEquals( 3, revisions.size() );
			assertEquals( revCreate1, revisions.get( 0 ) );
			assertEquals( revCreate2, revisions.get( 1 ) );
			assertEquals( revDelete, revisions.get( 2 ) );
		}
	}

	@Test
	@Order(3)
	void testGetRevisionsForEntity2(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getChangesets( AuditedEntity.class, 2L );

			// Entity 2 was: created (rev2), updated (rev3)
			assertEquals( 2, revisions.size() );
			assertEquals( revCreate2, revisions.get( 0 ) );
			assertEquals( revUpdate, revisions.get( 1 ) );
		}
	}

	@Test
	@Order(4)
	void testGetRevisionsChronologicalOrder(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getChangesets( AuditedEntity.class, 1L );

			// Should be in ascending order
			for ( int i = 1; i < revisions.size(); i++ ) {
				assertTrue(
						( (Comparable) revisions.get( i - 1 ) ).compareTo( revisions.get( i ) ) < 0,
						"Revisions should be in ascending order"
				);
			}
		}
	}

	@Test
	@Order(5)
	void testGetRevisionsForNonExistentEntity(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getChangesets( AuditedEntity.class, 999L );
			assertTrue( revisions.isEmpty() );
		}
	}

	// --- getModificationType ---

	@Test
	@Order(6)
	void testGetModificationTypeAdd(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals(
					ModificationType.ADD,
					auditLog.getModificationType( AuditedEntity.class, 1L, revCreate1 )
			);
			assertEquals(
					ModificationType.ADD,
					auditLog.getModificationType( AuditedEntity.class, 2L, revCreate2 )
			);
		}
	}

	@Test
	@Order(7)
	void testGetModificationTypeMod(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Entity 1 was also updated in rev2
			assertEquals(
					ModificationType.MOD,
					auditLog.getModificationType( AuditedEntity.class, 1L, revCreate2 )
			);
			assertEquals(
					ModificationType.MOD,
					auditLog.getModificationType( AuditedEntity.class, 2L, revUpdate )
			);
		}
	}

	@Test
	@Order(8)
	void testGetModificationTypeDel(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals(
					ModificationType.DEL,
					auditLog.getModificationType( AuditedEntity.class, 1L, revDelete )
			);
		}
	}

	@Test
	@Order(9)
	void testGetModificationTypeReturnsNullForUnmodified(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Entity 2 was not modified at rev1
			assertNull( auditLog.getModificationType( AuditedEntity.class, 2L, revCreate1 ) );
		}
	}

	@Test
	@Order(10)
	void testGetModificationTypeReturnsNullForNonExistentEntity(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertNull( auditLog.getModificationType( AuditedEntity.class, 999L, revCreate1 ) );
		}
	}

	// --- findEntitiesModifiedAt with ModificationType ---

	@Test
	@Order(11)
	void testFindEntitiesModifiedAtWithModificationType(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 1: entity 1 was ADD
			final var adds = auditLog.findEntitiesModifiedAt( AuditedEntity.class, revCreate1, ModificationType.ADD );
			assertEquals( 1, adds.size() );
			assertEquals( "first", adds.get( 0 ).name );

			// No MODs or DELs in rev 1
			assertTrue( auditLog.findEntitiesModifiedAt( AuditedEntity.class, revCreate1, ModificationType.MOD ).isEmpty() );
			assertTrue( auditLog.findEntitiesModifiedAt( AuditedEntity.class, revCreate1, ModificationType.DEL ).isEmpty() );
		}
	}

	@Test
	@Order(12)
	void testFindEntitiesModifiedAtWithDelType(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var dels = auditLog.findEntitiesModifiedAt( AuditedEntity.class, revDelete, ModificationType.DEL );
			assertEquals( 1, dels.size() );
		}
	}

	// --- findEntitiesGroupedByModificationType ---

	@Test
	@Order(13)
	void testFindEntitiesGroupedByModificationType(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 2: entity 1 was MOD, entity 2 was ADD
			final var grouped = auditLog.findEntitiesGroupedByModificationType( AuditedEntity.class, revCreate2 );
			assertEquals( 1, grouped.get( ModificationType.ADD ).size() );
			assertEquals( "second", grouped.get( ModificationType.ADD ).get( 0 ).name );
			assertEquals( 1, grouped.get( ModificationType.MOD ).size() );
			assertEquals( "first-updated", grouped.get( ModificationType.MOD ).get( 0 ).name );
			assertTrue( grouped.get( ModificationType.DEL ).isEmpty() );
		}
	}

	// --- empty revisions ---

	@Test
	@Order(14)
	void testNoEmptyRevisionsForNonAuditedChanges(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// The non-audited entity was persisted in a separate transaction
			// but should NOT have created any audit rows
			final var revisions1 = auditLog.getChangesets( AuditedEntity.class, 1L );
			final var revisions2 = auditLog.getChangesets( AuditedEntity.class, 2L );

			// No revision should exist beyond revDelete for entity 1
			// or beyond revUpdate for entity 2
			for ( var rev : revisions1 ) {
				assertTrue( ( (Number) rev ).intValue() <= revDelete );
			}
			for ( var rev : revisions2 ) {
				assertTrue( ( (Number) rev ).intValue() <= revUpdate );
			}
		}
	}

	// --- atTransaction + AuditLog combined ---

	@Test
	@Order(15)
	void testAuditLogWithAtTransactionReads(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var sf = scope.getSessionFactory();

			// Use AuditLog to get revisions, then read entity state via atTransaction
			final var revisions = auditLog.getChangesets( AuditedEntity.class, 1L );
			assertEquals( 3, revisions.size() );

			// First revision: entity was created with name "first"
			final int firstRev = ( (Number) revisions.get( 0 ) ).intValue();
			assertEquals( ModificationType.ADD, auditLog.getModificationType( AuditedEntity.class, 1L, firstRev ) );
			try (var s = sf.withOptions().atChangeset( firstRev ).open()) {
				final var entity = s.find( AuditedEntity.class, 1L );
				assertNotNull( entity );
				assertEquals( "first", entity.name );
			}

			// Second revision: entity was updated to "first-updated"
			final int secondRev = ( (Number) revisions.get( 1 ) ).intValue();
			assertEquals( ModificationType.MOD, auditLog.getModificationType( AuditedEntity.class, 1L, secondRev ) );
			try (var s = sf.withOptions().atChangeset( secondRev ).open()) {
				final var entity = s.find( AuditedEntity.class, 1L );
				assertNotNull( entity );
				assertEquals( "first-updated", entity.name );
			}

			// Third revision: entity was deleted
			final int thirdRev = ( (Number) revisions.get( 2 ) ).intValue();
			assertEquals( ModificationType.DEL, auditLog.getModificationType( AuditedEntity.class, 1L, thirdRev ) );
			try (var s = sf.withOptions().atChangeset( thirdRev ).open()) {
				final var entity = s.find( AuditedEntity.class, 1L );
				assertNull( entity );
			}
		}
	}

	// --- convenience methods: find + findEntitiesModifiedAt ---

	@Test
	@Order(16)
	void testFindAtRevision(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// At revCreate1: entity 1 was just created
			final var entity1 = auditLog.find( AuditedEntity.class, 1L, revCreate1 );
			assertNotNull( entity1 );
			assertEquals( "first", entity1.name );

			// At revCreate2: entity 1 was updated
			final var entity1Updated = auditLog.find( AuditedEntity.class, 1L, revCreate2 );
			assertNotNull( entity1Updated );
			assertEquals( "first-updated", entity1Updated.name );

			// At revCreate2: entity 2 was created
			final var entity2 = auditLog.find( AuditedEntity.class, 2L, revCreate2 );
			assertNotNull( entity2 );
			assertEquals( "second", entity2.name );

			// At revDelete: entity 1 was deleted
			final var deleted = auditLog.find( AuditedEntity.class, 1L, revDelete );
			assertNull( deleted );
		}
	}

	@Test
	@Order(16)
	void testFindAtRevisionWhereEntityNotModified(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Entity 2 was NOT modified at revDelete (only entity 1 was deleted),
			// but it was last modified at revUpdate. find() should return the
			// most recent snapshot at or before the given revision.
			final var entity2 = auditLog.find( AuditedEntity.class, 2L, revDelete );
			assertNotNull( entity2 );
			assertEquals( "second-updated", entity2.name );

			// Entity 1 was NOT modified at revUpdate (only entity 2 was updated),
			// but it was last modified at revCreate2.
			final var entity1 = auditLog.find( AuditedEntity.class, 1L, revUpdate );
			assertNotNull( entity1 );
			assertEquals( "first-updated", entity1.name );

			// Entity 2 did not exist at revCreate1, should return null
			final var notYet = auditLog.find( AuditedEntity.class, 2L, revCreate1 );
			assertNull( notYet );
		}
	}

	@Test
	@Order(17)
	void testFindEntitiesModifiedAtSingleEntity(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 1: only entity 1 was created
			final var modified = auditLog.findEntitiesModifiedAt( AuditedEntity.class, revCreate1 );
			assertEquals( 1, modified.size() );
			assertEquals( "first", modified.get( 0 ).name );
		}
	}

	@Test
	@Order(18)
	void testFindEntitiesModifiedAtMultipleEntities(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 2: entity 2 created + entity 1 updated = 2 entities
			final var modified = auditLog.findEntitiesModifiedAt( AuditedEntity.class, revCreate2 );
			assertEquals( 2, modified.size() );
			final var names = modified.stream().map( e -> e.name ).sorted().toList();
			assertEquals( List.of( "first-updated", "second" ), names );
		}
	}

	@Test
	@Order(19)
	void testFindEntitiesModifiedAtIncludesDeleted(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Rev 4: entity 1 was deleted, should be included in results
			final var modified = auditLog.findEntitiesModifiedAt( AuditedEntity.class, revDelete );
			assertEquals( 1, modified.size() );
		}
	}

	@Test
	@Order(20)
	void testFindEntitiesModifiedAtNoResults(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var modified = auditLog.findEntitiesModifiedAt( AuditedEntity.class, 999 );
			assertTrue( modified.isEmpty() );
		}
	}

	// --- getHistory ---

	@Test
	@Order(21)
	void testGetHistoryIncludesAllModificationTypes(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Entity 1: created (rev1), updated (rev2), deleted (rev4)
			final var history = auditLog.getHistory( AuditedEntity.class, 1L );
			assertEquals( 3, history.size() );

			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertEquals( ModificationType.MOD, history.get( 1 ).modificationType() );
			assertEquals( ModificationType.DEL, history.get( 2 ).modificationType() );
		}
	}

	@Test
	@Order(22)
	void testGetHistoryEntityStateAtEachRevision(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var history = auditLog.getHistory( AuditedEntity.class, 1L );

			// ADD: entity was created with "first"
			assertEquals( "first", history.get( 0 ).entity().name );

			// MOD: entity was updated to "first-updated"
			assertEquals( "first-updated", history.get( 1 ).entity().name );

			// DEL: entity snapshot preserves the last state before deletion
			assertNotNull( history.get( 2 ).entity() );
			assertEquals( "first-updated", history.get( 2 ).entity().name );
		}
	}

	@Test
	@Order(23)
	void testGetHistoryRevisionValues(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var history = auditLog.getHistory( AuditedEntity.class, 1L );

			// Without a ChangelogSupplier, revision should be the plain txId
			assertEquals( revCreate1, history.get( 0 ).changeset() );
			assertEquals( revCreate2, history.get( 1 ).changeset() );
			assertEquals( revDelete, history.get( 2 ).changeset() );
		}
	}

	@Test
	@Order(24)
	void testGetHistoryForEntityWithNoDeletes(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Entity 2: created (rev2), updated (rev3), no delete
			final var history = auditLog.getHistory( AuditedEntity.class, 2L );
			assertEquals( 2, history.size() );

			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertEquals( "second", history.get( 0 ).entity().name );

			assertEquals( ModificationType.MOD, history.get( 1 ).modificationType() );
			assertEquals( "second-updated", history.get( 1 ).entity().name );
		}
	}

	@Test
	@Order(25)
	void testGetHistoryForNonExistentEntity(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var history = auditLog.getHistory( AuditedEntity.class, 999L );
			assertTrue( history.isEmpty() );
		}
	}
}
