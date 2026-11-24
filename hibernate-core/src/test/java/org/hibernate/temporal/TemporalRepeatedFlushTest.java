package org.hibernate.temporal;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

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

@JiraKey("HHH-21220")
class TemporalRepeatedFlushTest {
	private static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";
	private static final String HQL = "select i.name from Item i order by i.id";

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

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void persistThenUpdateSingleTable(SessionFactoryScope scope) {
		persistThenUpdate( scope );
	}

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void persistThenUpdateHistoryTable(SessionFactoryScope scope) {
		persistThenUpdate( scope );
	}

	private void updateTwice(SessionFactoryScope scope) {
		final var before = scope.fromTransaction( session -> {
			session.persist( new Item( 1, "a" ) );
			return session.getCurrentChangesetIdentifier();
		} );
		final var revisions = scope.fromSession( session -> {
			final var transaction = session.beginTransaction();
			final var item = session.find( Item.class, 1 );
			item.name = "b";
			session.flush();
			session.clear();
			session.find( Item.class, 1 ).name = "c";
			final var after = session.getCurrentChangesetIdentifier();
			transaction.commit();

			session.beginTransaction();
			session.find( Item.class, 1 ).name = "d";
			final var next = session.getCurrentChangesetIdentifier();
			session.getTransaction().commit();
			return List.of( after, next );
		} );
		assertNames( scope, before, "a" );
		assertNames( scope, revisions.get( 0 ), "c" );
		assertNames( scope, revisions.get( 1 ), "d" );
	}

	private void persistThenUpdate(SessionFactoryScope scope) {
		final var changeset = scope.fromTransaction( session -> {
			final var item = new Item( 1, "a" );
			session.persist( item );
			session.flush();
			item.name = "b";
			return session.getCurrentChangesetIdentifier();
		} );
		assertNames( scope, changeset, "b" );
	}

	private static void assertNames(SessionFactoryScope scope, Object changeset, String... names) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			assertEquals( List.of( names ), session.createSelectionQuery( HQL, String.class ).list() );
		}
	}

	@Entity(name = "Item")
	@Temporal
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
