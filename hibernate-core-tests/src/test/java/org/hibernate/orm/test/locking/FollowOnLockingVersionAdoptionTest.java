package org.hibernate.orm.test.locking;

import java.sql.SQLTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.QueryTimeoutException;
import org.hibernate.ScrollMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Acquired versions drive flush and force increment across mappings, consumers,
/// read-only state, duplicate rows, and cached plans. Both enhancement variants run.
///
/// @author Steve Ebersole
@DomainModel(overrideCacheStrategy = false, annotatedClasses = {
		FollowOnLockingVersionAdoptionTest.Item.class, FollowOnLockingVersionAdoptionTest.PropertyItem.class,
		FollowOnLockingVersionAdoptionTest.TimestampItem.class, FollowOnLockingVersionAdoptionTest.MultiItem.class
})
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true")
})
@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
@BytecodeEnhanced(runNotEnhancedAsWell = true, testEnhancedClasses = {
		FollowOnLockingVersionAdoptionTest.Item.class, FollowOnLockingVersionAdoptionTest.PropertyItem.class
})
@Tag("db-locking")
@Jira("https://hibernate.atlassian.net/browse/HHH-21029")
class FollowOnLockingVersionAdoptionTest {
	private static final Timestamp INITIAL_TIME = Timestamp.valueOf( "2000-01-01 00:00:00" );
	private static final Timestamp ACQUIRED_TIME = Timestamp.valueOf( "2001-01-01 00:00:00" );

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			session.persist( new Item( 1 ) );
			session.persist( new Item( 2 ) );
			session.persist( new PropertyItem() );
			session.persist( new TimestampItem() );
			session.persist( new MultiItem() );
		} );
		scope.inTransaction( session -> session.doWork( connection -> {
			try ( var statement = connection.prepareStatement( "update adopt_timestamp set version_value = ? where id = 1" ) ) {
				statement.setTimestamp( 1, INITIAL_TIME );
				assertThat( statement.executeUpdate() ).isEqualTo( 1 );
			}
		} ) );
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	// Named tests keep every matrix case visible in both enhancement-engine variants.
	@Test
	void fieldRead(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( Item.class, LockModeType.PESSIMISTIC_READ, scope );
	}

	@Test
	void fieldWrite(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( Item.class, LockModeType.PESSIMISTIC_WRITE, scope );
	}

	@Test
	void fieldForceIncrement(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( Item.class, LockModeType.PESSIMISTIC_FORCE_INCREMENT, scope );
	}

	@Test
	void propertyRead(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( PropertyItem.class, LockModeType.PESSIMISTIC_READ, scope );
	}

	@Test
	void propertyWrite(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( PropertyItem.class, LockModeType.PESSIMISTIC_WRITE, scope );
	}

	@Test
	void propertyForceIncrement(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( PropertyItem.class, LockModeType.PESSIMISTIC_FORCE_INCREMENT, scope );
	}

	@Test
	void timestampRead(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( TimestampItem.class, LockModeType.PESSIMISTIC_READ, scope );
	}

	@Test
	void timestampWrite(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( TimestampItem.class, LockModeType.PESSIMISTIC_WRITE, scope );
	}

	@Test
	void timestampForceIncrement(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( TimestampItem.class, LockModeType.PESSIMISTIC_FORCE_INCREMENT, scope );
	}

	@Test
	void secondaryTableRead(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( MultiItem.class, LockModeType.PESSIMISTIC_READ, scope );
	}

	@Test
	void secondaryTableWrite(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( MultiItem.class, LockModeType.PESSIMISTIC_WRITE, scope );
	}

	@Test
	void secondaryTableForceIncrement(SessionFactoryScope scope) {
		acquiredVersionIsTheFlushAndIncrementBaseline( MultiItem.class, LockModeType.PESSIMISTIC_FORCE_INCREMENT, scope );
	}

	@Test
	void readOnlyRead(SessionFactoryScope scope) {
		readOnlyEntriesKeepTheirAbsentSnapshot( LockModeType.PESSIMISTIC_READ, scope );
	}

	@Test
	void readOnlyWrite(SessionFactoryScope scope) {
		readOnlyEntriesKeepTheirAbsentSnapshot( LockModeType.PESSIMISTIC_WRITE, scope );
	}

	@Test
	void listConsumption(SessionFactoryScope scope) {
		consumptionPathsExposeCoherentVersions( "list", scope );
	}

	@Test
	void singleConsumption(SessionFactoryScope scope) {
		consumptionPathsExposeCoherentVersions( "single", scope );
	}

	@Test
	void scrollConsumption(SessionFactoryScope scope) {
		consumptionPathsExposeCoherentVersions( "scroll", scope );
	}

	@Test
	void streamConsumption(SessionFactoryScope scope) {
		consumptionPathsExposeCoherentVersions( "stream", scope );
	}

	@Test
	void statelessConsumption(SessionFactoryScope scope) {
		consumptionPathsExposeCoherentVersions( "stateless", scope );
	}

	@Test
	void failedSecondTableWrite(SessionFactoryScope scope) {
		failedSecondTableAcquisitionDoesNotFinalizeEntry( LockModeType.PESSIMISTIC_WRITE, scope );
	}

	@Test
	void failedSecondTableForceIncrement(SessionFactoryScope scope) {
		failedSecondTableAcquisitionDoesNotFinalizeEntry( LockModeType.PESSIMISTIC_FORCE_INCREMENT, scope );
	}

	@Test
	void managedStateIsPreserved(SessionFactoryScope scope) {
		managedStateIsPreservedAndStaleStateIsRejected( false, scope );
	}

	@Test
	void staleManagedStateIsRejected(SessionFactoryScope scope) {
		managedStateIsPreservedAndStaleStateIsRejected( true, scope );
	}

	void acquiredVersionIsTheFlushAndIncrementBaseline(Class<? extends Row> type, LockModeType mode, SessionFactoryScope scope) {
		final var committedVersion = new AtomicReference<Object>();
		withAcquisition( scope, type, mode, false, "single", false, acquired -> {
			acquired.assertCoherent();
			final var actual = acquired.item().version();
			if ( mode == LockModeType.PESSIMISTIC_FORCE_INCREMENT ) {
				if ( actual instanceof Number ) {
					assertThat( actual ).isEqualTo( ((Number) acquired.acquiredVersion()).intValue() + 1 );
				}
				else {
					assertThat( (Timestamp) actual ).isAfter( ACQUIRED_TIME );
				}
			}
			else {
				assertThat( actual ).isEqualTo( acquired.acquiredVersion() );
			}
			assertThat( acquired.entry().getLockMode() ).isEqualTo( LockMode.fromJpaLockMode( mode ) );
			final int[] tables = { 0 };
			acquired.entry().getPersister().forEachTableDetails( table -> tables[0]++ );
			assertThat( acquired.sql() ).hasSize( 1 + tables[0] + (mode == LockModeType.PESSIMISTIC_FORCE_INCREMENT ? 1 : 0) );
			acquired.flushClean();
			final int beforeUpdate = acquired.sql().size();
			acquired.item().setTitle( "local change" );
			((SessionImplementor) acquired.session()).flush();
			acquired.assertCoherent();
			if ( actual instanceof Number ) {
				assertThat( acquired.item().version() ).isEqualTo( ((Number) actual).intValue() + 1 );
			}
			committedVersion.set( acquired.item().version() );
			assertThat( acquired.sql().size() ).isGreaterThan( beforeUpdate );
		} );
		// Item is cacheable: a separate session must observe the committed state and version.
		scope.inTransaction( session -> {
			final var stored = session.find( type, 1 );
			assertThat( stored.getTitle() ).isEqualTo( "local change" );
			final var persister = session.getFactory().getMappingMetamodel().getEntityDescriptor( type );
			assertThat( persister.getVersionType().isEqual( stored.version(), committedVersion.get() ) ).isTrue();
		} );
	}

	@Test
	void strongerManagedModeIsRetained(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( reader -> {
			reader.setHibernateFlushMode( FlushMode.COMMIT );
			final var managed = reader.find( Item.class, 1, LockMode.PESSIMISTIC_WRITE );
			final var entry = reader.getPersistenceContextInternal().getEntry( managed );
			managed.setTitle( "local change" );
			assertThat( reader.createQuery( "from AdoptionItem where id = 1", Item.class )
					.setLockMode( LockModeType.PESSIMISTIC_READ ).setFollowOnStrategy( Locking.FollowOn.FORCE ).getSingleResult() )
					.isSameAs( managed );
			assertThat( managed.getTitle() ).isEqualTo( "local change" );
			assertThat( entry.getLockMode() ).isEqualTo( LockMode.PESSIMISTIC_WRITE );
			assertThat( entry.getVersion() ).isEqualTo( 0 );
			assertThat( entry.getLoadedValue( "title" ) ).isEqualTo( "initial" );
		} );
	}

	void readOnlyEntriesKeepTheirAbsentSnapshot(LockModeType mode, SessionFactoryScope scope) {
		withAcquisition( scope, Item.class, mode, true, "single", false, acquired -> {
			acquired.assertCoherent();
			assertThat( acquired.item().version() ).isEqualTo( 1 );
			assertThat( acquired.entry().isReadOnly() ).isTrue();
			assertThat( acquired.entry().getLoadedState() ).isNull();
			acquired.item().setTitle( "ignored change" );
			acquired.flushClean();
		} );
		scope.inTransaction( session -> assertThat( session.find( Item.class, 1 ).title ).isEqualTo( "initial changed" ) );
	}

	@Test
	void readOnlyEntryCanBecomeWritable(SessionFactoryScope scope) {
		withAcquisition( scope, Item.class, PESSIMISTIC_WRITE, true, "single", false, acquired -> {
			((SessionImplementor) acquired.session()).setReadOnly( acquired.item(), false );
			acquired.item().setTitle( "local change" );
			((SessionImplementor) acquired.session()).flush();
			acquired.assertCoherent();
			assertThat( acquired.item().version() ).isEqualTo( 2 );
		} );
	}

	void consumptionPathsExposeCoherentVersions(String consumer, SessionFactoryScope scope) {
		withAcquisition( scope, Item.class, PESSIMISTIC_WRITE, false, consumer, false, acquired -> {
			acquired.assertCoherent();
			assertThat( acquired.item().version() ).isEqualTo( 1 );
			assertThat( acquired.sql() ).hasSize( 2 );
		} );
	}

	@Test
	void batchesAndDuplicateRowsIncrementEachEntityOnce(SessionFactoryScope scope) {
		withAcquisition( scope, Item.class, LockModeType.PESSIMISTIC_FORCE_INCREMENT, false, "duplicates", false, acquired -> {
			assertThat( acquired.rows() ).hasSize( 2 );
			for ( var row : acquired.rows() ) {
				assertThat( row.version() ).isEqualTo( 2 );
				assertCoherent( acquired.entries().get( row.getId() ), row );
			}
			assertThat( acquired.sql() ).filteredOn( sql -> sql.startsWith( "update " ) ).hasSize( 2 );
			acquired.flushClean();
		} );
	}

	@Test
	void cachedPlanUsesEachExecutionsVersion(SessionFactoryScope scope) {
		for ( int version = 1; version <= 2; version++ ) {
			final int expected = version;
			withAcquisition( scope, Item.class, PESSIMISTIC_WRITE, false, "single", false, acquired -> {
				acquired.assertCoherent();
				assertThat( acquired.item().version() ).isEqualTo( expected );
			} );
		}
	}

	void failedSecondTableAcquisitionDoesNotFinalizeEntry(LockModeType mode, SessionFactoryScope scope) {
		withAcquisition( scope, MultiItem.class, mode, false, "single", true, acquired -> {
			assertThat( acquired.entry().getVersion() ).isEqualTo( 0 );
			assertThat( acquired.entry().getLockMode() ).isEqualTo( LockMode.READ );
			assertThat( acquired.sql() ).noneMatch( sql -> sql.startsWith( "update " ) );
			assertThat( acquired.session().getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isFalse();
		} );
	}

	void managedStateIsPreservedAndStaleStateIsRejected(boolean stale, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireDeletionRace( scope );
		try ( var reader = scope.getSessionFactory().openSession() ) {
			final var tx = reader.beginTransaction();
			try {
				reader.setHibernateFlushMode( FlushMode.COMMIT );
				final var managed = reader.find( Item.class, 1 );
				final var entry = reader.getPersistenceContextInternal().getEntry( managed );
				final var snapshot = entry.getLoadedState();
				managed.setTitle( "local change" );
				if ( stale ) {
					advanceWriter( scope, Item.class, false );
				}
				final var query = reader.createQuery( "from AdoptionItem order by id", Item.class )
						.setLockMode( PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
				if ( stale ) {
					assertThatThrownBy( query::getResultList ).isInstanceOf( OptimisticLockException.class )
							.hasCauseInstanceOf( StaleObjectStateException.class );
				}
				else {
					final var rows = query.getResultList();
					assertThat( rows ).hasSize( 2 );
					assertThat( rows.get( 0 ) ).isSameAs( managed );
					assertCoherent( reader.getPersistenceContextInternal().getEntry( rows.get( 1 ) ), rows.get( 1 ) );
				}
				assertThat( managed.title ).isEqualTo( "local change" );
				assertThat( entry.getVersion() ).isEqualTo( 0 );
				assertThat( entry.getLoadedState() ).isSameAs( snapshot );
				assertThat( entry.getLoadedValue( "title" ) ).isEqualTo( "initial" );
				if ( !stale ) {
					reader.flush();
					assertCoherent( entry, managed );
					assertThat( managed.version ).isEqualTo( 1 );
					tx.commit();
				}
			}
			finally { if ( tx.isActive() ) { tx.rollback(); } }
		}
	}

	private static void withAcquisition(SessionFactoryScope scope, Class<? extends Row> type, LockModeType mode,
			boolean readOnly, String consumer, boolean fail, Consumer<Acquired> assertion) {
		FollowOnLockingTestSupport.requireDeletionRace( scope );
		final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( type );
		final var readerRef = new AtomicReference<SharedSessionContractImplementor>();
		final Map<Integer, EntityEntry> entries = new HashMap<>();
		final var acquiredVersion = new AtomicReference<Object>();
		final var acquisitionFailure = new AtomicReference<QueryTimeoutException>();
		final var sqls = new ArrayList<String>();
		final UnaryOperator<String> inspector = sql -> {
			final long count = sqls.stream().filter( s -> FollowOnLockingTestSupport.isEntitySelect( s, persister ) ).count();
			if ( FollowOnLockingTestSupport.isEntitySelect( sql, persister ) && count == 1 ) {
				final var reader = readerRef.get();
				for ( int id = 1; id <= (consumer.equals( "duplicates" ) ? 2 : 1); id++ ) {
					final var entity = reader.getPersistenceContextInternal().getEntity( reader.generateEntityKey( id, persister ) );
					entries.put( id, reader.getPersistenceContextInternal().getEntry( entity ) );
				}
				acquiredVersion.set( advanceWriter( scope, type, consumer.equals( "duplicates" ) ) );
			}
			if ( fail && FollowOnLockingTestSupport.isEntitySelect( sql, persister ) && count == 2 ) {
				final var timeout = new QueryTimeoutException( "injected second-table acquisition timeout", new SQLTimeoutException(), sql );
				acquisitionFailure.set( timeout );
				throw timeout;
			}
			sqls.add( sql );
			return sql;
		};
		try ( var reader = consumer.equals( "stateless" )
				? scope.getSessionFactory().withStatelessOptions().statementInspector( inspector ).openStatelessSession()
				: scope.getSessionFactory().withOptions().statementInspector( inspector ).openSession() ) {
			final var session = (SharedSessionContractImplementor) reader;
			readerRef.set( session );
			final var tx = reader.beginTransaction();
			try {
				final var name = persister.getEntityName();
				final var hql = consumer.equals( "duplicates" ) ? "select i from " + name + " i, " + name + " j order by i.id"
						: "from " + name + " where id = 1";
				final var query = reader.createQuery( hql, type ).setLockMode( mode ).setReadOnly( readOnly )
						.setFollowOnStrategy( Locking.FollowOn.FORCE );
				FollowOnLockingTestSupport.assertFollowOnPlan( session, query );
				final List<? extends Row> rows;
				if ( fail ) {
					assertThatThrownBy( query::getResultList ).satisfies( failure -> {
						final var timeout = acquisitionFailure.get();
						assertThat( timeout ).isNotNull();
						final var expectedType = session.getJdbcServices().getDialect().causesRollback( timeout.getSQLException() )
								? jakarta.persistence.PersistenceException.class : jakarta.persistence.QueryTimeoutException.class;
						assertThat( failure ).isExactlyInstanceOf( expectedType ).hasCauseReference( timeout );
					} );
					final var item = (Row) session.getPersistenceContextInternal().getEntity( session.generateEntityKey( 1, persister ) );
					assertion.accept( new Acquired( session, List.of( item ), entries, sqls, acquiredVersion.get() ) );
					return;
				}
				switch ( consumer ) {
					case "list", "duplicates" -> rows = query.getResultList();
					case "scroll" -> {
						try ( var scroll = query.scroll( ScrollMode.FORWARD_ONLY ) ) {
							assertThat( scroll.next() ).isTrue();
							rows = List.of( scroll.get() );
							assertCoherent( entries.get( 1 ), rows.get( 0 ) );
						}
					}
					case "stream" -> {
						try ( var stream = query.getResultStream() ) {
							rows = List.of( stream.findFirst().orElseThrow() );
							assertCoherent( entries.get( 1 ), rows.get( 0 ) );
						}
					}
					default -> rows = List.of( query.getSingleResult() );
				}
				assertion.accept( new Acquired( session, rows, entries, sqls, acquiredVersion.get() ) );
				tx.commit();
			}
			finally { if ( tx.isActive() ) { tx.rollback(); } }
		}
	}

	private static Object advanceWriter(SessionFactoryScope scope, Class<? extends Row> type, boolean batch) {
		return scope.fromTransaction( writer -> {
			FollowOnLockingTestSupport.assertReadCommitted( writer );
			if ( type == TimestampItem.class ) {
				writer.doWork( connection -> {
					try ( var statement = connection.prepareStatement( "update adopt_timestamp set title = ?, version_value = ? where id = 1" ) ) {
						statement.setString( 1, "initial changed" );
						statement.setTimestamp( 2, ACQUIRED_TIME );
						assertThat( statement.executeUpdate() ).isEqualTo( 1 );
					}
				} );
				return ACQUIRED_TIME;
			}
			for ( int id = 1; id <= (batch ? 2 : 1); id++ ) {
				final var row = writer.find( type, id );
				row.setTitle( row.getTitle() + " changed" );
			}
			writer.flush();
			return writer.find( type, 1 ).version();
		} );
	}

	private static void assertCoherent(EntityEntry entry, Row row) {
		assertThat( entry ).isNotNull();
		final var versionType = entry.getPersister().getVersionType();
		assertThat( versionType.isEqual( entry.getVersion(), row.version() ) ).isTrue();
		if ( entry.getLoadedState() != null ) {
			assertThat( versionType.isEqual( entry.getLoadedState()[entry.getPersister().getVersionPropertyIndex()], row.version() ) ).isTrue();
		}
	}

	private record Acquired(SharedSessionContractImplementor session, List<? extends Row> rows,
			Map<Integer, EntityEntry> entries, List<String> sql, Object acquiredVersion) {
		Row item() { return rows.get( 0 ); }
		EntityEntry entry() { return entries.get( item().getId() ); }
		void assertCoherent() { FollowOnLockingVersionAdoptionTest.assertCoherent( entry(), item() ); }
		void flushClean() {
			final int beforeFlush = sql.size();
			((SessionImplementor) session).flush();
			assertThat( sql ).hasSize( beforeFlush );
		}
	}

	interface Row {
		int getId();
		String getTitle();
		void setTitle(String value);
		Object version();
	}

	@Entity(name = "AdoptionItem") @Table(name = "adopt_item")
	@Cacheable @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Item implements Row {
		@Id int id;
		@Version int version;
		String title = "initial";
		Item() {}
		Item(int id) { this.id = id; }
		public int getId() { return id; }
		public String getTitle() { return title; }
		public void setTitle(String value) { title = value; }
		public Object version() { return version; }
	}

	@Entity(name = "AdoptionPropertyItem") @Table(name = "adopt_property")
	static class PropertyItem implements Row {
		private int id = 1;
		private int version;
		private String title = "initial";
		@Id public int getId() { return id; }
		public void setId(int value) { id = value; }
		@Version public int getVersion() { return version; }
		public void setVersion(int value) { version = value; }
		public String getTitle() { return title; }
		public void setTitle(String value) { title = value; }
		public Object version() { return version; }
	}

	@Entity(name = "AdoptionTimestampItem") @Table(name = "adopt_timestamp")
	static class TimestampItem implements Row {
		@Id int id = 1;
		@Version @Column(name = "version_value") Timestamp version;
		String title = "initial";
		public int getId() { return id; }
		public String getTitle() { return title; }
		public void setTitle(String value) { title = value; }
		public Object version() { return version; }
	}

	@Entity(name = "AdoptionMultiItem") @Table(name = "adopt_multi")
	@SecondaryTable(name = "adopt_multi_detail", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id"))
	@SecondaryRow(table = "adopt_multi_detail", optional = false)
	static class MultiItem implements Row {
		@Id int id = 1;
		@Version int version;
		@Column(table = "adopt_multi_detail") String title = "initial";
		public int getId() { return id; }
		public String getTitle() { return title; }
		public void setTitle(String value) { title = value; }
		public Object version() { return version; }
	}
}
