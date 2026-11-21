package org.hibernate.temporal.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;

import java.util.List;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@BytecodeEnhanced
@SessionFactory
@DomainModel(annotatedClasses = {
		TemporalLazyBasicTest.Document.class, TemporalLazyBasicTest.AuditedDocument.class, TemporalLazyBasicTest.Edition.class
})
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.lazy.TemporalLazyBasicTest$ChangesetIds")
})
class TemporalLazyBasicTest {
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

	@Entity(name = "Edition")
	static class Edition {
		@Id int id;
		@ManyToOne(fetch = LAZY) Document article;
	}

	@Entity(name = "AuditedDocument")
	@Audited
	static class AuditedDocument {
		@Id int id;
		@Basic(fetch = LAZY) String body;
		@Audited.Excluded @Basic(fetch = LAZY) String note;
	}

	@Entity(name = "Document")
	@Temporal
	static class Document {
		@Id int id;
		String title;
		@Basic(fetch = LAZY) String body;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
