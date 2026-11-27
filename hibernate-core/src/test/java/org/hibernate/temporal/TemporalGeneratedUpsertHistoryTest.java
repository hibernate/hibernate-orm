package org.hibernate.temporal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.h2.tools.TriggerAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JdbcSettings.USE_GET_GENERATED_KEYS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@JiraKey("HHH-21285")
@RequiresDialect(H2Dialect.class)
@SessionFactory(useCollectingStatementObserver = true)
@DomainModel(annotatedClasses = {
		TemporalGeneratedUpsertHistoryTest.Tracked.class, TemporalGeneratedUpsertHistoryTest.Versioned.class,
		TemporalGeneratedUpsertHistoryTest.PlainVersion.class, TemporalGeneratedUpsertHistoryTest.EventProperties.class
})
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = USE_GET_GENERATED_KEYS, value = "false"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalGeneratedPropertyHistoryTest.SUPPLIER)
})
class TemporalGeneratedUpsertHistoryTest {
	@Entity(name = "HistoryUpsertTracked")
	@Temporal
	static class Tracked {
		@Id int id = 1;
		int quantity;
		@Generated(event = { INSERT, UPDATE }, sql = "random_uuid()") UUID stamp;
		@Generated(event = { INSERT, UPDATE }, sql = "cast(null as varchar(30))") String nullableStamp = "initial";
	}

	@Test
	void upsertReturnsTheGeneratedValueAndCopiesItIntoHistory(SessionFactoryScope scope) {
		var observer = scope.getCollectingStatementObserver();
		var item = new Tracked();
		var factory = scope.getSessionFactory();
		var first = factory.fromStatelessTransaction( session -> {
			observer.clear();
			session.upsert( item );
			assertGeneratedUpsertStatements( observer.getSqlQueries() );
			assertNotNull( item.stamp );
			assertNull( item.nullableStamp );
			return session.unwrap( SharedSessionContractImplementor.class ).getCurrentChangesetIdentifier();
		} );
		var created = scope.fromSession( session -> session.find( Tracked.class, 1 ).stamp );
		assertEquals( created, item.stamp );
		var second = factory.fromStatelessTransaction( session -> {
			item.quantity++;
			session.upsert( item );
			return session.unwrap( SharedSessionContractImplementor.class ).getCurrentChangesetIdentifier();
		} );
		var updated = scope.fromSession( session -> session.find( Tracked.class, 1 ).stamp );
		assertEquals( updated, item.stamp );
		assertNotEquals( created, updated );
		try ( var before = factory.withOptions().atChangeset( first ).openSession();
				var after = factory.withOptions().atChangeset( second ).openSession() ) {
			assertEquals( created, before.find( Tracked.class, 1 ).stamp );
			assertEquals( updated, after.find( Tracked.class, 1 ).stamp );
		}
	}

	@Entity(name = "HistoryUpsertGeneratedVersion")
	@Table(name = "gen_upsert_version")
	@Temporal
	static class Versioned {
		@Id int id = 1;
		int quantity;
		@Version @Generated(event = { INSERT, UPDATE }) @ColumnDefault("0")
		@Column(insertable = false, updatable = false) Integer version;
	}

	public static class VersionTrigger extends TriggerAdapter {
		@Override
		public void fire(Connection connection, ResultSet oldRow, ResultSet newRow) throws SQLException {
			newRow.updateInt( "VERSION", oldRow == null ? 43 : oldRow.getInt( "VERSION" ) + 7 );
		}
	}

	@BeforeEach
	void installVersionTrigger(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"create trigger if not exists generate_version before insert, update on gen_upsert_version "
						+ "for each row call '" + VersionTrigger.class.getName() + "'" ).executeUpdate() );
	}

	@Test
	void statelessInsertThenUpdateCopiesTheGeneratedVersionIntoHistory(SessionFactoryScope scope) {
		var item = new Versioned();
		item.id = 2;
		var factory = scope.getSessionFactory();
		var changeset = factory.fromStatelessTransaction( session -> {
			session.setJdbcBatchSize( 10 );
			session.insert( item );
			assertEquals( 43, item.version );
			item.quantity++;
			session.update( item );
			assertEquals( 50, item.version );
			return session.unwrap( SharedSessionContractImplementor.class ).getCurrentChangesetIdentifier();
		} );
		try ( var asOf = factory.withOptions().atChangeset( changeset ).openSession() ) {
			assertEquals( 50, asOf.find( Versioned.class, 2 ).version );
		}
	}

	@Test
	void statelessUpsertReturnsTheDatabaseVersion(SessionFactoryScope scope) {
		var item = new Versioned();
		var factory = scope.getSessionFactory();
		var observer = scope.getCollectingStatementObserver();
		var first = factory.fromStatelessTransaction( session -> {
			observer.clear();
			session.upsert( item );
			assertGeneratedUpsertStatements( observer.getSqlQueries() );
			assertEquals( 43, item.version );
			return session.unwrap( SharedSessionContractImplementor.class ).getCurrentChangesetIdentifier();
		} );
		var second = factory.fromStatelessTransaction( session -> {
			item.quantity++;
			observer.clear();
			session.upsert( item );
			assertGeneratedUpsertStatements( observer.getSqlQueries() );
			assertEquals( 50, item.version );
			return session.unwrap( SharedSessionContractImplementor.class ).getCurrentChangesetIdentifier();
		} );
		try ( var before = factory.withOptions().atChangeset( first ).openSession();
				var after = factory.withOptions().atChangeset( second ).openSession() ) {
			assertEquals( 43, before.find( Versioned.class, 1 ).version );
			var revision = after.find( Versioned.class, 1 );
			assertEquals( 1, revision.quantity );
			assertEquals( 50, revision.version );
		}
	}

	@Entity(name = "PlainStatelessVersion")
	static class PlainVersion {
		@Id int id = 1;
		@Version Integer version;
		int quantity;
	}

	@Test
	void plainVersionDoesNotFetchGeneratedValues(SessionFactoryScope scope) {
		var item = new PlainVersion();
		var observer = scope.getCollectingStatementObserver();
		scope.inStatelessTransaction( session -> {
			observer.clear();
			session.insert( item );
			assertEquals( 0, item.version );
			item.quantity++;
			session.update( item );
			assertEquals( 1, item.version );
			item.quantity++;
			session.upsert( item );
			assertEquals( 2, item.version );
			observer.assertQueries().noneMatch( sql -> sql.startsWith( "select" ) );
		} );
	}

	@Entity(name = "StatelessEventProperties")
	static class EventProperties {
		@Id int id = 1;
		@Generated(event = INSERT, sql = "'created'") @Column(updatable = false) String created;
		@Generated(event = UPDATE, sql = "'updated'") String modified;
		int quantity;
	}

	@Test
	void statelessWritesApplyOnlyPropertiesGeneratedForTheEvent(SessionFactoryScope scope) {
		var item = new EventProperties();
		scope.inStatelessTransaction( session -> {
			session.insert( item );
			assertEquals( "created", item.created );
			assertNull( item.modified );
			item.created = "in memory";
			item.quantity++;
			session.update( item );
			assertEquals( "in memory", item.created );
			assertEquals( "updated", item.modified );
		} );
	}

	void assertGeneratedUpsertStatements(List<String> statements) {
		// A data change table returns the generated values in the upsert's own statement.
		assertEquals( expectedFollowUpSelects(), statements.stream()
				.filter( sql -> sql.startsWith( "select" ) && !sql.contains( "merge into" ) )
				.count() );
	}

	int expectedFollowUpSelects() {
		return 0;
	}
}
