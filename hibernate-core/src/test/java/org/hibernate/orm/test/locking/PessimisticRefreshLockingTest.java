package org.hibernate.orm.test.locking;

import java.util.function.Consumer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;
import jakarta.persistence.Version;

import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.LockingSqlRewriterSupport;
import org.hibernate.dialect.lock.internal.TableLockHintRendererSupport;
import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.dialect.lock.spi.FollowOnLockingPolicy;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.schema.spi.DelegatingTableCreationSupport;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.tool.schema.spi.StandardTableExporter;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.transaction.TransactionUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static jakarta.persistence.PessimisticLockScope.NORMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;
import static org.hibernate.LockMode.PESSIMISTIC_READ;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Exercises both SPI answers independently of production dialect policy. State,
/// version actions and scope assertions do not derive their expectations from
/// that policy; SQL assertions additionally verify that it is actually used.
///
/// @author Steve Ebersole
@DomainModel(overrideCacheStrategy = false, annotatedClasses = {
		PessimisticRefreshLockingTest.Item.class, PessimisticRefreshLockingTest.Parent.class,
		PessimisticRefreshLockingTest.Child.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ParameterizedClass(name = "plainReload={0}, isolation={1}")
@MethodSource("configurations")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
@JiraKey("HHH-21010")
@Tag("db-locking")
class PessimisticRefreshLockingTest implements ServiceRegistryProducer {
	private final Boolean plainReload;
	private final int isolation;
	private LockingSupport support;

	PessimisticRefreshLockingTest(Boolean plainReload, int isolation) {
		this.plainReload = plainReload;
		this.isolation = isolation;
	}

	static Stream<Arguments> configurations() {
		final var standard = Stream.of( Arguments.of( false, 2 ), Arguments.of( true, 2 ),
				Arguments.of( null, 2 ), Arguments.of( null, 4 ), Arguments.of( null, 8 ) );
		final var dialect = DialectContext.getDialect();
		return PessimisticRefreshSnapshotTest.isH2( dialect ) ? Stream.concat( standard, Stream.of( Arguments.of( null, 6 ) ) )
				: PessimisticRefreshSnapshotTest.isSQLServer( dialect ) ? Stream.concat( standard, Stream.of( Arguments.of( null, 4096 ) ) )
				: standard;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		PessimisticRefreshSnapshotTest.assumeIsolationSupported( isolation );
		final var original = DialectContext.getDialect();
		final Dialect dialect = mock( original.getClass(), delegatesTo( original ) );
		if ( isolation == java.sql.Connection.TRANSACTION_SERIALIZABLE && original instanceof OracleDialect ) {
			// Oracle cannot initialize deferred table segments in a SERIALIZABLE
			// transaction. Allocate this fixture's storage during schema creation.
			doReturn( new DelegatingTableCreationSupport( original.getTableCreationSupport() ) {
				@Override
				public String tableCreationOptions() {
					return super.tableCreationOptions() + " segment creation immediate";
				}
			} ).when( dialect ).getTableCreationSupport();
			// The delegate's exporter retains the original dialect instance.
			doReturn( new StandardTableExporter( dialect ) ).when( dialect ).getTableExporter();
		}
		support = mock( LockingSupport.class, delegatesTo( original.getLockingSupport() ) );
		doReturn( support ).when( dialect ).getLockingSupport();
		if ( plainReload != null ) {
			when( support.canUsePlainReadForRefresh( any(), any() ) ).thenReturn( plainReload );
		}
		return PessimisticRefreshSnapshotTest.configureIsolation( builder, isolation ).applySetting( DIALECT, dialect )
				.applySetting( "hibernate.use_sql_comments", false ).build();
	}

	@BeforeEach
	void setup(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getJdbcServices().getJdbcEnvironment()
				.getJdbcMetadata().getTransactionIsolation() ).isEqualTo( isolation );
		scope.inTransaction( session -> {
			assertThat( session.doReturningWork( java.sql.Connection::getTransactionIsolation ) ).isEqualTo( isolation );
		} );
		inFixtureTransaction( scope, session -> {
			session.persist( new Item() );
			final var child = new Child();
			session.persist( child );
			final var parent = new Parent();
			parent.child = child;
			session.persist( parent );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		if ( usesReadCommittedFixtures( scope ) ) {
			inFixtureTransaction( scope, session -> {
				session.createMutationQuery( "delete from RefreshScopeParent" ).executeUpdate();
				session.createMutationQuery( "delete from RefreshScopeChild" ).executeUpdate();
				session.createMutationQuery( "delete from RefreshLockItem" ).executeUpdate();
			} );
		}
		else {
			scope.dropData();
		}
	}

	private boolean usesReadCommittedFixtures(SessionFactoryScope scope) {
		return isolation == java.sql.Connection.TRANSACTION_SERIALIZABLE
				&& scope.getSessionFactory().getJdbcServices().getDialect() instanceof OracleDialect;
	}

	private void inFixtureTransaction(SessionFactoryScope scope, Consumer<SessionImplementor> action) {
		if ( !usesReadCommittedFixtures( scope ) ) {
			scope.inTransaction( action );
			return;
		}
		// Use a separate session and restore its connection before returning it to
		// the pool. Test transactions and bootstrap metadata remain SERIALIZABLE.
		scope.inSession( session -> session.doWork( connection -> {
			final int originalIsolation = connection.getTransactionIsolation();
			connection.setTransactionIsolation( java.sql.Connection.TRANSACTION_READ_COMMITTED );
			try {
				try ( var fixtureSession = scope.getSessionFactory().withOptions().connection( connection ).openSession() ) {
					TransactionUtil.inTransaction( (SessionImplementor) fixtureSession, action );
				}
			}
			finally {
				connection.setTransactionIsolation( originalIsolation );
			}
		} ) );
	}

	static Stream<Arguments> modes() {
		return Stream.of( PESSIMISTIC_READ, PESSIMISTIC_WRITE, PESSIMISTIC_FORCE_INCREMENT,
				LockMode.UPGRADE_NOWAIT, LockMode.UPGRADE_SKIPLOCKED ).flatMap( held ->
				Stream.of( LockMode.values() ).filter( requested -> requested != LockMode.WRITE && !requested.greaterThan( held ) )
						.map( requested -> Arguments.of( held, requested ) ) );
	}

	@ParameterizedTest(name = "held={0}, requested={1}")
	@MethodSource("modes")
	void preservesModeAndDoesNotReplayVersionActions(LockMode held, LockMode requested, SessionFactoryScope scope) {
		if ( held == LockMode.UPGRADE_SKIPLOCKED ) {
			assumeSkipLockedIsolationSupported( scope );
		}
		assumeTrue( held != LockMode.UPGRADE_SKIPLOCKED
				|| support.getFollowOnLockingPolicy() != FollowOnLockingPolicy.ALWAYS,
				"The initial skip-locked acquisition requires a direct locking route" );
		scope.inTransaction( session -> {
			final var item = session.get( Item.class, 1, held );
			final int version = item.version;
			item.name = "local change";
			session.setHibernateFlushMode( FlushMode.MANUAL );
			clearInvocations( support );
			scope.getCollectingStatementObserver().clear();
			session.refresh( item, requested, NORMAL, Timeouts.WAIT_FOREVER, Locking.FollowOn.ALLOW );
			assertThat( item.name ).isEqualTo( "database" );
			assertVersion( session, item, version );
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( requested.lessThan( held ) ? held : requested );
			verify( support ).canUsePlainReadForRefresh( held, requested );
			assertThat( sql( scope ) ).noneMatch( statement -> statement.startsWith( "update " ) );
			final boolean plain = plainReload != null ? plainReload
					: DialectContext.getDialect().getLockingSupport().canUsePlainReadForRefresh( held, requested );
			assertReload( scope, held == PESSIMISTIC_READ ? PESSIMISTIC_READ : PESSIMISTIC_WRITE, !plain );
		} );
		// A duplicated optimistic process would perform a version action at commit.
		scope.inTransaction( session -> assertThat( session.get( Item.class, 1 ).version )
				.isEqualTo( held == PESSIMISTIC_FORCE_INCREMENT ? 1 : 0 ) );
	}

	@ParameterizedTest
	@EnumSource(value = LockMode.class, names = { "PESSIMISTIC_READ", "PESSIMISTIC_WRITE", "PESSIMISTIC_FORCE_INCREMENT" })
	void preservesReadOnlyVersionState(LockMode mode, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.get( Item.class, 1, mode );
			session.setReadOnly( item, true );
			final int version = item.version;
			session.refresh( item );
			final var entry = session.getPersistenceContextInternal().getEntry( item );
			assertThat( entry.isReadOnly() ).isTrue();
			assertThat( entry.getLoadedState() ).isNull();
			assertThat( entry.getVersion() ).isEqualTo( version );
			assertThat( item.version ).isEqualTo( version );
			assertThat( entry.getLockMode() ).isEqualTo( mode );
		} );
	}

	@Test
	void strongerRequestStillIncrementsExactlyOnce(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.get( Item.class, 1, PESSIMISTIC_READ );
			clearInvocations( support );
			session.refresh( item, PESSIMISTIC_FORCE_INCREMENT );
			assertVersion( session, item, 1 );
			verify( support, never() ).canUsePlainReadForRefresh( any(), any() );
			session.refresh( item, PESSIMISTIC_FORCE_INCREMENT );
			assertVersion( session, item, 1 );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = PessimisticLockScope.class, names = { "EXTENDED", "FETCHED" })
	void widerScopeBypassesPlainReload(PessimisticLockScope lockScope, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.get( Item.class, 1, PESSIMISTIC_WRITE );
			clearInvocations( support );
			scope.getCollectingStatementObserver().clear();
			session.refresh( item, PESSIMISTIC_WRITE, lockScope );
			verify( support, never() ).canUsePlainReadForRefresh( any(), any() );
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_WRITE );
			assertThat( item.tags ).containsExactly( "tag" );
			assertReload( scope, PESSIMISTIC_WRITE, true );
			if ( lockScope == PessimisticLockScope.EXTENDED ) {
				assertThat( sql( scope ) ).anyMatch( statement -> statement.contains( "refresh_lock_tags" ) );
			}
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "READPAST is not a reliable physical-row probe")
	void ordinaryRefreshRetainsRootProtection(SessionFactoryScope scope) {
		requirePhysicalLockProbe( scope );
		scope.inTransaction( session -> {
			final var item = session.get( Item.class, 1, PESSIMISTIC_WRITE );
			assertProtected( scope, "refresh_lock_item", "id", true );
			clearInvocations( support );
			session.refresh( item );
			verify( support ).canUsePlainReadForRefresh( PESSIMISTIC_WRITE, LockMode.READ );
			assertProtected( scope, "refresh_lock_item", "id", true );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "READPAST is not a reliable physical-row probe")
	void refreshRetainsRootProtectionAndAcquiresExtendedScope(SessionFactoryScope scope) {
		requirePhysicalLockProbe( scope );
		scope.inTransaction( session -> {
			final var item = session.get( Item.class, 1, PESSIMISTIC_WRITE );
			assertProtected( scope, "refresh_lock_tags", "item_id", false );
			session.refresh( item, PESSIMISTIC_WRITE, PessimisticLockScope.EXTENDED );
			assertProtected( scope, "refresh_lock_item", "id", true );
			assertProtected( scope, "refresh_lock_tags", "item_id", true );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "READPAST is not a reliable physical-row probe")
	void fetchedScopeAcquiresAssociatedEntity(SessionFactoryScope scope) {
		requirePhysicalLockProbe( scope );
		final var blocking = scope.getSessionFactory().getJdbcServices().getJdbcEnvironment().getTransactionConcurrency()
				.getBlockingDuration( Operation.READ, Operation.WRITE );
		assumeTrue( blocking == BlockingDuration.NONE || blocking == BlockingDuration.STATEMENT,
				"This scope-widening probe requires the initial ordinary join read to leave the child unprotected" );
		scope.inTransaction( session -> {
			// Force acquisition of individual targets: some direct-query profiles
			// ignore locking on the nullable side or also lock it in NORMAL scope.
			final var parent = session.get( Parent.class, 1, PESSIMISTIC_WRITE, Locking.FollowOn.FORCE );
			assertProtected( scope, "refresh_scope_child", "id", false );
			clearInvocations( support );
			session.refresh( parent, PESSIMISTIC_WRITE, PessimisticLockScope.FETCHED, Locking.FollowOn.FORCE );
			verify( support, never() ).canUsePlainReadForRefresh( any(), any() );
			assertProtected( scope, "refresh_scope_child", "id", true );
		} );
	}

	private static void assertProtected(SessionFactoryScope scope, String table, String key, boolean locked) {
		scope.inSession( session -> {
			final var transaction = session.beginTransaction();
			try {
				final var rewritten = LockingSqlRewriterSupport.rewrite( session.getDialect().getLockingSupport(),
						"select 1 from " + table + " t where " + key + " = 1",
						new LockOptions( PESSIMISTIC_WRITE ).setTimeout( Timeouts.SKIP_LOCKED ),
						Map.of( "t", new String[] { key } ) );
				final int rows = session.createNativeQuery( rewritten.sql(), Integer.class ).getResultList().size();
				assertThat( rows ).as( "physical protection of %s; SQL: %s", table, sql( scope ) ).isEqualTo( locked ? 0 : 1 );
			}
			finally {
				// On Oracle even a committed lock-only probe can invalidate the
				// owner's SERIALIZABLE snapshot before it widens the lock scope.
				transaction.rollback();
			}
		} );
	}

	private static void requirePhysicalLockProbe(SessionFactoryScope scope) {
		assumeTrue( !PessimisticRefreshSnapshotTest.isSQLServer( scope.getSessionFactory().getJdbcServices().getDialect() ),
				"READPAST is not a reliable physical-row probe, including legacy SQL Server profiles" );
		FollowOnLockingTestSupport.requireAcquisition( scope );
		final var concurrency = scope.getSessionFactory().getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
		assumeTrue( concurrency.getBlockingDuration( Operation.UPDATE_LOCK_READ, Operation.WRITE ) == BlockingDuration.TRANSACTION,
				"Physical protection checks require transaction-duration update locks" );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	@ExtendWith(FollowOnLockingTestSupport.Eligibility.class)
	void widerScopeRetainsFollowOnSkipLockedValidation(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.beginTransaction();
			try {
				final var item = session.get( Item.class, 1, PESSIMISTIC_WRITE );
				scope.getCollectingStatementObserver().clear();
				final var failure = assertThrows( RuntimeException.class, () -> session.refresh(
						item, PESSIMISTIC_WRITE, PessimisticLockScope.EXTENDED, Timeouts.SKIP_LOCKED, Locking.FollowOn.FORCE ) );
				assertThat( org.hibernate.testing.util.ExceptionUtil.rootCause( failure ) )
						.isInstanceOf( org.hibernate.query.IllegalQueryOperationException.class )
						.hasMessage( "SKIP_LOCKED is not supported with follow-on locking; use direct locking for a supported query shape" );
				assertThat( sql( scope ) ).isEmpty();
			}
			finally {
				session.getTransaction().rollback();
			}
		} );
	}

	@Test
	@RequiresDialect(SQLServerDialect.class)
	@RequiresDialect(SybaseDialect.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	void widerScopeRetainsDirectLockingOverride(SessionFactoryScope scope) {
		assumeSkipLockedIsolationSupported( scope );
		scope.inTransaction( session -> {
			final var item = session.get( Item.class, 1, PESSIMISTIC_WRITE );
			clearInvocations( support );
			scope.getCollectingStatementObserver().clear();
			session.refresh( item, PESSIMISTIC_WRITE, PessimisticLockScope.EXTENDED,
					Timeouts.SKIP_LOCKED, Locking.FollowOn.FORCE );
			assertVersion( session, item, 0 );
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_WRITE );
			verify( support, never() ).canUsePlainReadForRefresh( any(), any() );
			// The root is locked by its reload, without a second root acquisition.
			assertThat( sql( scope ).stream().filter( statement -> statement.contains( "from refresh_lock_item " ) ).count() )
					.isEqualTo( 1 );
			assertReload( scope, PESSIMISTIC_WRITE, true, Timeouts.SKIP_LOCKED );
			final var hint = TableLockHintRendererSupport.renderHint( support, PESSIMISTIC_WRITE, Timeouts.SKIP_LOCKED, "tbl" ).strip();
			assertThat( sql( scope ) ).anyMatch( statement -> statement.contains( "from refresh_lock_tags " ) && statement.contains( hint ) );
		} );
	}

	private void assumeSkipLockedIsolationSupported(SessionFactoryScope scope) {
		assumeTrue( !PessimisticRefreshSnapshotTest.isSQLServer(
				scope.getSessionFactory().getJdbcServices().getDialect() )
				|| isolation == java.sql.Connection.TRANSACTION_READ_COMMITTED
				|| isolation == java.sql.Connection.TRANSACTION_REPEATABLE_READ,
				"SQL Server READPAST requires READ COMMITTED or REPEATABLE READ isolation" );
	}

	static Stream<Timeout> callerOptionTimeouts() {
		// Unsupported finite requests are rejected by Spanner before acquisition.
		// Keep the general option-composition test on every locking profile.
		return new DialectFeatureChecks.SupportsWait().apply( DialectContext.getDialect() )
				? Stream.of( Timeouts.WAIT_FOREVER, Timeout.milliseconds( 750 ) )
				: Stream.of( Timeouts.WAIT_FOREVER );
	}

	@ParameterizedTest
	@MethodSource("callerOptionTimeouts")
	void retainsCallerOptionsForNonIncrementingAcquisition(Timeout timeout, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.get( Item.class, 1, PESSIMISTIC_FORCE_INCREMENT );
			final var options = new LockOptions( PESSIMISTIC_FORCE_INCREMENT )
					.setLockScope( PessimisticLockScope.EXTENDED ).setTimeout( timeout )
					.setFollowOnStrategy( Locking.FollowOn.FORCE );
			clearInvocations( support );
			scope.getCollectingStatementObserver().clear();
			session.refresh( item, options );
			assertVersion( session, item, 1 );
			assertThat( options.getLockMode() ).isEqualTo( PESSIMISTIC_FORCE_INCREMENT );
			assertThat( options.getLockScope() ).isEqualTo( PessimisticLockScope.EXTENDED );
			assertThat( options.getTimeout() ).isEqualTo( timeout );
			assertThat( options.getFollowOnStrategy() ).isEqualTo( Locking.FollowOn.FORCE );
			verify( support, never() ).canUsePlainReadForRefresh( any(), any() );
			assertReload( scope, PESSIMISTIC_WRITE, true, timeout );
		} );
	}

	@Test
	void honorsForcedFollowOnAfterAnInitialFollowOnLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.get( Item.class, 1, PESSIMISTIC_WRITE, Locking.FollowOn.FORCE );
			item.name = "local change";
			session.setHibernateFlushMode( FlushMode.MANUAL );
			session.refresh( item, PESSIMISTIC_WRITE, Locking.FollowOn.FORCE );
			assertThat( item.name ).isEqualTo( "database" );
			assertVersion( session, item, 0 );
			assertThat( session.getCurrentLockMode( item ) ).isEqualTo( PESSIMISTIC_WRITE );
		} );
	}

	static void assertVersion(SessionImplementor session, Item item, int version) {
		final var entry = session.getPersistenceContextInternal().getEntry( item );
		assertThat( item.version ).isEqualTo( version );
		assertThat( entry.getVersion() ).isEqualTo( version );
		assertThat( entry.getLoadedState()[entry.getPersister().getVersionPropertyIndex()] ).isEqualTo( version );
	}

	private void assertReload(SessionFactoryScope scope, LockMode mode, boolean locking) {
		assertReload( scope, mode, locking, Timeouts.WAIT_FOREVER );
	}

	private void assertReload(SessionFactoryScope scope, LockMode mode, boolean locking, Timeout timeout) {
		final var fragments = Stream.of( "i1_0", "tbl" ).map( alias -> lockingFragment( mode, timeout, alias ) ).toList();
		assertThat( fragments ).allSatisfy( fragment -> assertThat( fragment ).isNotEmpty() );
		assertThat( sql( scope ).stream().filter( statement -> statement.contains( "select " )
				&& statement.contains( "from refresh_lock_item " ) ).anyMatch( statement -> fragments.stream().anyMatch( statement::contains ) ) )
				.isEqualTo( locking );
	}

	private String lockingFragment(LockMode mode, Timeout timeout, String alias) {
		if ( support.getMetadata().getPessimisticLockStyle() == PessimisticLockStyle.TABLE_HINT ) {
			return TableLockHintRendererSupport.renderHint( support, mode, timeout, alias ).strip();
		}
		final var strategy = mode == PESSIMISTIC_READ
				? support.getMetadata().getReadRowLockStrategy() : support.getMetadata().getWriteRowLockStrategy();
		final List<LockingClauseRequest.Target> targets = switch ( strategy ) {
			case NONE -> List.of();
			case TABLE -> List.of( new LockingClauseRequest.TableTarget( alias ) );
			case COLUMN -> List.of( new LockingClauseRequest.ColumnTarget( alias, "id" ) );
		};
		return support.getLockingClauseRenderer().render( new LockingClauseRequest(
				mode == PESSIMISTIC_READ ? PessimisticLockKind.SHARE : PessimisticLockKind.UPDATE,
				timeout, targets ) ).strip();
	}

	private static List<String> sql(SessionFactoryScope scope) {
		return scope.getCollectingStatementObserver().getSqlQueries();
	}

	@Entity(name = "RefreshLockItem")
	@Table(name = "refresh_lock_item")
	static class Item {
		@Id int id = 1;
		String name = "database";
		@Version int version;
		@ElementCollection
		@CollectionTable(name = "refresh_lock_tags", joinColumns = @JoinColumn(name = "item_id"))
		Set<String> tags = new HashSet<>( Set.of( "tag" ) );
	}

	@Entity(name = "RefreshScopeParent") @Table(name = "refresh_scope_parent")
	static class Parent {
		@Id int id = 1;
		@ManyToOne(fetch = FetchType.EAGER) Child child;
	}

	@Entity(name = "RefreshScopeChild") @Table(name = "refresh_scope_child")
	static class Child {
		@Id int id = 1;
	}
}
