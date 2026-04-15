/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import java.time.Instant;
import java.util.Set;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.RevisionEntity;
import org.hibernate.audit.AuditException;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.RevisionListener;

import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test demonstrating {@link RevisionEntity @RevisionEntity}
 * auto-detection with a custom revision entity and
 * {@link RevisionListener}.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditRevisionEntityTest.MyEntity.class,
		AuditRevisionEntityTest.RevisionInfo.class
})
class AuditRevisionEntityTest {

	/**
	 * Custom revision entity with a {@link RevisionListener}
	 * that populates the {@code username} field.
	 */
	@RevisionEntity(listener = UsernameRevisionListener.class)
	@Entity(name = "RevisionInfo")
	@Table(name = "REVINFO")
	static class RevisionInfo {
		@Id
		@GeneratedValue
		@RevisionEntity.TransactionId
		@Column(name = "REV")
		int id;

		@RevisionEntity.Timestamp
		@Column(name = "REVTSTMP")
		long timestamp;

		@Column(name = "USERNAME")
		String username;
	}

	public static class UsernameRevisionListener implements RevisionListener {
		@Override
		public void newRevision(Object revisionEntity) {
			( (RevisionInfo) revisionEntity ).username = "test-user";
		}
	}

	@Audited
	@Entity(name = "MyEntity")
	static class MyEntity {
		@Id
		long id;
		String name;
	}

