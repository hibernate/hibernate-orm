package org.hibernate.temporal;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.RowId;
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

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-21220")
@SessionFactory
@DomainModel(annotatedClasses = { TemporalRepeatedFlushTest.Item.class, TemporalRepeatedFlushTest.VersionedItem.class })
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "graph"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalRepeatedFlushTest.SUPPLIER)
})
class TemporalRepeatedFlushTest {
	static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";
	private static final String HQL = "select i.name from Item i order by i.id";

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void updateTwice(SessionFactoryScope scope) {
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

	@Test
	void persistThenUpdate(SessionFactoryScope scope) {
		final var changeset = scope.fromTransaction( session -> {
			final var item = new Item( 1, "a" );
			session.persist( item );
			session.flush();
			item.name = "b";
			return session.getCurrentChangesetIdentifier();
		} );
		assertNames( scope, changeset, "b" );
	}

	@Test
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner does not allow updating primary key columns")
	@SkipForDialect(dialectClass = SpannerPostgreSQLDialect.class, reason = "Spanner does not allow updating primary key columns")
	void updateVersionedRevision(SessionFactoryScope scope) {
		final var before = scope.fromTransaction( session -> {
			session.persist( new VersionedItem() );
			return session.getCurrentChangesetIdentifier();
		} );
		final var after = scope.fromTransaction( session -> {
			final var item = session.find( VersionedItem.class, 1 );
			item.name = "first update";
			session.flush();
			item.name = "second update";
			return session.getCurrentChangesetIdentifier();
		} );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( before ).openSession()) {
			final var item = session.find( VersionedItem.class, 1 );
			assertEquals( "original", item.name );
			assertEquals( 0, item.version );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( after ).openSession()) {
			final var item = session.find( VersionedItem.class, 1 );
			assertEquals( "second update", item.name );
			assertEquals( 2, item.version );
		}
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

	@Entity(name = "VersionedItem")
	@Temporal
	@RowId
	static class VersionedItem {
		@Id int id = 1;
		@Version int version;
		String name = "original";
	}
}
