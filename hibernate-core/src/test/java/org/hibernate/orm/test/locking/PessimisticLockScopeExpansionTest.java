package org.hibernate.orm.test.locking;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;
import jakarta.persistence.Version;

import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Session;
import org.hibernate.Timeouts;
import org.hibernate.annotations.NaturalId;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.lock.internal.LockingSqlRewriterSupport;
import org.hibernate.dialect.lock.internal.PessimisticEntityLockException;
import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.LockTimeoutException;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static jakarta.persistence.PessimisticLockScope.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;
import static org.hibernate.LockMode.PESSIMISTIC_READ;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Verifies physical collection-row protection independently of the root's
/// recorded mode. Separate transactions use skip-locked selects as nonblocking
/// probes, at READ_COMMITTED, without changing collection data.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = PessimisticLockScopeExpansionTest.Item.class)
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "READPAST does not reliably skip all locked rows for these probes")
@Tag("db-locking")
@Jira("https://hibernate.atlassian.net/browse/HHH-21030")
class PessimisticLockScopeExpansionTest {
	@BeforeEach
	void createData(SessionFactoryScope scope) {
		FollowOnLockingTestSupport.requireAcquisition( scope );
		scope.inTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			session.persist( new Item( 1, true ) );
			session.persist( new Item( 2, false ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void normalScopeProtectsRootButNotCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			assertProtection( scope, false, true );
			assertProtection( scope, true, false );
		} );
	}

