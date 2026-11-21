package org.hibernate.temporal.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

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
@DomainModel(annotatedClasses = { TemporalLazyBasicTest.Document.class, TemporalLazyBasicTest.AuditedDocument.class })
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
					"from AuditedDocument d order by changesetId(d)", AuditedDocument.class ).getResultList();
			assertEquals( 2, documents.size() );
			for ( var document : documents ) {
				assertFalse( isPropertyInitialized( document, "body" ) );
			}
			assertEquals( "Final draft", documents.get( 1 ).body );
			assertEquals( "First draft", documents.get( 0 ).body );
			assertNull( documents.get( 0 ).note );
		}
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
