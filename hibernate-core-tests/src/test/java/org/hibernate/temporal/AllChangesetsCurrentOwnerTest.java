package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Hibernate.unproxy;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * In an all-changesets session, a to-one held by current state refers to the
 * current version of an audited or temporal target, however it is fetched.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		AllChangesetsCurrentOwnerTest.Event.class,
		AllChangesetsCurrentOwnerTest.Edition.class,
		AllChangesetsCurrentOwnerTest.Article.class,
		AllChangesetsCurrentOwnerTest.Document.class
})
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.AllChangesetsCurrentOwnerTest$ChangesetIds")
})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsPrimaryKeyUpdate.class)
class AllChangesetsCurrentOwnerTest {

	private static Object historyCreatedFor;

	@BeforeEach
	void createHistory(SessionFactoryScope scope) {
		if ( historyCreatedFor == scope.getSessionFactory() ) {
			return;
		}
		historyCreatedFor = scope.getSessionFactory();
		scope.inTransaction( session -> {
			final var article = new Article();
			article.id = 1;
			article.title = "Original title";
			session.persist( article );
			final var document = new Document();
			document.id = 1;
			document.title = "Original document";
			session.persist( document );
			for ( int id = 1; id <= 2; id++ ) {
				final var edition = new Edition();
				edition.id = id;
				edition.article = article;
				edition.document = document;
				session.persist( edition );
			}
			final var event = new Event();
			event.id = 1;
			event.label = "First event";
			event.edition = session.find( Edition.class, 1 );
			event.document = document;
			session.persist( event );
		} );
		scope.inTransaction( session -> {
			session.find( Article.class, 1 ).title = "Revised title";
			session.find( Event.class, 1 ).label = "Second event";
		} );
		scope.inTransaction( session -> {
			session.find( Article.class, 1 ).title = "Published title";
			session.find( Document.class, 1 ).title = "Current document";
		} );
	}

	@Test
	void joinFetchFromNonAuditedRoot(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var editions = session.createSelectionQuery(
					"from Edition ed join fetch ed.article join fetch ed.document where ed.id = 1",
					Edition.class ).getResultList();
			assertEquals( 1, editions.size() );
			final var edition = editions.get( 0 );
			assertEquals( "Published title", edition.article.title );
			assertEquals( "Current document", edition.document.title );
			assertFalse( session.isReadOnly( edition.article ) );
			// the lazy association of another edition is the same current version
			final var other = session.find( Edition.class, 2 );
			assertSame( edition.article, unproxy( other.article ) );
			assertSame( edition.document, unproxy( other.document ) );
		}
	}

	@Test
	void plainJoinFromNonAuditedRoot(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			assertEquals( List.of( "Published title" ), session.createSelectionQuery(
					"select a.title from Edition ed join ed.article a where ed.id = 1", String.class ).getResultList() );
			assertEquals( List.of( "Current document" ), session.createSelectionQuery(
					"select d.title from Edition ed join ed.document d where ed.id = 1", String.class ).getResultList() );
		}
	}

	@Test
	void lazyFromNonAuditedRoot(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var edition = session.createSelectionQuery( "from Edition where id = 1", Edition.class )
					.getSingleResult();
			assertEquals( "Published title", unproxy( edition.article, Article.class ).title );
			assertEquals( "Current document", unproxy( edition.document, Document.class ).title );
		}
	}

	@Test
	void joinFetchThroughAuditedRevisions(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var events = session.createSelectionQuery(
					"from Event e join fetch e.edition ed join fetch ed.article join fetch ed.document"
					+ " where e.id = 1 order by changesetId(e)",
					Event.class ).getResultList();
			assertEquals( 2, events.size() );
			assertEquals( "First event", events.get( 0 ).label );
			assertEquals( "Second event", events.get( 1 ).label );
			for ( var event : events ) {
				assertSame( events.get( 0 ).edition, event.edition );
				assertEquals( "Published title", event.edition.article.title );
				assertEquals( "Current document", event.edition.document.title );
			}
		}
	}

	@Test
	void lazyThroughAuditedRevisions(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var events = session.createSelectionQuery(
					"from Event e where e.id = 1 order by changesetId(e)", Event.class ).getResultList();
			assertEquals( 2, events.size() );
			for ( var event : events ) {
				final var edition = unproxy( event.edition, Edition.class );
				assertEquals( "Published title", unproxy( edition.article, Article.class ).title );
				assertEquals( "Current document", unproxy( edition.document, Document.class ).title );
			}
		}
	}

	@Test
	void temporalTargetOfRevisionAtItsRevision(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			// the document was revised after both revisions of the event
			final var fetched = session.createSelectionQuery(
					"from Event e join fetch e.document where e.id = 1 order by changesetId(e)",
					Event.class ).getResultList();
			assertEquals( 2, fetched.size() );
			for ( var event : fetched ) {
				assertEquals( "Original document", event.document.title );
			}
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var events = session.createSelectionQuery(
					"from Event e where e.id = 1 order by changesetId(e)", Event.class ).getResultList();
			assertEquals( 2, events.size() );
			for ( var event : events ) {
				assertEquals( "Original document", unproxy( event.document, Document.class ).title );
			}
		}
	}

	@Entity(name = "Event")
	@Audited
	static class Event {
		@Id int id;
		String label;
		@ManyToOne(fetch = LAZY) Edition edition;
		@ManyToOne(fetch = LAZY) Document document;
	}

	@Entity(name = "Edition")
	static class Edition {
		@Id int id;
		@ManyToOne(fetch = LAZY) Article article;
		@ManyToOne(fetch = LAZY) Document document;
	}

	@Entity(name = "Article")
	@Audited
	static class Article {
		@Id int id;
		String title;
	}

	@Entity(name = "Document")
	@Temporal
	static class Document {
		@Id int id;
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