	@Test
	void initialExtendedLockProtectsCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			session.lock( item, PESSIMISTIC_WRITE, EXTENDED );
			assertProtection( scope, true, true );
		} );
	}

	@ParameterizedTest(name = "existing WRITE, requested {0} with EXTENDED scope")
	@EnumSource(value = LockMode.class, names = { "PESSIMISTIC_READ", "PESSIMISTIC_WRITE" })
	void widerScopeProtectsCollectionDespiteExistingMode(LockMode requestedMode, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			final var state = new ManagedState( session, item );
			session.lock( item, requestedMode, EXTENDED );
			state.assertPreserved( session, item, PESSIMISTIC_WRITE );
			assertProtection( scope, true, true );
		} );
	}

	@Test
	void equalReadModeStillAcquiresCollectionProtection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_READ, NORMAL );
			session.lock( item, PESSIMISTIC_READ, EXTENDED );
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_READ );
			assertProtection( scope, true, true );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = LockMode.class, names = { "PESSIMISTIC_READ", "PESSIMISTIC_WRITE" })
	void normalScopeRemainsANoOp(LockMode requestedMode, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			scope.getCollectingStatementObserver().clear();
			session.lock( item, requestedMode, NORMAL );
			assertThat( scope.getCollectingStatementObserver().getSqlQueries() ).isEmpty();
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_WRITE );
			assertProtection( scope, true, false );
		} );
	}

	@Test
	void repeatedExtendedRequestAcquiresAgain(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			session.lock( item, PESSIMISTIC_WRITE, EXTENDED );
			scope.getCollectingStatementObserver().clear();
			session.lock( item, PESSIMISTIC_WRITE, EXTENDED );
			assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
					.anyMatch( sql -> sql.startsWith( "select " ) && sql.contains( "scope_expansion_tags" ) );
			assertProtection( scope, true, true );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = LockMode.class, names = { "PESSIMISTIC_READ", "PESSIMISTIC_WRITE", "PESSIMISTIC_FORCE_INCREMENT" })
	void scopeExpansionAfterForceIncrementDoesNotIncrementAgain(LockMode requestedMode, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			session.lock( item, PESSIMISTIC_FORCE_INCREMENT, NORMAL );
			assertThat( item.version ).isEqualTo( 1 );
			final var state = new ManagedState( session, item );
			session.lock( item, requestedMode, EXTENDED );
			state.assertPreserved( session, item, PESSIMISTIC_FORCE_INCREMENT );
			assertProtection( scope, true, true );
		} );
	}

	@Test
	void managedFindAcquiresExtendedScope(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			final var state = new ManagedState( session, item );
			assertThat( session.find( Item.class, 1, PESSIMISTIC_READ, EXTENDED ) ).isSameAs( item );
			state.assertPreserved( session, item, PESSIMISTIC_WRITE );
			assertProtection( scope, true, true );
		} );
	}

	@Test
	void managedNaturalIdLookupAcquiresExtendedScope(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			final var state = new ManagedState( session, item );
			assertThat( session.byMultipleNaturalId( Item.class ).with( PESSIMISTIC_READ, EXTENDED ).multiLoad( "item1" ) )
					.containsExactly( item );
			state.assertPreserved( session, item, PESSIMISTIC_WRITE );
			assertProtection( scope, true, true );
		} );
	}

	@Test
	@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
	void followOnReloadOfManagedEntityAcquiresExtendedScope(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			final var state = new ManagedState( session, item );
			// Preserve dirty state while testing acquisition rather than query auto-flush.
			session.setHibernateFlushMode( FlushMode.MANUAL );
			final var query = session.createQuery( "from ScopeExpansionItem where id = 1", Item.class )
					.setHibernateLockMode( PESSIMISTIC_READ ).setLockScope( EXTENDED )
					.setFollowOnStrategy( Locking.FollowOn.FORCE );
			FollowOnLockingTestSupport.assertFollowOnPlan( (SharedSessionContractImplementor) session, query );
			assertThat( query.getSingleResult() ).isSameAs( item );
			state.assertPreserved( session, item, PESSIMISTIC_WRITE );
			assertProtection( scope, true, true );
		} );
	}

	@Test
	void initializedDirtyCollectionIsNotRefreshedOrFlushed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			Hibernate.initialize( item.tags );
			item.tags.add( "local tag" );
			scope.getCollectingStatementObserver().clear();
			session.lock( item, PESSIMISTIC_READ, EXTENDED );
			assertThat( item.tags ).containsExactlyInAnyOrder( "initial tag", "local tag" );
			assertThat( item.version ).isZero();
			assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
					.allMatch( sql -> sql.startsWith( "select " ) );
			assertProtection( scope, true, true );
		} );
	}

	@Test
	void replacementCollectionCanBeFlushedAfterScopeExpansion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			item.tags = new HashSet<>( Set.of( "replacement" ) );
			session.lock( item, PESSIMISTIC_READ, EXTENDED );
		} );
		scope.inTransaction( session -> assertThat( session.find( Item.class, 1 ).tags ).containsExactly( "replacement" ) );
	}

	@Test
	void nullCollectionCanBeFlushedAfterScopeExpansion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			item.tags = null;
			session.lock( item, PESSIMISTIC_READ, EXTENDED );
		} );
		scope.inTransaction( session -> assertThat( session.find( Item.class, 1 ).tags ).isEmpty() );
	}

	@ParameterizedTest(name = "property-ref={0}, clear={1}, initialized={2}, read-only={3}")
	@CsvSource({
			"false, false, false, false", "false, false, true, false",
			"false, true, false, false", "false, true, true, false",
			"true, false, false, false", "true, false, true, false",
			"true, true, false, false", "true, true, true, false",
			"false, false, false, true", "false, false, true, true",
			"false, true, false, true", "false, true, true, true",
			"true, false, false, true", "true, false, true, true",
			"true, true, false, true", "true, true, true, true"
	})
	void replacedOrClearedCollectionDoesNotPreventAcquisition(
			boolean propertyRef, boolean clear, boolean initialized, boolean readOnly, SessionFactoryScope scope) {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
				final var original = propertyRef ? item.labels : item.tags;
				if ( initialized ) {
					Hibernate.initialize( original );
				}
				session.setReadOnly( item, readOnly );
				final var entry = session.getPersistenceContextInternal().getEntry( item );
				final var snapshot = entry.getLoadedState() == null ? null : entry.getLoadedState().clone();
				final var replacement = clear ? null : new HashSet<>( Set.of( "replacement" ) );
				if ( propertyRef ) {
					item.labels = replacement;
				}
				else {
					item.tags = replacement;
				}
				item.title = "local change";
				if ( readOnly ) {
					// This change is not persisted, so collection rows still use item1.
					item.code = "local code";
				}
				scope.getCollectingStatementObserver().clear();
				session.lock( item, PESSIMISTIC_READ, EXTENDED );
				assertThat( (Object) (propertyRef ? item.labels : item.tags) ).isSameAs( replacement );
				if ( replacement != null ) {
					assertThat( replacement ).containsExactly( "replacement" );
				}
				assertThat( Hibernate.isInitialized( original ) ).isEqualTo( initialized );
				assertSnapshotUnchanged( entry, snapshot );
				assertThat( item.title ).isEqualTo( "local change" );
				assertThat( item.code ).isEqualTo( readOnly ? "local code" : "item1" );
				assertThat( item.version ).isZero();
				assertThat( entry.getVersion() ).isEqualTo( 0 );
				assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_WRITE );
				assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
						.isNotEmpty().allMatch( sql -> sql.startsWith( "select " ) );
				assertProtection( scope, true, true );
				assertThat( probeWriteProtection( scope, "scope_expansion_labels", "item_code", "'item1'" ) )
						.as( "property-ref collection rows remain protected" ).isZero();
			}
			finally {
				transaction.rollback();
			}
		}
	}

	@Test
	void emptyCollectionDoesNotIndicateAMissingEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 2, PESSIMISTIC_WRITE, NORMAL );
			session.lock( item, PESSIMISTIC_READ, EXTENDED );
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_WRITE );
			assertThat( Hibernate.isInitialized( item.tags ) ).isFalse();
		} );
	}

	@Test
	void requestedReadScopeDoesNotWeakenRootWriteLock(SessionFactoryScope scope) {
		final var concurrency = scope.getSessionFactory().getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
		assumeTrue( concurrency.supports( Operation.SHARED_LOCK_READ ), "Dialect uses an update-lock fallback for read locks" );
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
			session.lock( item, PESSIMISTIC_READ, EXTENDED );
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_WRITE );
			// A U-lock may coexist with shared readers (DB2), but not with another
			// U-lock. This still detects weakening the root to a shared lock.
			assertThat( probe( scope, false, PESSIMISTIC_WRITE ) ).as( "root WRITE conflicts with another update lock" ).isZero();
			assertThat( probe( scope, true, PESSIMISTIC_READ ) ).as( "collection READ permits another shared lock" ).isEqualTo( 1 );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWait.class)
	void collectionAcquisitionTimeoutRetainsRootModeAndManagedState(SessionFactoryScope scope) {
		try ( var session = scope.getSessionFactory().openSession(); var blocker = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			final var blockerTransaction = blocker.beginTransaction();
			try {
				FollowOnLockingTestSupport.assertReadCommitted( session );
				FollowOnLockingTestSupport.assertReadCommitted( blocker );
				final var item = session.find( Item.class, 1, PESSIMISTIC_WRITE, NORMAL );
				final var state = new ManagedState( session, item );
				blocker.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						assertThat( statement.executeUpdate( "update scope_expansion_tags set tag_value = 'blocked' where item_id = 1" ) )
								.isEqualTo( 1 );
					}
				} );
				assertThatThrownBy( () -> session.lock( item, PESSIMISTIC_READ, EXTENDED, Timeout.seconds( 1 ) ) )
						.satisfies( failure -> {
							// Native Session bootstrap preserves the strategy exception.
							assertThat( failure ).isExactlyInstanceOf( PessimisticEntityLockException.class )
									.hasCauseInstanceOf( LockTimeoutException.class );
							assertThat( ((LockTimeoutException) failure.getCause()).getSQL() ).contains( "scope_expansion_tags" );
						} );
				state.assertPreserved( session, item, PESSIMISTIC_WRITE );
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
				if ( blockerTransaction.isActive() ) {
					blockerTransaction.rollback();
				}
			}
		}
		// A fresh request can acquire all rows after failure cleanup and rollback.
		scope.inTransaction( session -> session.find( Item.class, 1, PESSIMISTIC_WRITE, EXTENDED ) );
	}

	private static void assertProtection(SessionFactoryScope scope, boolean collection, boolean protectedRows) {
		assertThat( probeWriteProtection( scope, collection ? "scope_expansion_tags" : "scope_expansion_item",
				collection ? "item_id" : "id", "1" ) )
				.as( "%s row protection", collection ? "collection" : "root" ).isEqualTo( protectedRows ? 0 : 1 );
	}

	private static int probeWriteProtection(SessionFactoryScope scope, String table, String key, String value) {
		return scope.fromTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var sql = FollowOnLockingTestSupport.writeProtectionProbeSql( session,
					"select 1 from " + table + " t where " + key + " = " + value,
					Map.of( "t", new String[] { key } ) );
			return session.createNativeQuery( sql, Integer.class ).getResultList().size();
		} );
	}

	private static int probe(SessionFactoryScope scope, boolean collection, LockMode mode) {
		return probe( scope, collection ? "scope_expansion_tags" : "scope_expansion_item",
				collection ? "item_id" : "id", "1", mode );
	}

	private static int probe(SessionFactoryScope scope, String table, String key, String value, LockMode mode) {
		return scope.fromTransaction( session -> {
			FollowOnLockingTestSupport.assertReadCommitted( session );
			final var sql = LockingSqlRewriterSupport.rewrite(
					session.getJdbcServices().getDialect().getLockingSupport(),
					"select 1 from " + table + " t where " + key + " = " + value,
					new LockOptions( mode ).setTimeout( Timeouts.SKIP_LOCKED ),
					Map.of( "t", new String[] { key } ) ).sql();
			return session.createNativeQuery( sql, Integer.class ).getResultList().size();
		} );
	}

	private static void assertSnapshotUnchanged(EntityEntry entry, Object[] snapshot) {
		final var actual = entry.getLoadedState();
		if ( snapshot == null ) {
			assertThat( actual ).isNull();
			return;
		}
		assertThat( actual ).hasSameSizeAs( snapshot );
		for ( int i = 0; i < snapshot.length; i++ ) {
			// PersistentSet.equals() initializes the collection, even for itself.
			if ( snapshot[i] instanceof PersistentCollection<?> ) {
				assertThat( actual[i] ).isSameAs( snapshot[i] );
			}
			else {
				assertThat( actual[i] ).isEqualTo( snapshot[i] );
			}
		}
	}

	private static class ManagedState {
		private final EntityEntry entry;
		private final Object[] snapshot;
		private final Object version;
		private final Set<String> tags;

		ManagedState(Session session, Item item) {
			entry = ((SharedSessionContractImplementor) session).getPersistenceContextInternal().getEntry( item );
			snapshot = entry.getLoadedState().clone();
			version = entry.getVersion();
			tags = item.tags;
			assertThat( Hibernate.isInitialized( tags ) ).isFalse();
			item.title = "local change";
		}

		void assertPreserved(Session session, Item item, LockMode mode) {
			assertThat( Hibernate.isInitialized( tags ) ).isFalse();
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( mode );
			assertThat( item.title ).isEqualTo( "local change" );
			assertSnapshotUnchanged( entry, snapshot );
			assertThat( entry.getVersion() ).isEqualTo( version );
			assertThat( item.version ).isEqualTo( version );
			assertThat( (Object) item.tags ).isSameAs( tags );
			assertThat( Hibernate.isInitialized( tags ) ).isFalse();
		}
	}

	@Entity(name = "ScopeExpansionItem")
	@Table(name = "scope_expansion_item")
	static class Item {
		@Id int id;
		@Version int version;
		@NaturalId String code;
		String title;
		@ElementCollection
		@CollectionTable(name = "scope_expansion_tags", joinColumns = @JoinColumn(name = "item_id"))
		@Column(name = "tag_value")
		Set<String> tags = new HashSet<>();
		@ElementCollection
		@CollectionTable(name = "scope_expansion_labels", joinColumns = @JoinColumn(name = "item_code", referencedColumnName = "code"))
		@Column(name = "label_value")
		Set<String> labels = new HashSet<>();

		Item() {
		}

		Item(int id, boolean withTag) {
			this.id = id;
			code = "item" + id;
			title = "initial";
			if ( withTag ) {
				tags.add( "initial tag" );
				labels.add( "initial label" );
			}
		}
	}
}
