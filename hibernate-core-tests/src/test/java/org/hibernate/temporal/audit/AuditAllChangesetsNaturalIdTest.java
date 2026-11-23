package org.hibernate.temporal.audit;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.NaturalId;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.KeyType.NATURAL;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static jakarta.persistence.FetchType.LAZY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * In an all-changesets session, a lookup by natural id finds the entity which has
 * the natural id now, even after a snapshot of another entity which had the natural
 * id then was read, by a query or through an association.
 */
@SessionFactory
@DomainModel(annotatedClasses = { AuditAllChangesetsNaturalIdTest.Book.class, AuditAllChangesetsNaturalIdTest.Loan.class })
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditAllChangesetsNaturalIdTest$ChangesetIds"))
class AuditAllChangesetsNaturalIdTest {

	@Test
	void lookupAfterSnapshotWithSameNaturalId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = new Book();
			book.id = 1;
			book.isbn = "A";
			book.title = "old";
			session.persist( book );
		} );
		scope.inTransaction( session -> {
			// the natural id moves to another book
			final var book = session.find( Book.class, 1 );
			book.isbn = "B";
			book.title = "latest";
			session.flush();
			final var replacement = new Book();
			replacement.id = 2;
			replacement.isbn = "A";
			replacement.title = "replacement";
			session.persist( replacement );
		} );

		for ( boolean reference : List.of( false, true ) ) {
			try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
				assertEquals( "old", session.createSelectionQuery( "from Book where id = 1 and title = 'old'", Book.class )
						.getSingleResult().title );
				final var current = reference
						? session.getReference( Book.class, "A", NATURAL )
						: session.find( Book.class, "A", NATURAL );
				assertEquals( "replacement", ( (Book) Hibernate.unproxy( current ) ).title );
			}
		}
	}

	@Test
	void lookupAfterSnapshotReachedByAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = new Book();
			book.id = 3;
			book.isbn = "C";
			book.title = "initial";
			session.persist( book );
			final var loan = new Loan();
			loan.note = "first";
			loan.book = book;
			session.persist( loan );
		} );
		scope.inTransaction( session -> {
			// the natural id moves to another book
			final var book = session.find( Book.class, 3 );
			book.isbn = "D";
			book.title = "revised";
			session.find( Loan.class, 1 ).note = "second";
			session.flush();
			final var replacement = new Book();
			replacement.id = 4;
			replacement.isbn = "C";
			replacement.title = "successor";
			session.persist( replacement );
		} );

		for ( boolean reference : List.of( false, true ) ) {
			try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
				final var book = session.createSelectionQuery( "from Loan where note = 'first'", Loan.class )
						.getSingleResult().book;
				assertFalse( Hibernate.isInitialized( book ) );
				assertEquals( "initial", ( (Book) Hibernate.unproxy( book ) ).title );
				final var current = reference
						? session.getReference( Book.class, "C", NATURAL )
						: session.find( Book.class, "C", NATURAL );
				assertEquals( "successor", ( (Book) Hibernate.unproxy( current ) ).title );
			}
		}
	}

	@Audited
	@Entity(name = "Loan")
	static class Loan {
		@Id
		int id = 1;
		String note;
		@ManyToOne(fetch = LAZY)
		Book book;
	}

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		int id;
		@NaturalId(mutable = true)
		String isbn;
		String title;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
