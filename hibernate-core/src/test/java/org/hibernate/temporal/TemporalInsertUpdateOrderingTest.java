package org.hibernate.temporal;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;

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

@JiraKey("HHH-21217")
class TemporalInsertUpdateOrderingTest {
	private static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void singleTable(SessionFactoryScope scope) {
		insertAndUpdateInOneFlush( scope );
	}

	@Test
	@SessionFactory
	@DomainModel(annotatedClasses = Item.class)
	@ServiceRegistry(settings = {
			@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
			@Setting(name = CHANGESET_ID_SUPPLIER, value = SUPPLIER)
	})
	void historyTable(SessionFactoryScope scope) {
		insertAndUpdateInOneFlush( scope );
	}

	private void insertAndUpdateInOneFlush(SessionFactoryScope scope) {
		final var before = scope.fromTransaction( session -> {
			session.persist( new Item( 1, "original" ) );
			return session.getCurrentChangesetIdentifier();
		} );
		final var after = scope.fromTransaction( session -> {
			final var existing = session.find( Item.class, 1 );
			session.persist( new Item( 2, "new" ) );
			existing.name = "updated";
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );

		final String hql = "select i.name from Item i order by i.id";
		final var factory = scope.getSessionFactory();
		try (var session = factory.withOptions().atChangeset( before ).openSession()) {
			assertEquals( List.of( "original" ), session.createSelectionQuery( hql, String.class ).list() );
		}
		try (var session = factory.withOptions().atChangeset( after ).openSession()) {
			assertEquals( List.of( "updated", "new" ), session.createSelectionQuery( hql, String.class ).list() );
		}
		scope.inSession( session -> assertEquals( List.of( "updated", "new" ),
				session.createSelectionQuery( hql, String.class ).list() ) );
	}

	public static class Changesets implements ChangesetIdentifierSupplier<Integer> {
		private int next;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++next;
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
