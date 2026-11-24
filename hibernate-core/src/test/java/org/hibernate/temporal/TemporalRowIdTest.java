package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.RowId;
import org.hibernate.annotations.Temporal;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@JiraKey("HHH-21220")
class TemporalRowIdTest {
	private static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void updateTwiceSingleTable(SessionFactoryScope scope) {
		updateTwice( scope );
	}

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void updateTwiceHistoryTable(SessionFactoryScope scope) {
		updateTwice( scope );
	}

	private void updateTwice(SessionFactoryScope scope) {
		final var before = scope.fromTransaction( session -> {
			session.persist( new Item( 1, "a" ) );
			return session.getCurrentChangesetIdentifier();
		} );
		final var after = scope.fromTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			item.name = "b";
			session.flush();
			item.name = "c";
			session.flush();
			item.name = "d";
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> session.remove( session.find( Item.class, 1 ) ) );
		scope.inSession( session -> assertNull( session.find( Item.class, 1 ) ) );
		assertName( scope, before, "a" );
		assertName( scope, after, "d" );
	}

	private static void assertName(SessionFactoryScope scope, Object changeset, String expected) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			assertEquals( expected, session.find( Item.class, 1 ).name );
		}
	}

	@Entity(name = "Item")
	@Temporal
	@RowId
	static class Item {
		@Id
		int id;
		String name;

		Item() {
		}

		Item(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
