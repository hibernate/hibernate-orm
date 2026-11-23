package org.hibernate.temporal.audit;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.ModificationType;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@code changesetId()} and {@code modificationType()} applied to joined
 * audited entities in an all-changesets session.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditColumnFunctionJoinTest.Shelf.class,
		AuditColumnFunctionJoinTest.Book.class,
		AuditColumnFunctionJoinTest.Review.class,
		AuditColumnFunctionJoinTest.Edition.class
})
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditColumnFunctionJoinTest$ChangesetIds"))
class AuditColumnFunctionJoinTest {

	private static Object historyCreatedFor;

	@BeforeEach
	void createHistory(SessionFactoryScope scope) {
		if ( historyCreatedFor == scope.getSessionFactory() ) {
			return;
		}
		historyCreatedFor = scope.getSessionFactory();
		// changeset 1
		scope.inTransaction( session -> {
			final var shelf = new Shelf();
			shelf.id = 1;
			session.persist( shelf );
			final var book = new Book();
			book.id = 1;
			book.title = "First title";
			book.shelf = shelf;
			session.persist( book );
			final var review = new Review();
			review.id = 1;
			review.text = "Good";
			review.book = book;
			session.persist( review );
			final var edition = new Edition();
			edition.id = 1;
			edition.book = book;
			session.persist( edition );
		} );
		// changeset 2
		scope.inTransaction( session -> session.find( Book.class, 1 ).title = "Second title" );
		// changeset 3
		scope.inTransaction( session -> session.find( Review.class, 1 ).text = "Better" );
	}

	@Test
	void toOneJoinAlias(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			// each review revision joins the book as of that revision
			final var rows = session.createSelectionQuery( """
					select changesetId(r), changesetId(b), modificationType(b)
					from Review r join r.book b
					where r.id = 1
					order by changesetId(r)
					""", Object[].class ).getResultList();
			assertEquals( 2, rows.size() );
			assertEquals( List.of( 1, 1, ModificationType.ADD ), List.of( rows.get( 0 ) ) );
			assertEquals( List.of( 3, 2, ModificationType.MOD ), List.of( rows.get( 1 ) ) );
		}
	}

	@Test
	void implicitJoinPath(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			assertEquals( List.of( 1, 2 ), session.createSelectionQuery(
					"select changesetId(r.book) from Review r where r.id = 1 order by changesetId(r)",
					Object.class ).getResultList() );
		}
	}

	@Test
	void collectionJoinAlias(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			// the shelf has one revision, and its books are as of that revision
			assertEquals( List.of( 1 ), session.createSelectionQuery(
					"select changesetId(b) from Shelf s join s.books b where s.id = 1",
					Object.class ).getResultList() );
		}
	}

	@Test
	void currentStateHasNoChangeset(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			// the book of a non-audited edition is its current version, not an audit row
			assertThrows( IllegalArgumentException.class, () -> session.createSelectionQuery(
					"select changesetId(b) from Edition ed join ed.book b", Object.class ).getResultList() );
		}
	}

	@Test
	void basicCollectionIsNotAnEntity(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			assertThrows( IllegalArgumentException.class, () -> session.createSelectionQuery(
					"select changesetId(l) from Shelf s join s.labels l", Object.class ).getResultList() );
		}
	}

	@Entity(name = "Shelf")
	@Audited
	static class Shelf {
		@Id int id;
		@OneToMany(mappedBy = "shelf") Set<Book> books = new HashSet<>();
		@ElementCollection Set<String> labels = new HashSet<>();
	}

	@Entity(name = "Book")
	@Audited
	static class Book {
		@Id int id;
		String title;
		@ManyToOne(fetch = LAZY) Shelf shelf;
	}

	@Entity(name = "Review")
	@Audited
	static class Review {
		@Id int id;
		String text;
		@ManyToOne(fetch = LAZY) Book book;
	}

	@Entity(name = "Edition")
	static class Edition {
		@Id int id;
		@ManyToOne(fetch = LAZY) Book book;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
