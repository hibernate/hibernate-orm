package org.hibernate.temporal;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.Temporal;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.annotations.SourceType.DB;
import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-21285")
@SessionFactory
@DomainModel(annotatedClasses = {
		TemporalGeneratedTimestampHistoryTest.Timestamps.class,
		TemporalGeneratedTimestampHistoryTest.Versioned.class
})
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "graph"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalGeneratedPropertyHistoryTest.SUPPLIER)
})
class TemporalGeneratedTimestampHistoryTest {
	@Entity(name = "HistoryTimestamps")
	@Temporal
	static class Timestamps {
		@Id int id = 1;
		int quantity;
		@Temporal.Excluded String note;
		@Version int version;
		@CreationTimestamp Instant vmCreated;
		@UpdateTimestamp Instant vmUpdated;
		@CreationTimestamp(source = DB) Instant dbCreated;
		@UpdateTimestamp(source = DB) Instant dbUpdated;
		@Generated @ColumnDefault("current_timestamp") Instant defaultCreated;
		@Generated @ColumnDefault("42") Integer answer;

		Values values() {
			return new Values( quantity, version, vmCreated, vmUpdated, dbCreated, dbUpdated, defaultCreated, answer );
		}
	}

	record Values(int quantity, int version, Instant vmCreated, Instant vmUpdated,
			Instant dbCreated, Instant dbUpdated, Instant defaultCreated, Integer answer) {}

	@Entity(name = "HistoryGeneratedVersion")
	@Temporal
	static class Versioned {
		@Id int id = 1;
		int quantity;
		@Version @UpdateTimestamp(source = DB) Instant version;
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner does not allow updating primary key columns")
	void revisionsRetainCreationTimesAndTrackUpdateTimes(SessionFactoryScope scope) {
		var first = scope.fromTransaction( session -> {
			session.persist( new Timestamps() );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		var inserted = scope.fromSession( session -> session.find( Timestamps.class, 1 ).values() );
		assertNotNull( inserted.vmCreated );
		assertNotNull( inserted.vmUpdated );
		assertNotNull( inserted.dbCreated );
		assertNotNull( inserted.dbUpdated );
		assertNotNull( inserted.defaultCreated );
		assertEquals( 42, inserted.answer );

		var second = scope.fromTransaction( session -> {
			var current = session.find( Timestamps.class, 1 );
			current.quantity = 1;
			session.flush();
			current.quantity = 2;
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		var updated = scope.fromSession( session -> session.find( Timestamps.class, 1 ).values() );
		assertEquals( inserted.version + 2, updated.version );
		assertEquals( inserted.vmCreated, updated.vmCreated );
		assertEquals( inserted.dbCreated, updated.dbCreated );
		assertEquals( inserted.defaultCreated, updated.defaultCreated );
		try ( var before = scope.getSessionFactory().withOptions().atChangeset( first ).openSession();
				var after = scope.getSessionFactory().withOptions().atChangeset( second ).openSession() ) {
			assertEquals( inserted, before.find( Timestamps.class, 1 ).values() );
			assertEquals( updated, after.find( Timestamps.class, 1 ).values() );
		}
	}

	@Test
	void statelessUpdatePreservesCreationTimesAbsentFromTheArgument(SessionFactoryScope scope) {
		var factory = scope.getSessionFactory();
		scope.inTransaction( session -> session.persist( new Timestamps() ) );
		var created = scope.fromSession( session -> session.find( Timestamps.class, 1 ).values() );
		var changeset = factory.fromStatelessTransaction( session -> {
			var detached = new Timestamps();
			detached.version = created.version;
			detached.quantity = 2;
			session.update( detached );
			return session.unwrap( SharedSessionContractImplementor.class ).getCurrentChangesetIdentifier();
		} );
		var updated = scope.fromSession( session -> session.find( Timestamps.class, 1 ).values() );
		assertEquals( created.vmCreated, updated.vmCreated );
		assertEquals( created.dbCreated, updated.dbCreated );
		try ( var asOf = factory.withOptions().atChangeset( changeset ).openSession() ) {
			assertEquals( updated, asOf.find( Timestamps.class, 1 ).values() );
		}
	}

	@Test
	void excludedChangeKeepsTheOpenRevisionAndItsGeneratedValuesCurrent(SessionFactoryScope scope) {
		var first = scope.fromTransaction( session -> {
			session.persist( new Timestamps() );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> session.find( Timestamps.class, 1 ).note = "excluded" );
		var edited = scope.fromSession( session -> session.find( Timestamps.class, 1 ).values() );
		scope.inTransaction( session -> session.find( Timestamps.class, 1 ).quantity++ );
		try ( var asOf = scope.getSessionFactory().withOptions().atChangeset( first ).openSession() ) {
			var revision = asOf.find( Timestamps.class, 1 );
			assertEquals( "excluded", revision.note );
			assertEquals( edited, revision.values() );
		}
	}

	@Test
	void statelessUpsertReturnsTheStoredTimestampVersion(SessionFactoryScope scope) {
		var item = new Versioned();
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		assertNotNull( item.version );
		scope.inSession( session -> assertEquals( session.find( Versioned.class, item.id ).version, item.version ) );

		item.quantity++;
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		scope.inSession( session -> {
			var stored = session.find( Versioned.class, item.id );
			assertEquals( 1, stored.quantity );
			assertEquals( stored.version, item.version );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner does not allow updating primary key columns")
	void databaseGeneratedVersionAcrossFlushes(SessionFactoryScope scope) {
		var first = scope.fromTransaction( session -> {
			session.persist( new Versioned() );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		var inserted = scope.fromSession( session -> session.find( Versioned.class, 1 ).version );
		assertNotNull( inserted );
		var second = scope.fromTransaction( session -> {
			var current = session.find( Versioned.class, 1 );
			current.quantity++;
			session.flush();
			current.quantity++;
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		var updated = scope.fromSession( session -> session.find( Versioned.class, 1 ).version );
		try ( var before = scope.getSessionFactory().withOptions().atChangeset( first ).openSession();
				var after = scope.getSessionFactory().withOptions().atChangeset( second ).openSession() ) {
			assertEquals( inserted, before.find( Versioned.class, 1 ).version );
			var revision = after.find( Versioned.class, 1 );
			assertEquals( 2, revision.quantity );
			assertEquals( updated, revision.version );
		}
	}
}
