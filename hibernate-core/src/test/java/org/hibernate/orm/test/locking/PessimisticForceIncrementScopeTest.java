package org.hibernate.orm.test.locking;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;
import jakarta.persistence.Version;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Timeouts;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.lock.internal.LockingSqlRewriterSupport;
import org.hibernate.dialect.lock.internal.PessimisticEntityLockException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static jakarta.persistence.PessimisticLockScope.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Verifies that managed force increment acquires the full requested scope before
/// changing versions. Physical probes use independent READ_COMMITTED transactions.
///
/// @author Steve Ebersole
@DomainModel(overrideCacheStrategy = false, annotatedClasses = {
		PessimisticForceIncrementScopeTest.Item.class, PessimisticForceIncrementScopeTest.CompositeItem.class
})
@SessionFactory(useCollectingStatementObserver = true, generateStatistics = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true")
})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "READPAST does not reliably skip all locked rows for these probes")
@Tag("db-locking")
@Jira("https://hibernate.atlassian.net/browse/HHH-21031")
class PessimisticForceIncrementScopeTest {
	@BeforeEach
	void createData(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			session.persist( new Item( 1 ) );
			session.persist( new CompositeItem() );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	void normalForceIncrementProtectsRootButNotCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			scope.getCollectingStatementObserver().clear();
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, NORMAL );
			assertVersion( session, item, 1 );
			assertThat( sqls( scope ) ).noneMatch( sql -> sql.startsWith( "select " ) );
			assertThat( versionUpdates( scope ) ).isEqualTo( 1 );
			assertProtection( scope, "force_increment_scope_item", "id", "1", true );
			assertCollectionsProtected( scope, false );
		} );
	}

	@Test
	void extendedWriteLockProtectsCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			session.lock( item, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( item.version ).isZero();
			assertCollectionsProtected( scope, true );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = LockMode.class, names = { "READ", "PESSIMISTIC_READ", "PESSIMISTIC_WRITE" })
	void extendedForceIncrementProtectsCollection(LockMode initialMode, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, initialMode, NORMAL );
			final var entry = entry( session, item );
			final var snapshot = entry.getLoadedState().clone();
			item.title = "local change";
			scope.getCollectingStatementObserver().clear();
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
			assertVersion( session, item, 1 );
			assertSnapshot( entry, snapshot, 1 );
			assertThat( item.title ).isEqualTo( "local change" );
			assertThat( Hibernate.isInitialized( item.tags ) ).isFalse();
			assertThat( Hibernate.isInitialized( item.labels ) ).isFalse();
			assertAcquisitionBeforeIncrement( scope );
			assertProtection( scope, "force_increment_scope_item", "id", "1", true );
			assertCollectionsProtected( scope, true );
		} );
	}

	enum Path { LOCK, FIND, NATURAL_ID, FOLLOW_ON }

	@ParameterizedTest
	@EnumSource(Path.class)
	@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
	void managedEntryPointsIncrementOnce(Path path, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			item.title = "local change";
			session.setHibernateFlushMode( FlushMode.MANUAL );
			scope.getCollectingStatementObserver().clear();
			switch ( path ) {
				case LOCK -> session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
				case FIND -> assertThat( session.find( Item.class, 1, PESSIMISTIC_FORCE_INCREMENT, EXTENDED ) ).isSameAs( item );
				case NATURAL_ID -> assertThat( session.byMultipleNaturalId( Item.class )
						.with( PESSIMISTIC_FORCE_INCREMENT, EXTENDED ).multiLoad( "item1" ) ).containsExactly( item );
				case FOLLOW_ON -> assertThat( followOn( session ).getSingleResult() ).isSameAs( item );
			}
			assertVersion( session, item, 1 );
			assertThat( versionUpdates( scope ) ).isEqualTo( 1 );
			assertThat( item.title ).isEqualTo( "local change" );
			assertCollectionsProtected( scope, true );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "false, false", "false, true", "true, false", "true, true" })
	void replacedOrClearedCollectionsKeepTheirState(boolean propertyRef, boolean clear, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			final var original = propertyRef ? item.labels : item.tags;
			final var snapshot = entry( session, item ).getLoadedState().clone();
			final var replacement = clear ? null : new HashSet<>( Set.of( "replacement" ) );
			if ( propertyRef ) {
				item.labels = replacement;
			}
			else {
				item.tags = replacement;
			}
			scope.getCollectingStatementObserver().clear();
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
			assertThat( (Object) (propertyRef ? item.labels : item.tags) ).isSameAs( replacement );
			assertThat( Hibernate.isInitialized( original ) ).isFalse();
			assertSnapshot( entry( session, item ), snapshot, 1 );
			assertVersion( session, item, 1 );
			assertAcquisitionBeforeIncrement( scope );
			assertCollectionsProtected( scope, true );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = PessimisticLockScope.class, names = { "NORMAL", "EXTENDED" })
	void readOnlyIncrementRetainsAbsentSnapshotAndInvalidatesCache(PessimisticLockScope scopeOption, SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getCache().containsEntity( Item.class, 1 ) ).isTrue();
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			session.setReadOnly( item, true );
			// Keep a changed immutable property-ref out of commit-time collection processing.
			session.setHibernateFlushMode( FlushMode.MANUAL );
			item.title = "ignored local title";
			item.code = "ignored local code";
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, scopeOption );
			assertVersion( session, item, 1 );
			assertThat( entry( session, item ).isReadOnly() ).isTrue();
			assertThat( entry( session, item ).getLoadedState() ).isNull();
			assertThat( item.title ).isEqualTo( "ignored local title" );
			assertThat( item.code ).isEqualTo( "ignored local code" );
			assertCollectionsProtected( scope, scopeOption == EXTENDED );
		} );
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			assertThat( item.version ).isEqualTo( 1 );
			assertThat( item.title ).isEqualTo( "initial" );
			assertThat( item.code ).isEqualTo( "item1" );
		} );
		assertThat( statistics.getSecondLevelCacheHitCount() ).isZero();
		assertThat( statistics.getSecondLevelCacheMissCount() ).isEqualTo( 1 );
	}

	@Test
	void repeatedExtendedRequestDoesNotIncrementAgain(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			scope.getCollectingStatementObserver().clear();
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
			assertVersion( session, item, 1 );
			assertThat( versionUpdates( scope ) ).isEqualTo( 1 );
			assertCollectionsProtected( scope, true );
		} );
	}

	@Test
	void incrementEstablishesSubsequentFlushBaseline(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			item.title = "local change";
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
			assertVersion( session, item, 1 );
			session.flush();
			assertThat( item.version ).isEqualTo( 2 );
			assertThat( entry( session, item ).getVersion() ).isEqualTo( 2 );
		} );
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			assertThat( item.title ).isEqualTo( "local change" );
			assertThat( item.version ).isEqualTo( 2 );
		} );
	}

	@Test
	void newlyLoadedDirectRequestIncrementsOnce(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			scope.getCollectingStatementObserver().clear();
			final var item = session.find( Item.class, 1, PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
			assertVersion( session, item, 1 );
			assertThat( versionUpdates( scope ) ).isEqualTo( 1 );
			assertCollectionsProtected( scope, true );
		} );
	}

	@Test
	@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
	void newlyLoadedFollowOnRequestIncrementsOnce(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			scope.getCollectingStatementObserver().clear();
			final var item = followOn( session ).getSingleResult();
			assertVersion( session, item, 1 );
			assertThat( versionUpdates( scope ) ).isEqualTo( 1 );
			assertCollectionsProtected( scope, true );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = PessimisticLockScope.class, names = { "NORMAL", "EXTENDED" })
	void newlyLoadedReadOnlyDirectRequest(PessimisticLockScope scopeOption, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( true );
			final var item = session.find( Item.class, 1, PESSIMISTIC_FORCE_INCREMENT, scopeOption );
			assertVersion( session, item, 1 );
			assertThat( entry( session, item ).getLoadedState() ).isNull();
			assertCollectionsProtected( scope, scopeOption == EXTENDED );
		} );
	}

	@Test
	@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
	void newlyLoadedReadOnlyFollowOnRequest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( true );
			scope.getCollectingStatementObserver().clear();
			final var item = followOn( session ).getSingleResult();
			assertVersion( session, item, 1 );
			assertThat( entry( session, item ).getLoadedState() ).isNull();
			assertThat( versionUpdates( scope ) ).isEqualTo( 1 );
			assertCollectionsProtected( scope, true );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	void compositeOwnerKeysDoNotRequireASnapshot(boolean readOnly, SessionFactoryScope scope) {
		assumeTrue( scope.getSessionFactory().getJdbcServices().getDialect().getRowValueSupport()
				.supports( RowValueSupport.Feature.IN_LIST ) );
		scope.inTransaction( session -> {
			session.setDefaultReadOnly( readOnly );
			final var item = session.find( CompositeItem.class, new CompositeId( 1, 2 ), PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
			assertThat( item.version ).isEqualTo( 1 );
			assertThat( entry( session, item ).getVersion() ).isEqualTo( 1 );
			assertThat( entry( session, item ).isReadOnly() ).isEqualTo( readOnly );
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_FORCE_INCREMENT );
			if ( readOnly ) {
				assertThat( entry( session, item ).getLoadedState() ).isNull();
			}
			assertThat( Hibernate.isInitialized( item.tags ) ).isFalse();
			assertProtection( scope, "force_increment_composite_tags", "owner_first", "1", true );
		} );
	}

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWait.class)
	void acquisitionTimeoutPrecedesIncrement(boolean collection, SessionFactoryScope scope) {
		try ( var session = scope.getSessionFactory().openSession(); var blocker = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			final var blocking = blocker.beginTransaction();
			try {
				final var item = session.find( Item.class, 1 );
				final var initialMode = session.getCurrentLockMode( item );
				final var snapshot = entry( session, item ).getLoadedState().clone();
				blocker.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						assertThat( statement.executeUpdate( collection
								? "update force_increment_scope_tags set tag_value = 'blocked' where item_id = 1"
								: "update force_increment_scope_item set title = 'blocked' where id = 1" ) ).isEqualTo( 1 );
					}
				} );
				scope.getCollectingStatementObserver().clear();
				assertThatThrownBy( () -> session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED, Timeout.seconds( 1 ) ) )
						.isExactlyInstanceOf( PessimisticEntityLockException.class ).hasCauseInstanceOf( LockTimeoutException.class );
				assertThat( item.version ).isZero();
				assertSnapshot( entry( session, item ), snapshot, 0 );
				assertThat( session.getCurrentLockMode( item ) ).isEqualTo( initialMode );
				assertThat( versionUpdates( scope ) ).isZero();
			}
			finally {
				transaction.rollback();
				blocking.rollback();
			}
		}
	}

	@ParameterizedTest
	@CsvSource({ "false", "true" })
	void missingOrStaleRootPreventsIncrement(boolean delete, SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireDeletionRace( scope );
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final var item = session.find( Item.class, 1 );
				final var initialMode = session.getCurrentLockMode( item );
				scope.inTransaction( writer -> {
					final var other = writer.find( Item.class, 1 );
					if ( delete ) {
						writer.remove( other );
					}
					else {
						other.title = "changed";
					}
				} );
				scope.getCollectingStatementObserver().clear();
				assertThatThrownBy( () -> session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED ) )
						.isInstanceOf( StaleObjectStateException.class );
				assertThat( item.version ).isZero();
				assertThat( entry( session, item ).getVersion() ).isEqualTo( 0 );
				assertThat( session.getCurrentLockMode( item ) ).isEqualTo( initialMode );
				assertThat( versionUpdates( scope ) ).isZero();
			}
			finally {
				transaction.rollback();
			}
		}
	}

	@Test
	void incrementFailureRetainsVersionsAndAllowsFreshAcquisitionAfterRollback(SessionFactoryScope scope) {
		final var failure = new AtomicReference<HibernateException>();
		try ( var session = scope.getSessionFactory().withOptions().statementInspector( (StatementInspector) sql -> {
			if ( isVersionUpdate( sql ) ) {
				final var injected = new HibernateException( "injected increment failure" );
				failure.set( injected );
				throw injected;
			}
			return sql;
		} ).openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final var item = session.find( Item.class, 1 );
				assertThatThrownBy( () -> session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED ) )
						.satisfies( thrown -> assertThat( thrown ).isSameAs( failure.get() ) );
				assertThat( failure.get() ).isNotNull();
				assertThat( item.version ).isZero();
				assertThat( entry( session, item ).getVersion() ).isEqualTo( 0 );
			}
			finally {
				transaction.rollback();
			}
		}
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, EXTENDED );
			assertVersion( session, item, 1 );
		} );
	}

	private static org.hibernate.query.SelectionQuery<Item> followOn(Session session) {
		final var query = session.createQuery( "from ForceIncrementScopeItem where id = 1", Item.class )
				.setHibernateLockMode( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
				.setFollowOnStrategy( Locking.FollowOn.FORCE );
		FollowOnLockingTestSupport.assertFollowOnPlan( (SharedSessionContractImplementor) session, query );
		return query;
	}

	private static EntityEntry entry(Session session, Object item) {
		return ((SharedSessionContractImplementor) session).getPersistenceContextInternal().getEntry( item );
	}

	private static void assertVersion(Session session, Item item, int expected) {
		assertThat( item.version ).isEqualTo( expected );
		final var entry = entry( session, item );
		assertThat( entry.getVersion() ).isEqualTo( expected );
		assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_FORCE_INCREMENT );
		if ( entry.getLoadedState() != null ) {
			assertThat( entry.getLoadedState()[entry.getPersister().getVersionPropertyIndex()] ).isEqualTo( expected );
		}
	}

	private static void assertSnapshot(EntityEntry entry, Object[] before, int version) {
		final var state = entry.getLoadedState();
		for ( int i = 0; i < before.length; i++ ) {
			if ( i == entry.getPersister().getVersionPropertyIndex() ) {
				assertThat( state[i] ).isEqualTo( version );
			}
			else if ( before[i] instanceof PersistentCollection<?> ) {
				assertThat( state[i] ).isSameAs( before[i] );
			}
			else {
				assertThat( state[i] ).isEqualTo( before[i] );
			}
		}
	}

	private static List<String> sqls(SessionFactoryScope scope) {
		return scope.getCollectingStatementObserver().getSqlQueries();
	}

	private static boolean isVersionUpdate(String sql) {
		return sql.startsWith( "update force_increment_scope_item " );
	}

	private static long versionUpdates(SessionFactoryScope scope) {
		return sqls( scope ).stream().filter( PessimisticForceIncrementScopeTest::isVersionUpdate ).count();
	}

	private static void assertAcquisitionBeforeIncrement(SessionFactoryScope scope) {
		final var sql = sqls( scope );
		assertThat( versionUpdates( scope ) ).isEqualTo( 1 );
		final int increment = java.util.stream.IntStream.range( 0, sql.size() ).filter( i -> isVersionUpdate( sql.get( i ) ) ).findFirst().orElseThrow();
		for ( var table : List.of( "force_increment_scope_tags", "force_increment_scope_labels" ) ) {
			final int acquisition = java.util.stream.IntStream.range( 0, sql.size() )
					.filter( i -> sql.get( i ).startsWith( "select " ) && sql.get( i ).contains( table ) ).findFirst().orElseThrow();
			assertThat( acquisition ).isLessThan( increment );
		}
	}

	private static void assertCollectionsProtected(SessionFactoryScope scope, boolean locked) {
		assertProtection( scope, "force_increment_scope_tags", "item_id", "1", locked );
		assertProtection( scope, "force_increment_scope_labels", "item_code", "'item1'", locked );
	}

	private static void assertProtection(SessionFactoryScope scope, String table, String key, String value, boolean locked) {
		final int rows = scope.fromTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var sql = LockingSqlRewriterSupport.rewrite( session.getJdbcServices().getDialect().getLockingSupport(),
					"select 1 from " + table + " t where " + key + " = " + value,
					new LockOptions( PESSIMISTIC_WRITE ).setTimeout( Timeouts.SKIP_LOCKED ), Map.of( "t", new String[] { key } ) ).sql();
			return session.createNativeQuery( sql, Integer.class ).getResultList().size();
		} );
		assertThat( rows ).as( "%s physical protection", table ).isEqualTo( locked ? 0 : 1 );
	}

	@Entity(name = "ForceIncrementScopeItem")
	@Table(name = "force_increment_scope_item")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Item {
		@Id int id;
		@Version int version;
		@NaturalId String code;
		String title;
		@ElementCollection
		@CollectionTable(name = "force_increment_scope_tags", joinColumns = @JoinColumn(name = "item_id"))
		@Column(name = "tag_value")
		Set<String> tags = new HashSet<>();
		@ElementCollection
		@CollectionTable(name = "force_increment_scope_labels", joinColumns = @JoinColumn(name = "item_code", referencedColumnName = "code"))
		@Column(name = "label_value")
		Set<String> labels = new HashSet<>();

		Item() {}
		Item(int id) {
			this.id = id;
			code = "item" + id;
			title = "initial";
			tags.add( "initial tag" );
			labels.add( "initial label" );
		}
	}
	@Entity(name = "ForceIncrementCompositeItem")
	@Table(name = "force_increment_composite_item")
	static class CompositeItem {
		@EmbeddedId CompositeId id = new CompositeId( 1, 2 );
		@Version int version;
		@ElementCollection
		@CollectionTable(name = "force_increment_composite_tags", joinColumns = {
				@JoinColumn(name = "owner_first", referencedColumnName = "first_part"),
				@JoinColumn(name = "owner_second", referencedColumnName = "second_part")
		})
		@Column(name = "tag_value")
		Set<String> tags = new HashSet<>( Set.of( "initial tag" ) );
	}

	@Embeddable
	static class CompositeId implements java.io.Serializable {
		@Column(name = "first_part") int first;
		@Column(name = "second_part") int second;

		CompositeId() {}
		CompositeId(int first, int second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof CompositeId id && first == id.first && second == id.second;
		}

		@Override
		public int hashCode() {
			return Objects.hash( first, second );
		}
	}

}
