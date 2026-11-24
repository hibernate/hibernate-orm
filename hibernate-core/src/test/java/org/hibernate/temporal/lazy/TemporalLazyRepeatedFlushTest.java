package org.hibernate.temporal.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.Temporal;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@JiraKey("HHH-21227")
@BytecodeEnhanced
class TemporalLazyRepeatedFlushTest {
	private static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Test
	@SessionFactory(useCollectingStatementObserver = true)
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void updateTwiceSingleTable(SessionFactoryScope scope) {
		updateTwice( scope, 3, 1 );
	}

	@Test
	@SessionFactory(useCollectingStatementObserver = true)
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void updateTwiceHistoryTable(SessionFactoryScope scope) {
		updateTwice( scope, 4, 2 );
	}

	private void updateTwice(SessionFactoryScope scope, int firstFlushStatements, int secondFlushStatements) {
		final var before = scope.fromTransaction( session -> {
			session.persist( new Item( 1, "a" ) );
			return session.getCurrentChangesetIdentifier();
		} );
		final var after = scope.fromTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			assertFalse( isPropertyInitialized( item, "body" ) );
			item.name = "b";
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();
			session.flush();
			observer.assertStatements().hasSize( firstFlushStatements );
			observer.assertQueries().filteredOn( sql -> sql.startsWith( "select" ) ).hasSize( 1 );
			session.clear();
			final var reloaded = session.find( Item.class, 1 );
			reloaded.body = "c";
			assertFalse( isPropertyInitialized( reloaded, "notes" ) );
			observer.clear();
			session.flush();
			observer.assertQueries().hasSize( secondFlushStatements ).allMatch( sql -> sql.startsWith( "update" ) );
			return session.getCurrentChangesetIdentifier();
		} );
		assertRevision( scope, before, "a", "a" );
		assertRevision( scope, after, "b", "c" );
		scope.inSession( session -> assertEquals( "c", session.find( Item.class, 1 ).body ) );
	}

	private static void assertRevision(SessionFactoryScope scope, Object changeset, String name, String body) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			final var item = session.find( Item.class, 1 );
			assertEquals( name, item.name );
			assertEquals( body, item.body );
			assertEquals( "notes", item.notes );
		}
	}

	@Entity(name = "Item")
	@Temporal
	static class Item {
		@Id
		int id;
		String name;
		@Basic(fetch = LAZY) String body;
		@Basic(fetch = LAZY)
		@LazyGroup("notes") String notes;

		Item() {
		}

		Item(int id, String name) {
			this.id = id;
			this.name = name;
			this.body = name;
			this.notes = "notes";
		}
	}
}
