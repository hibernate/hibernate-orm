package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.Temporal;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.testing.orm.junit.DialectContext.awaitTimestampTick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@JiraKey("HHH-21276")
@DomainModel(annotatedClasses = {
		TemporalRemoveInSameChangesetTest.Item.class,
		TemporalRemoveInSameChangesetTest.VersionedItem.class
})
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "graph"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalRemoveInSameChangesetTest.SUPPLIER)
})
class TemporalRemoveInSameChangesetTest {
	static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void updateThenRemove(SessionFactoryScope scope) {
		final var before = scope.fromTransaction( session -> {
			final var item = new Item();
			item.name = "original";
			session.persist( item );
			return session.getCurrentChangesetIdentifier();
		} );
		awaitTimestampTick();
		final var after = scope.fromTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			item.name = "updated";
			session.flush();
			session.remove( item );
			return session.getCurrentChangesetIdentifier();
		} );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession()) {
			assertEquals( "original", session.find( Item.class, 1 ).name );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( after ).openSession()) {
			assertNull( session.find( Item.class, 1 ) );
		}
		scope.inSession( session -> assertNull( session.find( Item.class, 1 ) ) );
		assertEquals( 1L, countRevisions( scope, Item.class ) );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void persistThenRemove(boolean flush, SessionFactoryScope scope) {
		final var changeset = scope.fromTransaction( session -> {
			final var item = new VersionedItem( 1 );
			session.persist( item );
			if ( flush ) {
				session.flush();
			}
			session.remove( item );
			return session.getCurrentChangesetIdentifier();
		} );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			assertNull( session.find( VersionedItem.class, 1 ) );
		}
		scope.inSession( session -> assertNull( session.find( VersionedItem.class, 1 ) ) );
		assertEquals( 0L, countRevisions( scope, VersionedItem.class ) );
	}

	@Test
	void removeOldAndNewRevisionsInOneFlush(SessionFactoryScope scope) {
		final var before = scope.fromTransaction( session -> {
			session.persist( new VersionedItem( 1 ) );
			session.persist( new VersionedItem( 2 ) );
			return session.getCurrentChangesetIdentifier();
		} );
		awaitTimestampTick();
		scope.inTransaction( session -> {
			session.setJdbcBatchSize( 5 );
			final var oldItem = session.find( VersionedItem.class, 1 );
			final var updatedItem = session.find( VersionedItem.class, 2 );
			updatedItem.name = "updated";
			session.flush();
			session.remove( oldItem );
			session.remove( updatedItem );
		} );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession()) {
			assertEquals( "original", session.find( VersionedItem.class, 1 ).name );
			assertEquals( "original", session.find( VersionedItem.class, 2 ).name );
		}
		scope.inSession( session -> {
			assertNull( session.find( VersionedItem.class, 1 ) );
			assertNull( session.find( VersionedItem.class, 2 ) );
		} );
		assertEquals( 2L, countRevisions( scope, VersionedItem.class ) );
	}

	private static long countRevisions(SessionFactoryScope scope, Class<?> entityType) {
		final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( entityType );
		return scope.fromSession( session -> session.createNativeQuery(
				"select count(*) from " + persister.getTemporalMapping().getTableName(), Long.class ).getSingleResult() );
	}

	@Entity(name = "Item")
	@Temporal
	static class Item {
		@Id int id = 1;
		String name;
	}

	@Entity(name = "VersionedItem")
	@Temporal
	static class VersionedItem {
		@Id int id;
		@Version int version;
		String name = "original";

		VersionedItem() {
		}

		VersionedItem(int id) {
			this.id = id;
		}
	}
}
