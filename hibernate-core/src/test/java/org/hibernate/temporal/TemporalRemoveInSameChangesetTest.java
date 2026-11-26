package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@JiraKey("HHH-21276")
@DomainModel(annotatedClasses = {
		TemporalRemoveInSameChangesetTest.Item.class,
		TemporalRemoveInSameChangesetTest.GeneratedItem.class,
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

	@Test
	void statelessInsertThenDelete(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var item = new GeneratedItem();
			session.insert( item );
			session.delete( session.get( GeneratedItem.class, item.id ) );
			assertNull( session.find( GeneratedItem.class, item.id ) );
		} );
		assertEquals( 0L, countRevisions( scope, GeneratedItem.class ) );
	}

	@Test
	void statelessUpdateThenDelete(SessionFactoryScope scope) {
		final var before = scope.fromTransaction( session -> {
			final var item = new Item();
			item.name = "original";
			session.persist( item );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inStatelessTransaction( session -> {
			final var item = session.get( Item.class, 1 );
			item.name = "updated";
			session.update( item );
			session.delete( item );
		} );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession()) {
			assertEquals( "original", session.find( Item.class, 1 ).name );
		}
		scope.inSession( session -> assertNull( session.find( Item.class, 1 ) ) );
		assertEquals( 1L, countRevisions( scope, Item.class ) );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner does not allow updating primary key columns")
	@SkipForDialect(dialectClass = SpannerPostgreSQLDialect.class, reason = "Spanner does not allow updating primary key columns")
	void reuseStatelessSessionAfterUpdate(boolean commit, SessionFactoryScope scope) {
		scope.inStatelessSession( session -> {
			final var inserted = session.fromTransaction( tx -> {
				session.insert( new VersionedItem( 1 ) );
				return session.getCurrentChangesetIdentifier();
			} );
			final var transaction = session.beginTransaction();
			final var item = session.get( VersionedItem.class, 1 );
			item.name = "first update";
			session.update( item );
			item.name = "second update";
			session.update( item );
			final var updated = session.getCurrentChangesetIdentifier();
			if ( commit ) {
				transaction.commit();
			}
			else {
				transaction.rollback();
			}
			session.inTransaction( tx -> session.delete( session.get( VersionedItem.class, 1 ) ) );

			try (var history = scope.getSessionFactory().withOptions().atChangeset( inserted ).openSession()) {
				final var original = history.find( VersionedItem.class, 1 );
				assertEquals( "original", original.name );
				assertEquals( 0, original.version );
			}
			try (var history = scope.getSessionFactory().withOptions().atChangeset( updated ).openSession()) {
				final var revision = history.find( VersionedItem.class, 1 );
				assertEquals( commit ? "second update" : "original", revision.name );
				assertEquals( commit ? 2 : 0, revision.version );
			}
			assertNull( session.find( VersionedItem.class, 1 ) );
		} );
		assertEquals( commit ? 2L : 1L, countRevisions( scope, VersionedItem.class ) );
	}

	@Test
	void reuseStatelessSessionSharingTransaction(SessionFactoryScope scope) {
		scope.inSession( owner -> {
			try (var session = owner.statelessWithOptions().connection().open()) {
				owner.inTransaction( tx -> session.insert( new VersionedItem( 1 ) ) );
				owner.inTransaction( tx -> session.delete( session.get( VersionedItem.class, 1 ) ) );
			}
		} );
		assertEquals( 1L, countRevisions( scope, VersionedItem.class ) );
		scope.inSession( session -> assertNull( session.find( VersionedItem.class, 1 ) ) );
	}

	static long countRevisions(SessionFactoryScope scope, Class<?> entityType) {
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

	@Entity(name = "GeneratedItem")
	@Temporal
	static class GeneratedItem {
		@Id @GeneratedValue Long id;
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
