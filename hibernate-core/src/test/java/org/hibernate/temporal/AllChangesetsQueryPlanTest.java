package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

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

import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The same query gives each kind of session its own results, whichever runs first:
 * all changesets reads every audit revision but only the current temporal version.
 */
@SessionFactory
@DomainModel(annotatedClasses = { AllChangesetsQueryPlanTest.Document.class, AllChangesetsQueryPlanTest.Note.class })
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = "org.hibernate.temporal.AllChangesetsQueryPlanTest$ChangesetIds")
})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsPrimaryKeyUpdate.class)
class AllChangesetsQueryPlanTest {
	private static final List<String> ORIGINAL = List.of( "Original" );
	private static final List<String> CURRENT = List.of( "Current" );
	private static final List<String> EVERY_REVISION = List.of( "Current", "Original" );

	private static Object historyCreatedFor;
	private static Object firstChangeset;

	@BeforeEach
	void createHistory(SessionFactoryScope scope) {
		if ( historyCreatedFor == scope.getSessionFactory() ) {
			return;
		}
		historyCreatedFor = scope.getSessionFactory();
		firstChangeset = scope.fromTransaction( session -> {
			final var document = new Document();
			document.id = 1;
			document.title = "Original";
			session.persist( document );
			final var note = new Note();
			note.id = 1;
			note.text = "Original";
			session.persist( note );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> {
			session.find( Document.class, 1 ).title = "Current";
			session.find( Note.class, 1 ).text = "Current";
		} );
	}

	@Test
	void allChangesetsFirst(SessionFactoryScope scope) {
		final String documents = "select d.title from Document d where d.id = 1";
		final String notes = "select n.text from Note n where n.id = 1";
		assertEquals( CURRENT, allChangesets( scope, documents ) );
		assertEquals( EVERY_REVISION, allChangesets( scope, notes ) );
		assertEquals( ORIGINAL, atFirstChangeset( scope, documents ) );
		assertEquals( ORIGINAL, atFirstChangeset( scope, notes ) );
		assertEquals( CURRENT, current( scope, documents ) );
		assertEquals( CURRENT, current( scope, notes ) );
	}

	@Test
	void pointInTimeFirst(SessionFactoryScope scope) {
		final String documents = "select title from Document where id = 1";
		final String notes = "select text from Note where id = 1";
		assertEquals( ORIGINAL, atFirstChangeset( scope, documents ) );
		assertEquals( ORIGINAL, atFirstChangeset( scope, notes ) );
		assertEquals( CURRENT, allChangesets( scope, documents ) );
		assertEquals( EVERY_REVISION, allChangesets( scope, notes ) );
		assertEquals( CURRENT, current( scope, documents ) );
		assertEquals( CURRENT, current( scope, notes ) );
	}

	@Test
	void entityRootsUnderAllChangesets(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var documents = session.createSelectionQuery( "from Document", Document.class ).getResultList();
			assertEquals( 1, documents.size() );
			assertEquals( "Current", documents.get( 0 ).title );
		}
	}

	private static List<String> allChangesets(SessionFactoryScope scope, String hql) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			return session.createSelectionQuery( hql, String.class ).getResultList().stream().sorted().toList();
		}
	}

	private static List<String> atFirstChangeset(SessionFactoryScope scope, String hql) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( firstChangeset ).openSession()) {
			return session.createSelectionQuery( hql, String.class ).getResultList().stream().sorted().toList();
		}
	}

	private static List<String> current(SessionFactoryScope scope, String hql) {
		try (var session = scope.getSessionFactory().openSession()) {
			return session.createSelectionQuery( hql, String.class ).getResultList().stream().sorted().toList();
		}
	}

	@Entity(name = "Document")
	@Temporal
	static class Document {
		@Id int id;
		String title;
	}

	@Entity(name = "Note")
	@Audited
	static class Note {
		@Id int id;
		String text;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
