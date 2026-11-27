package org.hibernate.temporal;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.annotations.Temporal;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.h2.tools.TriggerAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static jakarta.persistence.GenerationType.IDENTITY;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@JiraKey("HHH-21285")
@RequiresDialect(H2Dialect.class)
@SessionFactory(useCollectingStatementObserver = true)
@DomainModel(annotatedClasses = {
		TemporalGeneratedPropertyHistoryTest.Item.class,
		TemporalGeneratedPropertyHistoryTest.TriggeredItem.class,
		TemporalGeneratedPropertyHistoryTest.DefaultItem.class,
		TemporalGeneratedPropertyHistoryTest.MixedItem.class,
		TemporalGeneratedPropertyHistoryTest.IdentityItem.class,
		TemporalGeneratedPropertyHistoryTest.SequenceItem.class,
		TemporalGeneratedPropertyHistoryTest.Mirror.class
})
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "graph"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalGeneratedPropertyHistoryTest.SUPPLIER)
})
public class TemporalGeneratedPropertyHistoryTest {
	static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@BeforeEach
	void installLiveTableTriggers(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( String table : new String[] { "gen_item", "gen_triggered", "gen_mirror" } ) {
				session.createNativeMutationQuery( "create trigger if not exists " + table + "_stamp "
						+ "before insert, update on " + table + " for each row call '"
						+ LiveTrigger.class.getName() + "'" ).executeUpdate();
			}
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest(name = "{displayName} [{0}]")
	@EnumSource(Property.class)
	void insert(Property property, SessionFactoryScope scope) {
		var first = scope.fromTransaction( session -> insert( session, property, new Item() ) );
		assertItemHistory( scope, property, first );
	}

	@ParameterizedTest(name = "{displayName} [{0}]")
	@EnumSource(Property.class)
	void updateInLaterChangeset(Property property, SessionFactoryScope scope) {
		var first = scope.fromTransaction( session -> insert( session, property, new Item() ) );
		var second = scope.fromTransaction( session ->
				update( session, property, session.find( Item.class, 1 ), first, 5 ) );
		assertItemHistory( scope, property, first, second );
	}

	@ParameterizedTest(name = "{displayName} [{0}]")
	@EnumSource(Property.class)
	void persistFlushThenUpdate(Property property, SessionFactoryScope scope) {
		var last = scope.fromTransaction( session -> {
			var item = new Item();
			var first = insert( session, property, item );
			return update( session, property, item, first, 5 );
		} );
		assertItemHistory( scope, property, last );
	}

	@ParameterizedTest(name = "{displayName} [{0}]")
	@EnumSource(Property.class)
	void updateFlushThenUpdate(Property property, SessionFactoryScope scope) {
		var first = scope.fromTransaction( session -> insert( session, property, new Item() ) );
		var last = scope.fromTransaction( session -> {
			var item = session.find( Item.class, 1 );
			var middle = update( session, property, item, first, 5 );
			return update( session, property, item, middle, 7 );
		} );
		assertItemHistory( scope, property, first, last );
	}

	private static Revision insert(SessionImplementor session, Property property, Item item) {
		session.persist( item );
		session.flush();
		var result = readLive( session, "gen_item", property.column, item.id );
		assertEquals( result.value, property.read( item ), "managed value immediately after insert" );
		switch ( property ) {
			case JAVA -> assertEquals( "I", result.value );
			case COMPUTED -> assertEquals( "6", result.value );
			case EVENT_SQL -> assertEquals( "created", result.value );
			case JAVA_UPDATE, UPDATE_SQL -> assertNull( result.value );
			case WRITABLE -> assertEquals( "input3!", result.value );
			default -> assertNotNull( result.value );
		}
		return result;
	}

	private static Revision update(SessionImplementor session, Property property, Item item, Revision before, int quantity) {
		item.quantity = quantity;
		item.submitted = "input" + quantity;
		session.flush();
		var result = readLive( session, "gen_item", property.column, item.id );
		assertEquals( result.value, property.read( item ), "managed value immediately after update" );
		switch ( property ) {
			case JAVA, JAVA_UPDATE -> assertEquals( Objects.toString( before.value, "" ) + "U", result.value );
			case COMPUTED -> assertEquals( String.valueOf( quantity * 2 ), result.value );
			case EVENT_SQL -> assertEquals( "updated", result.value );
			case INSERT_SQL, DB_DEFAULT -> assertEquals( before.value, result.value );
			case WRITABLE -> assertEquals( "input" + quantity + "!", result.value );
			default -> {
				assertNotNull( result.value );
				assertNotEquals( before.value, result.value, "generation on update" );
			}
		}
		return result;
	}

	enum Property {
		JAVA( "events", i -> i.events ),
		JAVA_UPDATE( "updates", i -> i.updates ),
		COMPUTED( "doubled", i -> i.doubled ),
		EVENT_SQL( "eventStamp", i -> i.eventStamp ),
		INSERT_SQL( "createdToken", i -> i.createdToken ),
		UPDATE_SQL( "updatedToken", i -> i.updatedToken ),
		INSERT_UPDATE_SQL( "token", i -> i.token ),
		DB_DEFAULT( "defaultToken", i -> i.defaultToken ),
		DB_TRIGGER( "stamp", i -> i.stamp ),
		WRITABLE( "submitted", i -> i.submitted );

		final String column;
		final Function<Item, Object> accessor;

		Property(String column, Function<Item, Object> accessor) {
			this.column = column;
			this.accessor = accessor;
		}

		String read(Item item) {
			return Objects.toString( accessor.apply( item ), null );
		}
	}

	@Entity(name = "GeneratedPropertyHistoryItem")
	@Table(name = "gen_item")
	@Temporal
	static class Item {
		@Id int id = 1;
		int quantity = 3;
		@TemporalGeneratedStateTest.RecordEvents({ INSERT, UPDATE }) String events;
		@TemporalGeneratedStateTest.RecordEvents(UPDATE) String updates;
		@GeneratedColumn("quantity * 2") Integer doubled;
		@EventStamp String eventStamp;
		@Generated(sql = "random_uuid()") UUID createdToken;
		@Generated(event = UPDATE, sql = "random_uuid()") UUID updatedToken;
		@Generated(event = { INSERT, UPDATE }, sql = "random_uuid()") UUID token;
		@Generated @ColumnDefault("random_uuid()") UUID defaultToken;
		@Generated(event = { INSERT, UPDATE }) String stamp;
		@Generated(event = { INSERT, UPDATE }, writable = true) String submitted = "input3";
	}

	@Target(FIELD)
	@Retention(RUNTIME)
	@ValueGenerationType(generatedBy = EventSql.class)
	@interface EventStamp {}

	public static class EventSql implements OnExecutionGenerator {
		@Override
		public EnumSet<EventType> getEventTypes() {
			return EnumSet.of( INSERT, UPDATE );
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return true;
		}

		@Override
		public boolean writePropertyValue() {
			return false;
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return new String[] { "'created'" };
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect, EventType eventType) {
			return new String[] { eventType == INSERT ? "'created'" : "'updated'" };
		}
	}

	public static class LiveTrigger extends TriggerAdapter {
		@Override
		public void fire(Connection connection, ResultSet oldRow, ResultSet newRow) throws SQLException {
			newRow.updateString( "STAMP", UUID.randomUUID().toString() );
			if ( tableName.equalsIgnoreCase( "gen_item" ) ) {
				newRow.updateString( "SUBMITTED", newRow.getString( "SUBMITTED" ) + "!" );
			}
		}
	}

	@Embeddable
	static class MixedProperties {
		@Generated(sql = "'created'") String createdPart;
		@Generated(event = UPDATE, sql = "'updated'") String updatedPart;
		String inputPart;
	}

	@Entity(name = "GeneratedPropertyMixedItem")
	@Table(name = "gen_mixed")
	@Temporal
	static class MixedItem {
		@Id int id = 1;
		@Embedded MixedProperties parts = new MixedProperties();
	}

	@Test
	void mixedGenerationEventsInAnEmbeddable(SessionFactoryScope scope) {
		var item = new MixedItem();
		item.parts.inputPart = "one";
		var first = scope.fromTransaction( session -> {
			session.persist( item );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		var second = scope.fromTransaction( session -> {
			var current = session.find( MixedItem.class, 1 );
			current.parts.inputPart = "two";
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inSession( session -> {
			var parts = session.find( MixedItem.class, 1 ).parts;
			assertAll( "live embeddable", () -> assertEquals( "created", parts.createdPart ),
					() -> assertEquals( "updated", parts.updatedPart ), () -> assertEquals( "two", parts.inputPart ) );
		} );
		assertAll(
				() -> assertHistory( scope, "gen_mixed", "createdPart", 1, s -> s.find( MixedItem.class, 1 ).parts.createdPart,
						null, new Revision( first, "created" ), new Revision( second, "created" ) ),
				() -> assertHistory( scope, "gen_mixed", "updatedPart", 1, s -> s.find( MixedItem.class, 1 ).parts.updatedPart,
						null, new Revision( first, null ), new Revision( second, "updated" ) ),
				() -> assertHistory( scope, "gen_mixed", "inputPart", 1, s -> s.find( MixedItem.class, 1 ).parts.inputPart,
						null, new Revision( first, "one" ), new Revision( second, "two" ) )
		);
	}

	@Entity(name = "GeneratedPropertyTriggeredItem")
	@Table(name = "gen_triggered")
	@Temporal
	static class TriggeredItem {
		@Id int id = 1;
		int quantity = 1;
		@Temporal.Excluded String note;
		@Temporal.Excluded
		@Column(insertable = false, updatable = false)
		@Generated(event = { INSERT, UPDATE })
		String stamp;
	}

	private static Revision stateless(SessionFactoryScope scope, Consumer<StatelessSession> write) {
		return scope.getSessionFactory().fromStatelessTransaction( session -> {
			write.accept( session );
			var revision = readLive( session.unwrap( SharedSessionContractImplementor.class ), "gen_triggered", "stamp", 1 );
			assertNotNull( revision.value, "trigger value in the live table" );
			return revision;
		} );
	}

	@Test
	void statelessInsert(SessionFactoryScope scope) {
		var first = stateless( scope, session -> session.insert( new TriggeredItem() ) );
		assertTriggeredHistory( scope, null, first );
	}

	@Test
	void statelessUpdate(SessionFactoryScope scope) {
		var first = stateless( scope, session -> session.insert( new TriggeredItem() ) );
		var second = stateless( scope, session -> {
			var item = session.find( TriggeredItem.class, 1 );
			item.quantity++;
			session.update( item );
		} );
		assertNotEquals( first.value, second.value );
		assertTriggeredHistory( scope, null, first, second );
	}

	@Test
	void statelessInsertThenUpdate(SessionFactoryScope scope) {
		var last = stateless( scope, session -> {
			var item = new TriggeredItem();
			session.insert( item );
			item.quantity++;
			session.update( item );
		} );
		assertTriggeredHistory( scope, null, last );
	}

	@Test
	void statelessUpsertExisting(SessionFactoryScope scope) {
		var first = stateless( scope, session -> session.insert( new TriggeredItem() ) );
		var second = stateless( scope, session -> {
			var item = session.find( TriggeredItem.class, 1 );
			item.quantity++;
			session.upsert( item );
		} );
		assertNotEquals( first.value, second.value );
		assertTriggeredHistory( scope, null, first, second );
	}

	@Test
	void repeatedStatelessUpsertOfExistingRow(SessionFactoryScope scope) {
		var first = stateless( scope, session -> session.insert( new TriggeredItem() ) );
		var last = stateless( scope, session -> {
			var item = session.find( TriggeredItem.class, 1 );
			item.quantity++;
			session.upsert( item );
			item.quantity++;
			session.upsert( item );
		} );
		assertNotEquals( first.value, last.value );
		assertTriggeredHistory( scope, null, first, last );
	}

	@Entity(name = "GeneratedPropertyDefaultItem")
	@Table(name = "gen_default")
	@Temporal
	static class DefaultItem {
		@Id int id = 1;
		int quantity = 1;
		@Column(insertable = false, updatable = false)
		@Generated @ColumnDefault("random_uuid()") UUID token;
	}

	@Test
	void statelessUpsertNewWithInsertOnlyDefault(SessionFactoryScope scope) {
		var first = scope.getSessionFactory().fromStatelessTransaction( session -> {
			session.upsert( new DefaultItem() );
			return readLive( session.unwrap( SharedSessionContractImplementor.class ), "gen_default", "token", 1 );
		} );
		assertNotNull( first.value );
		assertHistory( scope, "gen_default", "token", 1,
				session -> session.find( DefaultItem.class, 1 ).token.toString(), null, first );
	}

	@Test
	void excludedOnlyUpdatePreservesClosedRevision(SessionFactoryScope scope) {
		var first = scope.fromTransaction( session -> {
			session.persist( new TriggeredItem() );
			session.flush();
			return readLive( session, "gen_triggered", "stamp", 1 );
		} );
		var second = scope.fromTransaction( session -> {
			session.find( TriggeredItem.class, 1 ).quantity++;
			session.flush();
			return readLive( session, "gen_triggered", "stamp", 1 );
		} );
		var excluded = scope.fromTransaction( session -> {
			session.find( TriggeredItem.class, 1 ).note = "new note";
			session.flush();
			return readLive( session, "gen_triggered", "stamp", 1 );
		} );
		assertNotEquals( second.value, excluded.value );
		assertTriggeredHistory( scope, null, first, new Revision( second.changeset, excluded.value ) );
	}

	@Test
	void deleteClosesTheLastStoredValue(SessionFactoryScope scope) {
		var first = scope.fromTransaction( session -> insert( session, Property.INSERT_UPDATE_SQL, new Item() ) );
		var stored = storedToken( scope, first.changeset );
		var deleted = scope.fromTransaction( session -> {
			session.remove( session.find( Item.class, 1 ) );
			return session.getCurrentChangesetIdentifier();
		} );
		try ( var asOf = scope.getSessionFactory().withOptions().atChangeset( deleted ).openSession() ) {
			assertNull( asOf.find( Item.class, 1 ) );
		}
		assertHistory( scope, "gen_item", "token", 1,
				session -> session.find( Item.class, 1 ).token.toString(), deleted, stored );
	}

	@Test
	void insertThenDeleteLeavesNoGeneratedHistory(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var item = new TriggeredItem();
			session.persist( item );
			session.flush();
			session.remove( item );
		} );
		assertTriggeredHistory( scope, null );
	}

	@Test
	void rolledBackUpdatePreservesHistory(SessionFactoryScope scope) {
		var first = scope.fromTransaction( session -> insert( session, Property.INSERT_UPDATE_SQL, new Item() ) );
		var stored = storedToken( scope, first.changeset );
		scope.inSession( session -> {
			var transaction = session.beginTransaction();
			try {
				update( session, Property.INSERT_UPDATE_SQL, session.find( Item.class, 1 ), first, 5 );
			}
			finally {
				transaction.rollback();
			}
		} );
		scope.inSession( session -> assertEquals( first.value, session.find( Item.class, 1 ).token.toString() ) );
		assertItemHistory( scope, Property.INSERT_UPDATE_SQL, stored );
	}

	// Isolate closure and rollback from any discrepancy introduced during the initial insert.
	private static Revision storedToken(SessionFactoryScope scope, Object changeset) {
		var token = scope.fromSession( session -> session.createNativeQuery(
				"select cast(token as varchar) from gen_item_history where id = 1 and superseded is null",
				String.class ).getSingleResult() );
		assertNotNull( token );
		return new Revision( changeset, token );
	}

	@Entity(name = "GeneratedPropertyIdentityItem")
	@Table(name = "gen_identity")
	@Temporal
	static class IdentityItem {
		@Id @GeneratedValue(strategy = IDENTITY) Integer id;
		int quantity;
		@Generated(event = { INSERT, UPDATE }, sql = "random_uuid()") UUID token;
	}

	@Test
	void generatedValuesBelongToTheRightIdentity(SessionFactoryScope scope) {
		var a = new IdentityItem();
		var b = new IdentityItem();
		var changeset = scope.fromTransaction( session -> {
			session.setJdbcBatchSize( 5 );
			session.persist( a );
			session.persist( b );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		assertNotEquals( a.id, b.id );
		assertNotEquals( a.token, b.token );
		assertAll( Stream.of( a, b ).map( item -> () -> {
			var current = scope.fromSession( session -> session.find( IdentityItem.class, item.id ).token );
			assertNotNull( current );
			assertEquals( current, item.token );
			assertHistory( scope, "gen_identity", "token", item.id,
					session -> session.find( IdentityItem.class, item.id ).token.toString(), null,
					new Revision( changeset, current.toString() ) );
		} ) );
	}

	@Entity(name = "GeneratedPropertyMirror")
	@Table(name = "gen_mirror")
	@Temporal
	static class Mirror {
		@Id int id = 1;
		@Column(name = "stamp") String input;
		@Column(name = "stamp", insertable = false, updatable = false)
		@Generated(event = { INSERT, UPDATE }) String actual;
	}

	@Test
	void generatedReadOnlyViewOfAWritableColumn(SessionFactoryScope scope) {
		var first = scope.fromTransaction( session -> {
			var item = new Mirror();
			item.input = "one";
			session.persist( item );
			session.flush();
			return readLive( session, "gen_mirror", "stamp", 1 );
		} );
		var last = scope.fromTransaction( session -> {
			var item = session.find( Mirror.class, 1 );
			item.input = "two";
			session.flush();
			item.input = "three";
			session.flush();
			return readLive( session, "gen_mirror", "stamp", 1 );
		} );
		assertNotEquals( first.value, last.value );
		assertHistory( scope, "gen_mirror", "stamp", 1,
				session -> session.find( Mirror.class, 1 ).actual, null, first, last );
	}

	@Embeddable
	record SequenceId(int ledger, int entryNumber) {}

	@Entity(name = "GeneratedPropertySequenceItem")
	@Table(name = "gen_sequence_item")
	@Temporal
	@DynamicInsert
	@DynamicUpdate
	static class SequenceItem {
		@EmbeddedId SequenceId id;
		String description;
		@Generated(event = { INSERT, UPDATE }, sql = "next value for gen_history_sequence")
		@Column(nullable = false) Long serialNumber;

		SequenceItem() {}

		SequenceItem(int entryNumber) {
			id = new SequenceId( 1, entryNumber );
		}
	}

	@Test
	void consumesOneSequenceValuePerLiveWriteWithCompositeKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"create sequence if not exists gen_history_sequence" ).executeUpdate() );
		var a = new SequenceItem( 10 );
		var b = new SequenceItem( 20 );
		var first = scope.fromTransaction( session -> {
			session.setJdbcBatchSize( 5 );
			session.persist( a );
			session.persist( b );
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		assertEquals( 1L, Math.abs( a.serialNumber - b.serialNumber ) );
		var second = scope.fromTransaction( session -> {
			var item = session.find( SequenceItem.class, b.id );
			item.description = "second revision";
			session.flush();
			item.description = "same revision";
			session.flush();
			return session.getCurrentChangesetIdentifier();
		} );
		var last = Math.max( a.serialNumber, b.serialNumber ) + 2;
		try ( var before = scope.getSessionFactory().withOptions().atChangeset( first ).openSession();
				var after = scope.getSessionFactory().withOptions().atChangeset( second ).openSession() ) {
			assertEquals( a.serialNumber, before.find( SequenceItem.class, a.id ).serialNumber );
			assertEquals( b.serialNumber, before.find( SequenceItem.class, b.id ).serialNumber );
			assertEquals( a.serialNumber, after.find( SequenceItem.class, a.id ).serialNumber );
			assertEquals( last, after.find( SequenceItem.class, b.id ).serialNumber );
		}
		scope.inSession( session -> {
			assertEquals( last, session.find( SequenceItem.class, b.id ).serialNumber );
			assertEquals( last + 1, session.createNativeQuery(
					"select next value for gen_history_sequence", Long.class ).getSingleResult() );
		} );
	}

	@Test
	void revisionReadsTheLiveRowOnce(SessionFactoryScope scope) {
		var observer = scope.getCollectingStatementObserver();
		observer.clear();
		scope.inTransaction( session -> session.persist( new Item() ) );
		observer.assertQueries().filteredOn( sql -> sql.contains( "insert into gen_item_history " ) )
				.singleElement().asString().containsOnlyOnce( " from gen_item " )
				.contains( ") select " ).doesNotContain( "(select " );
	}

	@Test
	void historyIsWrittenOnceWhenTheTransactionCompletes(SessionFactoryScope scope) {
		var observer = scope.getCollectingStatementObserver();
		observer.clear();
		scope.inTransaction( session -> {
			var item = new TriggeredItem();
			session.persist( item );
			session.flush();
			item.quantity++;
			session.flush();
			// a flush writes only the table holding current data
			observer.assertQueries().isNotEmpty().noneMatch( sql -> sql.contains( "gen_triggered_history" ) );
		} );
		// an entity created in the changeset has one revision, however often it was flushed
		observer.assertQueries().filteredOn( sql -> sql.contains( "gen_triggered_history" ) )
				.singleElement().asString().startsWith( "insert into gen_triggered_history " );

		observer.clear();
		scope.inTransaction( session -> {
			session.find( TriggeredItem.class, 1 ).quantity++;
			session.flush();
			observer.assertQueries().isNotEmpty().noneMatch( sql -> sql.contains( "gen_triggered_history" ) );
		} );
		// the open revision is ended, and the current row is copied to a new revision
		observer.assertQueries().filteredOn( sql -> sql.contains( "gen_triggered_history" ) )
				.hasSize( 2 )
				.anyMatch( sql -> sql.startsWith( "update gen_triggered_history set " ) )
				.anyMatch( sql -> sql.startsWith( "insert into gen_triggered_history " ) );
	}

	private record Revision(Object changeset, String value) {}

	private static Revision readLive(SharedSessionContractImplementor session, String table, String column, int id) {
		var value = session.createNativeQuery( "select cast(" + column + " as varchar) from " + table + " where id = :id",
						String.class ).setParameter( "id", id ).getSingleResult();
		return new Revision( session.getCurrentChangesetIdentifier(), value );
	}

	private static void assertItemHistory(SessionFactoryScope scope, Property property, Revision... revisions) {
		assertHistory( scope, "gen_item", property.column, 1,
				session -> property.read( session.find( Item.class, 1 ) ), null, revisions );
	}

	private static void assertTriggeredHistory(SessionFactoryScope scope, Object end, Revision... revisions) {
		assertHistory( scope, "gen_triggered", "stamp", 1,
				session -> session.find( TriggeredItem.class, 1 ).stamp, end, revisions );
	}

	private static void assertHistory(SessionFactoryScope scope, String table, String column, int id,
			Function<Session, String> historicalValue, Object end, Revision... expected) {
		var rows = scope.fromSession( session -> session.createNativeQuery(
						"select effective, superseded, cast(" + column + " as varchar) from " + table
								+ "_history where id = :id order by effective", Object[].class )
				.setParameter( "id", id ).getResultList() );
		assertEquals( expected.length, rows.size(), "revision count" );
		assertAll( IntStream.range( 0, expected.length ).mapToObj( i -> () -> {
			var row = rows.get( i );
			var revision = expected[i];
			try ( var asOf = scope.getSessionFactory().withOptions().atChangeset( revision.changeset ).openSession() ) {
				assertAll( "changeset " + revision.changeset,
						() -> assertEquals( revision.changeset, row[0], "effective" ),
						() -> assertEquals( i + 1 < expected.length ? expected[i + 1].changeset : end, row[1], "superseded" ),
						() -> assertEquals( revision.value, row[2], "physical history value" ),
						() -> assertEquals( revision.value, historicalValue.apply( asOf ), "atChangeset value" ) );
			}
		} ) );
	}
}
