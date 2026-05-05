/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.ChangesetEntity;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.DefaultChangesetEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the built-in {@link DefaultChangesetEntity}, which is
 * auto-detected via {@link ChangesetEntity @ChangesetEntity}
 * with no explicit supplier configuration needed.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		DefaultChangesetEntityTest.Book.class,
		DefaultChangesetEntity.class
})
class DefaultChangesetEntityTest {

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		long id;
		String title;
	}

	@Test
	void testDefaultChangesetEntity(SessionFactoryScope scope) {
		final long beforeTest = System.currentTimeMillis();

		// Create
		scope.getSessionFactory().inTransaction( session -> {
			final var book = new Book();
			book.id = 1L;
			book.title = "Original Title";
			session.persist( book );
		} );

		// Update
		scope.getSessionFactory().inTransaction( session -> {
			final var book = session.find( Book.class, 1L );
			book.title = "Updated Title";
		} );

		// Delete
		scope.getSessionFactory().inTransaction( session -> {
			final var book = session.find( Book.class, 1L );
			session.remove( book );
		} );

		// Verify REVINFO rows for this test (book id=1)
		scope.getSessionFactory().inTransaction( session -> {
			final var auditLog = AuditLogFactory.create( session );
			final var revisionIds = auditLog.getChangesets( Book.class, 1L );
			assertEquals( 3, revisionIds.size() );

			final var revisions = session.createSelectionQuery(
					"from DefaultChangesetEntity where id in :ids order by id",
					DefaultChangesetEntity.class
			).setParameter( "ids", revisionIds ).getResultList();
			assertEquals( 3, revisions.size() );

			for ( var rev : revisions ) {
				assertTrue( rev.getTimestamp() >= beforeTest,
						"Timestamp should be >= test start time" );
				assertNotNull( rev.getRevisionInstant() );
			}

			final long rev1 = revisions.get( 0 ).getId();
			final long rev2 = revisions.get( 1 ).getId();
			final long rev3 = revisions.get( 2 ).getId();

			// Verify sequential revision numbers
			assertTrue( rev1 < rev2 );
			assertTrue( rev2 < rev3 );

			// Read at rev1: entity was created
			try (var s = scope.getSessionFactory().withOptions()
					.atChangeset( rev1 ).open()) {
				final var book = s.find( Book.class, 1L );
				assertNotNull( book );
				assertEquals( "Original Title", book.title );
			}

			// Read at rev2: entity was updated
			try (var s = scope.getSessionFactory().withOptions()
					.atChangeset( rev2 ).open()) {
				final var book = s.find( Book.class, 1L );
				assertNotNull( book );
				assertEquals( "Updated Title", book.title );
			}

			// Read at rev3: entity was deleted
			try (var s = scope.getSessionFactory().withOptions()
					.atChangeset( rev3 ).open()) {
				final var book = s.find( Book.class, 1L );
				assertNull( book );
			}
		} );
	}

	@Test
	void testGetHistoryReturnsChangesetEntity(SessionFactoryScope scope) {
		// Create and update a book
		scope.getSessionFactory().inTransaction( session -> {
			var book = new Book();
			book.id = 2L;
			book.title = "History Book";
			session.persist( book );
		} );
		scope.getSessionFactory().inTransaction( session -> {
			var book = session.find( Book.class, 2L );
			book.title = "Updated History Book";
		} );

		// getHistory() should return DefaultChangesetEntity instances as the revision member
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Book.class, 2L );

			assertEquals( 2, history.size() );

			// Verify revision is a DefaultChangesetEntity, not a plain Integer
			var entry1 = history.get( 0 );
			assertInstanceOf( DefaultChangesetEntity.class, entry1.changeset(),
					"Revision should be a DefaultChangesetEntity instance" );
			var rev1 = (DefaultChangesetEntity) entry1.changeset();
			assertTrue( rev1.getTimestamp() > 0, "Revision should have a timestamp" );

			var entry2 = history.get( 1 );
			assertInstanceOf( DefaultChangesetEntity.class, entry2.changeset() );
			var rev2 = (DefaultChangesetEntity) entry2.changeset();
			assertTrue( rev2.getId() > rev1.getId(),
					"Revisions should be sequential: rev1=" + rev1.getId() + ", rev2=" + rev2.getId() );
		}
	}

}
