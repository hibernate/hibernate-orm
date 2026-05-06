/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.TransactionIdentifierSupplier;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditEntityTest.AuditEntity.class,
		AuditEntityTest.EmbeddedEntity.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TRANSACTION_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditEntityTest$TxIdSupplier"))
class AuditEntityTest {
	private static int currentTxId;

	public static class TxIdSupplier implements TransactionIdentifierSupplier<Integer> {
		@Override
		public Integer generateTransactionIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}

	}

	@Test
	void test(SessionFactoryScope scope) {
		currentTxId = 0;
		scope.getSessionFactory().inTransaction(
				session -> {
					AuditEntity entity = new AuditEntity();
					entity.id = 1L;
					entity.text = "hello";
					entity.stringSet.add( "hello" );
					session.persist( entity );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					AuditEntity entity = session.find( AuditEntity.class, 1L );
					entity.text = "goodbye";
					entity.stringSet.add( "goodbye" );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					AuditEntity entity = session.find( AuditEntity.class, 1L );
					session.remove( entity );
				}
		);
		scope.getSessionFactory().inTransaction(
				session -> {
					AuditEntity entity = session.find( AuditEntity.class, 1L );
					assertNull( entity );
				}
		);
		try (var s = scope.getSessionFactory().withOptions().atTransaction( 0 ).open()) {
			AuditEntity entity = s.find( AuditEntity.class, 1L );
			assertNull( entity );
			AuditEntity result =
					s.createSelectionQuery( "from AuditEntity where id = 1", AuditEntity.class )
							.getSingleResultOrNull();
			assertNull( result );
		}
		try (var s = scope.getSessionFactory().withOptions().atTransaction( 1 ).open()) {
			AuditEntity entity = s.find( AuditEntity.class, 1L );
			assertEquals( "hello", entity.text );
			assertEquals( Set.of( "hello" ), entity.stringSet );
			AuditEntity result =
					s.createSelectionQuery( "from AuditEntity where id = 1", AuditEntity.class )
							.getSingleResultOrNull();
			assertSame( entity, result );
		}
		try (var s = scope.getSessionFactory().withOptions().atTransaction( 2 ).open()) {
			AuditEntity entity = s.find( AuditEntity.class, 1L );
			assertEquals( "goodbye", entity.text );
			assertEquals( Set.of( "hello", "goodbye" ), entity.stringSet );
			AuditEntity result =
					s.createSelectionQuery( "from AuditEntity where id = 1", AuditEntity.class )
							.getSingleResultOrNull();
			assertSame( entity, result );
		}
		try (var s = scope.getSessionFactory().withOptions().atTransaction( 3 ).open()) {
			AuditEntity entity = s.find( AuditEntity.class, 1L );
			assertNull( entity );
			AuditEntity result =
					s.createSelectionQuery( "from AuditEntity where id = 1", AuditEntity.class )
							.getSingleResultOrNull();
			assertNull( result );
		}
		try (var s = scope.getSessionFactory().withOptions().atTransaction( 4 ).open()) {
			AuditEntity entity = s.find( AuditEntity.class, 1L );
			assertNull( entity );
			AuditEntity result =
					s.createSelectionQuery( "from AuditEntity where id = 1", AuditEntity.class )
							.getSingleResultOrNull();
			assertNull( result );
		}
	}

	/**
	 * Test that {@code atTransaction().find()} returns the most recent
	 * snapshot at or before the given revision, even when the entity
	 * was not modified at that exact revision.
	 */
	@Test
	void testFindAtNonModifiedRevision(SessionFactoryScope scope) {
		currentTxId = 700;
		final var sf = scope.getSessionFactory();

		// Rev 701: create entity A
		sf.inTransaction( session -> {
			var e = new AuditEntity();
			e.id = 701L;
			e.text = "A-created";
			session.persist( e );
		} );

		// Rev 702: create entity B (entity A is NOT modified)
		sf.inTransaction( session -> {
			var e = new AuditEntity();
			e.id = 702L;
			e.text = "B-created";
			session.persist( e );
		} );

		// Rev 703: update entity B (entity A is still NOT modified)
		sf.inTransaction( session -> {
			var e = session.find( AuditEntity.class, 702L );
			e.text = "B-updated";
		} );

		// Entity A was only modified at rev 701, but should be visible
		// at rev 702 and 703 with its original state
		try (var s = sf.withOptions().atTransaction( 702 ).open()) {
			var a = s.find( AuditEntity.class, 701L );
			assertEquals( "A-created", a.text );
		}
		try (var s = sf.withOptions().atTransaction( 703 ).open()) {
			var a = s.find( AuditEntity.class, 701L );
			assertEquals( "A-created", a.text );
		}

		// Entity B should not be visible at rev 701 (created later)
		try (var s = sf.withOptions().atTransaction( 701 ).open()) {
			var b = s.find( AuditEntity.class, 702L );
			assertNull( b );
		}
	}

	@Test
	void testEmbeddedAuditing(SessionFactoryScope scope) {
		currentTxId = 100;

		scope.getSessionFactory().inTransaction( session -> {
			var e = new EmbeddedEntity();
			e.id = 1L;
			e.name = "test";
			e.address = new Address( "123 Main St", "Springfield" );
			session.persist( e );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var e = session.find( EmbeddedEntity.class, 1L );
			e.address = new Address( "456 Oak Ave", "Shelbyville" );
		} );

		try (var s = scope.getSessionFactory().withOptions().atTransaction( 101 ).open()) {
			var e = s.find( EmbeddedEntity.class, 1L );
			assertEquals( "123 Main St", e.address.street );
			assertEquals( "Springfield", e.address.city );
		}

		try (var s = scope.getSessionFactory().withOptions().atTransaction( 102 ).open()) {
			var e = s.find( EmbeddedEntity.class, 1L );
			assertEquals( "456 Oak Ave", e.address.street );
			assertEquals( "Shelbyville", e.address.city );
		}
	}

	/**
	 * Test that multiple flushes within the same transaction produce
	 * a single audit row with the latest state (MOD+MOD -> MOD).
	 */
	@Test
	void testMultiFlushMerge(SessionFactoryScope scope) {
		currentTxId = 200;

		// Revision 201: create entity
		scope.getSessionFactory().inTransaction( session -> {
			var e = new AuditEntity();
			e.id = 99L;
			e.text = "initial";
			session.persist( e );
		} );

		// Revision 202: modify twice with explicit flush between
		scope.getSessionFactory().inTransaction( session -> {
			var e = session.find( AuditEntity.class, 99L );
			e.text = "first change";
			session.flush();
			e.text = "second change";
			// second flush happens at commit
		} );

		// Verify: revision 201 = ADD with "initial"
		try (var s = scope.getSessionFactory().withOptions().atTransaction( 201 ).open()) {
			var e = s.find( AuditEntity.class, 99L );
			assertEquals( "initial", e.text );
		}

		// Verify: revision 202 = single MOD with "second change" (not two rows)
		try (var s = scope.getSessionFactory().withOptions().atTransaction( 202 ).open()) {
			var e = s.find( AuditEntity.class, 99L );
			assertEquals( "second change", e.text );
		}

		// Verify via AuditLog: exactly 2 revisions for this entity
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getRevisions( AuditEntity.class, 99L );
			assertEquals( 2, revisions.size() );
		}
	}

	/**
	 * Test that ADD + DEL in the same transaction cancels out (no audit row.
	 */
	@Test
	void testAddDeleteCancellation(SessionFactoryScope scope) {
		currentTxId = 300;

		// Single transaction: persist + flush + remove
		scope.getSessionFactory().inTransaction( session -> {
			var e = new AuditEntity();
			e.id = 88L;
			e.text = "ephemeral";
			session.persist( e );
			session.flush();
			session.remove( e );
		} );

		// Verify: no audit rows exist for this entity
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getRevisions( AuditEntity.class, 88L );
			assertEquals( 0, revisions.size() );
		}
	}

	/**
	 * Test ADD + MOD merge: insert then modify in same transaction
	 * should produce a single ADD row with the final state.
	 */
	@Test
	void testAddModMerge(SessionFactoryScope scope) {
		currentTxId = 400;

		scope.getSessionFactory().inTransaction( session -> {
			var e = new AuditEntity();
			e.id = 77L;
			e.text = "before";
			session.persist( e );
			session.flush();
			e.text = "after";
		} );

		// Verify: single revision, modification type = ADD, final state
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var history = auditLog.getHistory( AuditEntity.class, 77L );
			assertEquals( 1, history.size() );
			assertEquals( org.hibernate.audit.ModificationType.ADD, history.get( 0 ).modificationType() );
			assertEquals( "after", ((AuditEntity) history.get( 0 ).entity()).text );
		}
	}

	/**
	 * Test collection multi-flush: modify the same collection twice
	 * with explicit flush between. Should produce correct audit rows.
	 */
	@Test
	void testCollectionMultiFlush(SessionFactoryScope scope) {
		currentTxId = 500;

		// Revision 501: create entity with collection
		scope.getSessionFactory().inTransaction( session -> {
			var e = new AuditEntity();
			e.id = 66L;
			e.text = "coll-test";
			e.stringSet.add( "a" );
			session.persist( e );
		} );

		// Revision 502: modify collection twice with flush between
		scope.getSessionFactory().inTransaction( session -> {
			var e = session.find( AuditEntity.class, 66L );
			e.stringSet.add( "b" );
			session.flush();
			e.stringSet.add( "c" );
		} );

		// Verify: at revision 502, collection has a, b, c
		try (var s = scope.getSessionFactory().withOptions().atTransaction( 502 ).open()) {
			var e = s.find( AuditEntity.class, 66L );
			assertEquals( Set.of( "a", "b", "c" ), e.stringSet );
		}
	}

	/**
	 * Test ADD+DEL cancellation for entity with collection:
	 * no orphaned collection audit rows should exist.
	 */
	@Test
	void testAddDeleteCancellationWithCollection(SessionFactoryScope scope) {
		currentTxId = 600;

		scope.getSessionFactory().inTransaction( session -> {
			var e = new AuditEntity();
			e.id = 55L;
			e.text = "ephemeral-coll";
			e.stringSet.add( "x" );
			session.persist( e );
			session.flush();
			session.remove( e );
		} );

		// Verify: no entity audit rows
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getRevisions( AuditEntity.class, 55L );
			assertEquals( 0, revisions.size() );
		}

		// Note: orphaned collection audit rows may remain (orphaned rows are unreachable by any query).
		// They are unreachable by any query since the entity has no audit row.
	}

	// ---- Entity classes ----

	@Audited
	@Entity(name = "AuditEntity")
	static class AuditEntity {
		@Id
		long id;
		String text;
		@Version
		int version;
		@Audited
		@ElementCollection
		Set<String> stringSet = new HashSet<>();
	}

	@Audited
	@Entity(name = "EmbeddedEntity")
	static class EmbeddedEntity {
		@Id
		long id;
		String name;
		@Embedded
		Address address;
	}

	@Embeddable
	static class Address {
		String street;
		String city;

		Address() {
		}

		Address(String street, String city) {
			this.street = street;
			this.city = city;
		}
	}
}
