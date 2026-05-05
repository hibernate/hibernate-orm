/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditEntry;
import org.hibernate.audit.AuditLog;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = AuditColumnFunctionTest.Book.class)
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditColumnFunctionTest$TxIdSupplier"))
class AuditColumnFunctionTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}

	}

	@Test
	void testAuditColumnFunctions(SessionFactoryScope scope) {
		currentTxId = 0;

		// tx 1: insert
		scope.getSessionFactory().inTransaction( session -> {
			var book = new Book();
			book.id = 1L;
			book.title = "Original";
			session.persist( book );
		} );

		// tx 2: update
		scope.getSessionFactory().inTransaction( session -> {
			var book = session.find( Book.class, 1L );
			book.title = "Updated";
		} );

		// tx 3: delete
		scope.getSessionFactory().inTransaction( session -> {
			var book = session.find( Book.class, 1L );
			session.remove( book );
		} );

		// Query all revisions using the HQL functions with scalar projections
		// (avoids entity identity caching issues with duplicate PKs)
		try (var s = scope.getSessionFactory().withStatelessOptions()
				.atChangeset( AuditLog.ALL_REVISIONS ).openStatelessSession()) {

			List<Object[]> results = s.createSelectionQuery(
					"select e.title, transactionId(e), modificationType(e) " +
					"from Book e where e.id = :id " +
					"order by transactionId(e)",
					Object[].class
			).setParameter( "id", 1L ).getResultList();

			assertEquals( 3, results.size(), "Should have 3 audit rows (ADD, MOD, DEL)" );

			// First revision: ADD
			assertEquals( "Original", results.get( 0 )[0] );
			assertEquals( 1, results.get( 0 )[1] );
			assertEquals( ModificationType.ADD, results.get( 0 )[2] );

			// Second revision: MOD
			assertEquals( "Updated", results.get( 1 )[0] );
			assertEquals( 2, results.get( 1 )[1] );
			assertEquals( ModificationType.MOD, results.get( 1 )[2] );

			// Third revision: DEL
			assertEquals( 3, results.get( 2 )[1] );
			assertEquals( ModificationType.DEL, results.get( 2 )[2] );

			// Test transactionId() in WHERE clause
			List<String> titles = s.createSelectionQuery(
					"select e.title from Book e " +
					"where e.id = :id and transactionId(e) = :txId",
					String.class
			).setParameter( "id", 1L ).setParameter( "txId", 2 ).getResultList();

			assertEquals( 1, titles.size() );
			assertEquals( "Updated", titles.get( 0 ) );

			// Test plain entity select: each row should be a distinct snapshot
			List<Book> allBooks = s.createSelectionQuery(
					"from Book e where e.id = :id order by transactionId(e)",
					Book.class
			).setParameter( "id", 1L ).getResultList();
			assertEquals( 3, allBooks.size() );
			// Each row should be a distinct entity instance (not deduplicated)
			assertTrue( allBooks.get( 0 ) != allBooks.get( 1 ),
					"All-revisions entities should be distinct instances" );
			assertTrue( allBooks.get( 1 ) != allBooks.get( 2 ),
					"All-revisions entities should be distinct instances" );
			// Each instance should reflect the state at its revision
			assertEquals( "Original", allBooks.get( 0 ).title );
			assertEquals( "Updated", allBooks.get( 1 ).title );

			// Test modificationType() in WHERE clause to find deletions
			long delCount = s.createSelectionQuery(
							"select count(e) from Book e " +
							"where e.id = :id and modificationType(e) = :delType",
							Long.class
					).setParameter( "id", 1L )
					.setParameter( "delType", ModificationType.DEL )
					.getSingleResult();

			assertEquals( 1, delCount );
		}
	}

	@Test
	void testGetHistory(SessionFactoryScope scope) {
		currentTxId = 0;

		scope.getSessionFactory().inTransaction( session -> {
			var book = new Book();
			book.id = 2L;
			book.title = "First";
			session.persist( book );
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var book = session.find( Book.class, 2L );
			book.title = "Second";
		} );

		scope.getSessionFactory().inTransaction( session -> {
			var book = session.find( Book.class, 2L );
			session.remove( book );
		} );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Book.class, 2L );

			assertEquals( 3, history.size() );

			// ADD
			AuditEntry<Book> add = history.get( 0 );
			assertEquals( "First", add.entity().title );
			assertEquals( ModificationType.ADD, add.modificationType() );

			// MOD
			AuditEntry<Book> mod = history.get( 1 );
			assertEquals( "Second", mod.entity().title );
			assertEquals( ModificationType.MOD, mod.modificationType() );

			// DEL
			AuditEntry<Book> del = history.get( 2 );
			assertEquals( ModificationType.DEL, del.modificationType() );

			// Each entity should be a distinct instance
			assertTrue( add.entity() != mod.entity() );
			assertTrue( mod.entity() != del.entity() );
		}
	}

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		long id;
		String title;
	}
}