	@Test
	void testRevisionEntitySupplier(SessionFactoryScope scope) {
		// Create
		scope.getSessionFactory().inTransaction( session -> {
			final var entity = new MyEntity();
			entity.id = 1L;
			entity.name = "original";
			session.persist( entity );
		} );

		// Update
		scope.getSessionFactory().inTransaction( session -> {
			final var entity = session.find( MyEntity.class, 1L );
			entity.name = "updated";
		} );

		// Capture baseline revision count before read-only operations
		final long[] baseline = new long[1];
		scope.getSessionFactory().inTransaction( session ->
														baseline[0] = session.createSelectionQuery(
																"select count(*) from RevisionInfo", Long.class
														).getSingleResult()
		);

		// Read current entity via find(). No revision entity should be created
		scope.getSessionFactory().inTransaction( session -> {
			final var entity = session.find( MyEntity.class, 1L );
			assertNotNull( entity );
			assertEquals( "updated", entity.name );
		} );

		// Read current entity via HQL. No revision entity should be created
		scope.getSessionFactory().inTransaction( session -> {
			final var entity = session.createSelectionQuery(
					"from MyEntity where id = 1", MyEntity.class
			).getSingleResult();
			assertEquals( "updated", entity.name );
		} );

		// Verify no extra REVINFO rows were created by the read-only transactions
		scope.getSessionFactory().inTransaction( session -> {
			final long revCount = session.createSelectionQuery(
					"select count(*) from RevisionInfo", Long.class
			).getSingleResult();
			assertEquals( baseline[0], revCount, "Read-only queries must not create revision entities" );
		} );

		// Delete
		scope.getSessionFactory().inTransaction( session -> {
			final var entity = session.find( MyEntity.class, 1L );
			session.remove( entity );
		} );

		// Verify revision reads via atTransaction
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getRevisions( MyEntity.class, 1L );
			assertEquals( 3, revisions.size() );

			final int rev1 = ( (Number) revisions.get( 0 ) ).intValue();
			final int rev2 = ( (Number) revisions.get( 1 ) ).intValue();
			final int rev3 = ( (Number) revisions.get( 2 ) ).intValue();

			// Read at revision 1: entity was created
			try (var s = scope.getSessionFactory().withOptions().atTransaction( rev1 ).open()) {
				final var entity = s.find( MyEntity.class, 1L );
				assertNotNull( entity );
				assertEquals( "original", entity.name );
			}

			// Read at revision 2: entity was updated
			try (var s = scope.getSessionFactory().withOptions().atTransaction( rev2 ).open()) {
				final var entity = s.find( MyEntity.class, 1L );
				assertNotNull( entity );
				assertEquals( "updated", entity.name );
			}

			// Read at revision 3: entity was deleted
			try (var s = scope.getSessionFactory().withOptions().atTransaction( rev3 ).open()) {
				final var entity = s.find( MyEntity.class, 1L );
				assertNull( entity );
			}
		}
	}

	@Test
	void testFindWithIncludeDeletions(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction( session -> {
			var entity = new MyEntity();
			entity.id = 10L;
			entity.name = "to-delete";
			session.persist( entity );
		} );
		scope.getSessionFactory().inTransaction( session ->
														session.remove( session.find( MyEntity.class, 10L ) )
		);

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getRevisions( MyEntity.class, 10L );
			assertEquals( 2, revisions.size() );
			final var delRevision = revisions.get( 1 );

			// Without includeDeletions: null
			assertNull( auditLog.find( MyEntity.class, 10L, delRevision ) );

			// With includeDeletions: entity state at deletion
			final var deleted = auditLog.find( MyEntity.class, 10L, delRevision, true );
			assertNotNull( deleted );
			assertEquals( "to-delete", deleted.name );
		}
	}

	@Test
	void testFindByInstant(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inTransaction( session -> {
			var entity = new MyEntity();
			entity.id = 20L;
			entity.name = "instant-test";
			session.persist( entity );
		} );

		Thread.sleep( 50 );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var entity = auditLog.find( MyEntity.class, 20L, Instant.now() );
			assertNotNull( entity );
			assertEquals( "instant-test", entity.name );
		}
	}

	@Test
	void testGetTransactionTimestamp(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction( session -> {
			var entity = new MyEntity();
			entity.id = 30L;
			entity.name = "datetime-test";
			session.persist( entity );
		} );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getRevisions( MyEntity.class, 30L );
			assertEquals( 1, revisions.size() );

			final Instant timestamp = auditLog.getTransactionTimestamp( revisions.get( 0 ) );
			assertNotNull( timestamp );
			assertTrue( timestamp.isAfter( Instant.now().minusSeconds( 60 ) ) );
		}
	}

	@Test
	void testGetTransactionTimestampNonExistent(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertThrows(
					AuditException.class,
					() -> auditLog.getTransactionTimestamp( 999999 )
			);
		}
	}

	@Test
	void testGetTransactionIdForDate(SessionFactoryScope scope) throws InterruptedException {
		scope.getSessionFactory().inTransaction( session -> {
			var entity = new MyEntity();
			entity.id = 40L;
			entity.name = "txid-test";
			session.persist( entity );
		} );

		Thread.sleep( 50 );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var txId = auditLog.getTransactionId( Instant.now() );
			assertNotNull( txId );

			final var entity = auditLog.find( MyEntity.class, 40L, txId );
			assertNotNull( entity );
			assertEquals( "txid-test", entity.name );
		}
	}

	@Test
	void testGetTransactionIdForDateTooEarly(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertThrows(
					AuditException.class,
					() -> auditLog.getTransactionId( Instant.parse( "2000-01-01T00:00:00Z" ) )
			);
		}
	}

	@Test
	void testFindRevision(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction( session -> {
			var entity = new MyEntity();
			entity.id = 50L;
			entity.name = "findrev-test";
			session.persist( entity );
		} );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getRevisions( MyEntity.class, 50L );
			final var txId = revisions.get( 0 );

			RevisionInfo revInfo = auditLog.findRevision( txId );
			assertNotNull( revInfo );
			assertEquals( "test-user", revInfo.username );
			assertTrue( revInfo.timestamp > 0 );
		}
	}

	@Test
	void testFindRevisionNonExistent(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertThrows(
					AuditException.class,
					() -> auditLog.findRevision( 999999 )
			);
		}
	}

	@Test
	void testFindRevisions(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction( session -> {
			var entity = new MyEntity();
			entity.id = 60L;
			entity.name = "findrevs-v1";
			session.persist( entity );
		} );
		scope.getSessionFactory().inTransaction( session ->
														session.find( MyEntity.class, 60L ).name = "findrevs-v2"
		);

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getRevisions( MyEntity.class, 60L );
			assertEquals( 2, revisions.size() );

			final var revMap = auditLog.<RevisionInfo>findRevisions( Set.copyOf( revisions ) );
			assertEquals( 2, revMap.size() );
			for ( var entry : revMap.values() ) {
				assertEquals( "test-user", entry.username );
			}
		}
	}

	@Test
	void testIsAudited(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertTrue( auditLog.isAudited( MyEntity.class ) );
		}
	}

	@Test
	void testAuditTableForeignKeys(SessionFactoryScope scope) {
		// Verify REV -> REVINFO FK exists on the audit table
		final var metadata = scope.getMetadataImplementor();
		final var auditTable = metadata.getEntityBinding( MyEntity.class.getName() ).getAuxiliaryTable();
		assertNotNull( auditTable, "Audit table should exist" );
		boolean foundRevFk = false;
		for ( var fk : auditTable.getForeignKeyCollection() ) {
			final var referencedTable = fk.getReferencedTable();
			if ( referencedTable != null && referencedTable.getName().equalsIgnoreCase( "REVINFO" ) ) {
				foundRevFk = true;
				assertEquals( 1, fk.getColumnSpan(), "REV FK should have exactly 1 column" );
			}
		}
		assertTrue( foundRevFk, "Expected FK from MyEntity_aud.REV -> REVINFO" );
	}
}
