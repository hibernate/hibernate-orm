package org.hibernate.temporal.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BytecodeEnhanced
@SessionFactory(useCollectingStatementObserver = true)
@DomainModel(annotatedClasses = {
		TemporalLazyBasicTest.Document.class, TemporalLazyBasicTest.AuditedDocument.class,
		TemporalLazyBasicTest.Edition.class, TemporalLazyBasicTest.AuditEvent.class
})
@ServiceRegistry(settings = {
		@Setting(name = USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.lazy.TemporalLazyBasicTest$ChangesetIds")
})
class TemporalLazyBasicTest {
	@Test
	void enhancedProxyLoadsItsRevisionAfterCacheEviction(SessionFactoryScope scope) {
		final var firstChangeset = scope.fromTransaction( session -> {
			final var article = new Document();
			article.id = 6;
			article.title = "First title";
			session.persist( article );
			final var event = new AuditEvent();
			event.id = 6;
			event.article = article;
			session.persist( event );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> {
			session.find( Document.class, 6 ).title = "Second title";
			session.find( AuditEvent.class, 6 ).label = "Revised event";
		} );
		scope.inTransaction( session -> session.find( Document.class, 6 ).title = "Current title" );

		final var factory = scope.getSessionFactory();
		final var cache = factory.getCache();
		scope.inSession( session -> assertEquals( "Current title", session.find( Document.class, 6 ).title ) );
		assertTrue( cache.containsEntity( Document.class, 6 ) );
		cache.evictEntityData( Document.class, 6 );
		try (var session = factory.withOptions().atChangeset( firstChangeset ).openSession()) {
			final var article = session.getReference( Document.class, 6 );
			assertFalse( isInitialized( article ) );
			assertEquals( "First title", article.title );
		}
		assertFalse( cache.containsEntity( Document.class, 6 ) );
		try (var session = factory.withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var events = session.createSelectionQuery( """
					from AuditEvent e where e.id = 6 order by changesetId(e)
					""", AuditEvent.class ).getResultList();
			assertEquals( 2, events.size() );
			final var first = events.get( 0 ).article;
			final var second = events.get( 1 ).article;
			assertFalse( isInitialized( first ) );
			assertFalse( isInitialized( second ) );
			assertEquals( "Second title", second.title );
			assertEquals( "First title", first.title );
			assertEquals( 2L, session.createSelectionQuery(
					"select count(e) from AuditEvent e where e.id = 6", Long.class ).getSingleResult() );
		}
	}

	@Test
	@JiraKey("HHH-21227")
	void updateTitleWhileBodyIsUnfetched(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var document = new Document();
			document.id = 7;
			document.title = "before";
			document.body = "unchanged";
			session.persist( document );
		} );
		scope.getSessionFactory().getCache().evictEntityData( Document.class, 7 );
		final var after = scope.fromTransaction( session -> {
			final var document = session.find( Document.class, 7 );
			assertFalse( isPropertyInitialized( document, "body" ) );
			document.title = "after";
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inSession( session -> {
			final var document = session.find( Document.class, 7 );
			assertEquals( "after", document.title );
			assertEquals( "unchanged", document.body );
		} );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( after ).openSession()) {
			assertEquals( "unchanged", session.createSelectionQuery(
					"select d.body from Document d where d.id = 7 and d.title = 'after'", String.class ).getSingleResult() );
		}
	}

	@Test
	void cachedLazyPropertyPreservesPendingChanges(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var document = new Document();
			document.id = 10;
			document.body = "first draft";
			document.notes = "cached notes";
			session.persist( document );
		} );
		scope.inTransaction( session -> {
			session.setCacheMode( CacheMode.REFRESH );
			final var graph = session.createEntityGraph( Document.class );
			graph.addAttributeNodes( "body", "notes", "title" );
			final var document = session.find( graph, 10 );
			assertTrue( isPropertyInitialized( document, "notes" ) );
		} );
		assertTrue( scope.getSessionFactory().getCache().containsEntity( Document.class, 10 ) );
		final var changeset = scope.fromTransaction( session -> {
			session.setCacheMode( CacheMode.GET );
			final var document = session.createSelectionQuery( "from Document where id = 10", Document.class )
					.getSingleResult();
			assertFalse( isPropertyInitialized( document, "notes" ) );
			document.body = "edited draft";
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();
			assertEquals( "cached notes", document.notes );
			observer.assertStatements().isEmpty();
			assertEquals( "edited draft", document.body );
			return session.getCurrentChangesetIdentifier();
		} );
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession() ) {
			assertEquals( "edited draft", session.find( Document.class, 10 ).body );
		}
	}

	@Test
	void cachedNullAndUnfetchedField(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var document = new Document();
			document.id = 11;
			document.body = "uncached body";
			session.persist( document );
		} );
		scope.inTransaction( session -> {
			session.setCacheMode( CacheMode.REFRESH );
			final var graph = session.createEntityGraph( Document.class );
			graph.addAttributeNodes( "notes", "title" );
			final var document = session.find( graph, 11 );
			assertTrue( isPropertyInitialized( document, "notes" ) );
			assertFalse( isPropertyInitialized( document, "body" ) );
		} );
		scope.inTransaction( session -> {
			session.setCacheMode( CacheMode.GET );
			final var document = session.createSelectionQuery( "from Document where id = 11", Document.class )
					.getSingleResult();
			final var observer = scope.getCollectingStatementObserver();
			assertFalse( isPropertyInitialized( document, "notes" ) );
			observer.clear();
			assertNull( document.notes );
			observer.assertStatements().isEmpty();
			assertFalse( isPropertyInitialized( document, "body" ) );
			assertEquals( "uncached body", document.body );
			observer.assertStatements().hasSize( 1 );
		} );
	}

	@Test
	void lazyAttributesAtChangeset(SessionFactoryScope scope) {
		final var changeset = scope.fromTransaction( session -> {
			final var document = new Document();
			document.id = 1;
			document.title = "Original title";
			document.body = "First draft";
			session.persist( document );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> {
			final var document = session.find( Document.class, 1 );
			document.title = "Revised title";
			document.body = "Final draft";
		} );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			final var document = session.find( Document.class, 1 );
			assertEquals( "Original title", document.title );
			assertFalse( isPropertyInitialized( document, "body" ) );
			assertEquals( "First draft", document.body );
		}
		scope.inSession( session -> {
			final var document = session.find( Document.class, 1 );
			assertEquals( "Final draft", document.body );
			assertEquals( "Revised title", document.title );
		} );
	}

	@Test
	void lazyAttributesOfAuditRevisions(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var document = new AuditedDocument();
			document.id = 1;
			document.body = "First draft";
			document.note = "Private note";
			session.persist( document );
		} );
		scope.inTransaction( session -> session.find( AuditedDocument.class, 1 ).body = "Final draft" );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var documents = session.createSelectionQuery(
					"from AuditedDocument d where d.id = 1 order by changesetId(d)", AuditedDocument.class ).getResultList();
			assertEquals( 2, documents.size() );
			for ( var document : documents ) {
				assertFalse( isPropertyInitialized( document, "body" ) );
			}
			assertEquals( "Final draft", documents.get( 1 ).body );
			assertEquals( "First draft", documents.get( 0 ).body );
			assertNull( documents.get( 0 ).note );
		}
	}

	@Test
	void lazyAttributeOfIndependentRootInBothProjectionOrders(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var revision = new AuditedDocument();
			revision.id = 2;
			revision.body = "Earlier revision";
			session.persist( revision );
		} );
		scope.inTransaction( session -> {
			final var article = new Document();
			article.id = 2;
			article.body = "Article text";
			session.persist( article );
		} );

		final var builder = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS );
		for ( var projection : List.of( "a as article, r", "r, a as article" ) ) {
			try (var session = builder.openSession()) {
				final var tuple = session.createSelectionQuery( "select " + projection
						+ " from AuditedDocument r cross join Document a where r.id = 2 and a.id = 2",
						Tuple.class ).getSingleResult();
				final var article = tuple.get( "article", Document.class );
				assertFalse( isPropertyInitialized( article, "body" ) );
				assertEquals( "Article text", article.body );
			}
		}
	}

	@Test
	void joinFetchedArticleInBothProjectionOrders(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var article = new Document();
			article.id = 3;
			article.title = "Old title";
			article.body = "Old body";
			session.persist( article );
			final var edition = new Edition();
			edition.id = 3;
			edition.article = article;
			session.persist( edition );
		} );
		scope.inTransaction( session -> {
			final var revision = new AuditedDocument();
			revision.id = 3;
			revision.body = "Audit revision";
			session.persist( revision );
		} );
		scope.inTransaction( session -> {
			final var article = session.find( Document.class, 3 );
			article.title = "Current title";
			article.body = "Current body";
		} );

		final var builder = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS );
		for ( var projection : List.of( "e as edition, r", "r, e as edition" ) ) {
			try (var session = builder.openSession()) {
				final var tuple = session.createSelectionQuery( "select " + projection
						+ " from AuditedDocument r cross join Edition e join fetch e.article"
						+ " where r.id = 3 and e.id = 3", Tuple.class ).getSingleResult();
				final var article = tuple.get( "edition", Edition.class ).article;
				assertEquals( "Current title", article.title );
				assertFalse( isPropertyInitialized( article, "body" ) );
				assertEquals( "Current body", article.body );
			}
		}
	}

	@Test
	void joinFetchedArticleAtRevisionOfAuditedEvent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var article = new Document();
			article.id = 4;
			article.title = "Old title";
			article.body = "Old body";
			session.persist( article );
			final var event = new AuditEvent();
			event.id = 4;
			event.article = article;
			session.persist( event );
		} );
		scope.inTransaction( session -> {
			final var article = session.find( Document.class, 4 );
			article.title = "Current title";
			article.body = "Current body";
		} );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			// the article of an event revision is the article as of that revision
			final var event = session.createSelectionQuery(
					"from AuditEvent e join fetch e.article where e.id = 4", AuditEvent.class ).getSingleResult();
			final var article = event.article;
			assertTrue( isInitialized( article ) );
			assertTrue( session.isReadOnly( article ) );
			assertEquals( "Old title", article.title );
			assertFalse( isPropertyInitialized( article, "body" ) );
			assertEquals( "Old body", article.body );
		}
	}

	@Test
	void temporalCollectionAtRevisionOfAuditedEvent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var article = new Document();
			article.id = 9;
			article.title = "First title";
			session.persist( article );
			final var event = new AuditEvent();
			event.id = 9;
			event.label = "First event";
			event.articles.add( article );
			session.persist( event );
		} );
		scope.inTransaction( session -> {
			session.find( Document.class, 9 ).title = "Second title";
			session.find( AuditEvent.class, 9 ).label = "Second event";
		} );
		scope.inTransaction( session -> session.find( Document.class, 9 ).title = "Current title" );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var events = session.createSelectionQuery(
					"from AuditEvent e where e.id = 9 order by changesetId(e)", AuditEvent.class ).getResultList();
			assertEquals( 2, events.size() );
			assertEquals( List.of( "First title" ), events.get( 0 ).articles.stream().map( a -> a.title ).toList() );
			assertEquals( List.of( "Second title" ), events.get( 1 ).articles.stream().map( a -> a.title ).toList() );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var events = session.createSelectionQuery(
					"from AuditEvent e join fetch e.articles where e.id = 9 order by changesetId(e)",
					AuditEvent.class ).getResultList();
			assertEquals( 2, events.size() );
			assertEquals( List.of( "First title" ), events.get( 0 ).articles.stream().map( a -> a.title ).toList() );
			assertEquals( List.of( "Second title" ), events.get( 1 ).articles.stream().map( a -> a.title ).toList() );
		}
	}

	@Test
	void currentArticleFetchedThroughNonAuditedEdition(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var article = new AuditedDocument();
			article.id = 5;
			article.title = "Original title";
			article.body = "First draft";
			session.persist( article );
			final var edition = new Edition();
			edition.id = 5;
			edition.auditedArticle = article;
			session.persist( edition );
			final var event = new AuditEvent();
			event.id = 5;
			event.edition = edition;
			session.persist( event );
		} );
		scope.inTransaction( session -> {
			final var article = session.find( AuditedDocument.class, 5 );
			article.title = "Revised title";
			article.body = "Second draft";
		} );
		scope.inTransaction( session -> {
			final var article = session.find( AuditedDocument.class, 5 );
			article.title = "Published title";
			article.body = "Final draft";
		} );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			// The non-audited edition is current, so its article is too
			final var event = session.createSelectionQuery(
					"from AuditEvent e join fetch e.edition ed join fetch ed.auditedArticle where e.id = 5",
					AuditEvent.class ).getSingleResult();
			final var article = event.edition.auditedArticle;
			assertTrue( isInitialized( article ) );
			assertEquals( "Published title", article.title );
			assertFalse( isPropertyInitialized( article, "body" ) );
			assertEquals( "Final draft", article.body );
		}
	}

	@Test
	void excludedLazyGroupOfAuditRevision(SessionFactoryScope scope) {
		final var firstChangeset = scope.fromTransaction( session -> {
			final var document = new AuditedDocument();
			document.id = 8;
			document.body = "First draft";
			document.priority = 5;
			session.persist( document );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> session.find( AuditedDocument.class, 8 ).body = "Final draft" );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( firstChangeset ).openSession()) {
			final var document = session.getReference( AuditedDocument.class, 8 );
			assertFalse( isInitialized( document ) );
			// the audit row has no column for the group's only attribute, so it reads as absent
			assertEquals( 0, document.priority );
			assertEquals( "First draft", document.body );
		}
	}

	@Entity(name = "AuditEvent")
	@Audited
	static class AuditEvent {
		@Id int id;
		String label;
		@ManyToOne(fetch = LAZY) Document article;
		@ManyToOne(fetch = LAZY) Edition edition;
		@ManyToMany Set<Document> articles = new HashSet<>();
	}

	@Entity(name = "Edition")
	static class Edition {
		@Id int id;
		@ManyToOne(fetch = LAZY) Document article;
		@ManyToOne(fetch = LAZY) AuditedDocument auditedArticle;
	}

	@Entity(name = "AuditedDocument")
	@Audited
	static class AuditedDocument {
		@Id int id;
		String title;
		@Basic(fetch = LAZY) String body;
		@Audited.Excluded @Basic(fetch = LAZY) String note;
		@Audited.Excluded @Basic(fetch = LAZY) @LazyGroup("ranking") int priority;
	}

	@Entity(name = "Document")
	@Temporal
	@Cache(usage = READ_WRITE)
	static class Document {
		@Id int id;
		String title;
		@Basic(fetch = LAZY) String body;
		@Basic(fetch = LAZY) @LazyGroup("notes") String notes;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
