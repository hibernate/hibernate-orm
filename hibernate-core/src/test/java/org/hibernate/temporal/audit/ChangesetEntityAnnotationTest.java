/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.ChangesetEntity;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests that {@link ChangesetEntity @ChangesetEntity} auto-detection
 * works without an explicit {@code hibernate.temporal.changeset_id_supplier}
 * setting.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		ChangesetEntityAnnotationTest.Book.class,
		ChangesetEntityAnnotationTest.MyRevisionInfo.class
})
class ChangesetEntityAnnotationTest {

	@ChangesetEntity
	@Entity(name = "MyRevisionInfo")
	@Table(name = "REVINFO")
	static class MyRevisionInfo {
		@Id
		@GeneratedValue
		@ChangesetEntity.ChangesetId
		@Column(name = "REV")
		int id;

		@ChangesetEntity.Timestamp
		@Column(name = "REVTSTMP")
		long timestamp;
	}

	@Audited
	@Entity(name = "RevAnnotBook")
	static class Book {
		@Id
		long id;
		String title;
	}

	@Test
	void testAutoDetectedChangesetEntity(SessionFactoryScope scope) {
		// Create
		scope.getSessionFactory().inTransaction( session -> {
			final var book = new Book();
			book.id = 1L;
			book.title = "Auto-detected";
			session.persist( book );
		} );

		// Update
		scope.getSessionFactory().inTransaction( session -> {
			final var book = session.find( Book.class, 1L );
			book.title = "Updated";
		} );

		// Delete
		scope.getSessionFactory().inTransaction( session -> {
			final var book = session.find( Book.class, 1L );
			session.remove( book );
		} );

		// Verify changeset entity was auto-configured
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var revisions = auditLog.getChangesets( Book.class, 1L );
			assertEquals( 3, revisions.size() );

			final int rev1 = (int) revisions.get( 0 );
			final int rev2 = (int) revisions.get( 1 );
			final int rev3 = (int) revisions.get( 2 );

			// Read at each revision
			try (var s = scope.getSessionFactory().withOptions()
					.atChangeset( rev1 ).open()) {
				final var book = s.find( Book.class, 1L );
				assertNotNull( book );
				assertEquals( "Auto-detected", book.title );
			}

			try (var s = scope.getSessionFactory().withOptions()
					.atChangeset( rev2 ).open()) {
				final var book = s.find( Book.class, 1L );
				assertNotNull( book );
				assertEquals( "Updated", book.title );
			}

			try (var s = scope.getSessionFactory().withOptions()
					.atChangeset( rev3 ).open()) {
				final var book = s.find( Book.class, 1L );
				assertNull( book );
			}
		}
	}

	@Test
	void testGetHistoryWithAutoDetectedChangesetEntity(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction( session -> {
			final var book = new Book();
			book.id = 2L;
			book.title = "History Book";
			session.persist( book );
		} );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var history = auditLog.getHistory( Book.class, 2L );
			assertEquals( 1, history.size() );

			// The revision should be a MyRevisionInfo instance (joined in HQL)
			final var entry = history.get( 0 );
			assertNotNull( entry.entity() );
			assertEquals( "History Book", entry.entity().title );
			assertNotNull( entry.changeset() );
		}
	}
}
