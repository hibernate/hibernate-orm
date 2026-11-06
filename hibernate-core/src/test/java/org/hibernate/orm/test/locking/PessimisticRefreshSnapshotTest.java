package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Version;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.testing.jdbc.JdbcProperties;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_HANDLING;
import static org.hibernate.cfg.JdbcSettings.ISOLATION;
import static org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider.INIT_SQL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Refresh must retain the state obtained by a pessimistic load even when an earlier
/// consistent read established a snapshot preceding a concurrent insert or update.
/// Each isolation parameter configures the service registry before SessionFactory
/// bootstrap, keeping concurrency metadata and both transactions' connections aligned.
/// No test changes isolation on an individual connection.
/// PostgreSQL repeatable-read locking reads retain the snapshot: a later insert is
/// invisible, and locking a row updated since the snapshot raises a serialization failure.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		PessimisticRefreshSnapshotTest.SnapshotMarker.class,
		PessimisticRefreshSnapshotTest.RefreshTarget.class,
		PessimisticRefreshSnapshotTest.TimestampTarget.class,
		PessimisticRefreshSnapshotTest.UnversionedTarget.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ParameterizedClass(name = "isolation={0}")
@MethodSource("isolationLevels")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@JiraKey("HHH-21010")
@Tag("db-locking")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
public class PessimisticRefreshSnapshotTest implements ServiceRegistryProducer {
	private final int isolationLevel;

	public PessimisticRefreshSnapshotTest(int isolationLevel) {
		this.isolationLevel = isolationLevel;
	}

	static Stream<Integer> isolationLevels() {
		return isH2( DialectContext.getDialect() )
				? Stream.of( TRANSACTION_READ_COMMITTED, TRANSACTION_REPEATABLE_READ, 6 )
				: Stream.of( TRANSACTION_READ_COMMITTED, TRANSACTION_REPEATABLE_READ );
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		assumeIsolationSupported( isolationLevel );
		return configureIsolation( builder, isolationLevel )
				.applySetting( org.hibernate.generator.internal.CurrentTimestampGeneration.CLOCK_SETTING_NAME,
						Clock.fixed( Instant.parse( "2026-01-01T00:00:00Z" ), ZoneOffset.UTC ) )
				.applySetting( CONNECTION_HANDLING, "DELAYED_ACQUISITION_AND_HOLD" )
				.build();
	}

	static void assumeIsolationSupported(int isolationLevel) {
		final var dialect = DialectContext.getDialect();
		assumeTrue( isolationLevel == Connection.TRANSACTION_SERIALIZABLE
				|| !(dialect instanceof SpannerDialect || dialect instanceof SpannerPostgreSQLDialect
						|| dialect instanceof CockroachDialect && dialect.getVersion().isBefore( 23, 2 )),
				"This profile supports only SERIALIZABLE" );
		// Check support before configuring the pool: unsupported isolation may
		// otherwise fail during bootstrap, before a test can be skipped.
		final var jdbc = JdbcProperties.INSTANCE;
		assumeTrue( assertDoesNotThrow( () -> {
			try ( var connection = DriverManager.getConnection( jdbc.getUrl(), jdbc.getUser(), jdbc.getPassword() ) ) {
				if ( isolationLevel == 4096 && isSQLServer( dialect ) ) {
					try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
							"select snapshot_isolation_state from sys.databases where name=db_name()" ) ) {
						return rows.next() && rows.getInt( 1 ) == 1;
					}
				}
				return connection.getMetaData().supportsTransactionIsolationLevel( isolationLevel );
			}
		} ), "JDBC driver does not support isolation level " + isolationLevel );
	}

	static StandardServiceRegistryBuilder configureIsolation(StandardServiceRegistryBuilder builder, int isolation) {
		if ( isolation == 4096 ) {
			// A raw SET statement leaves the Microsoft driver's cached isolation at
			// READ_COMMITTED. Its connection property initializes both JDBC and SQL.
			builder.getSettings().remove( ISOLATION );
			return builder.applySetting( "hibernate.connection.defaultTransactionIsolation", "SNAPSHOT" );
		}
		if ( isolation == 6 ) {
			// The standard isolation setting accepts JDBC constants only. Initialize
			// every physical pool connection before bootstrap observes its metadata.
			builder.getSettings().remove( ISOLATION );
			return builder.applySetting( INIT_SQL, "set session characteristics as transaction isolation level snapshot" );
		}
		return builder.applySetting( ISOLATION, isolation );
	}

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		assertEquals( isolationLevel,
				scope.getSessionFactory().getJdbcServices().getJdbcEnvironment().getJdbcMetadata().getTransactionIsolation(),
				"Bootstrap metadata must describe the configured isolation" );
		scope.inTransaction( session -> {
			assertIsolation( session );
			session.persist( new SnapshotMarker() );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Arguments> refreshModes() {
		return Stream.of( LockMode.PESSIMISTIC_READ, LockMode.PESSIMISTIC_WRITE ).flatMap( loadMode ->
				Stream.of( loadMode, LockMode.READ, LockMode.NONE ).map( refreshMode ->
						Arguments.of( loadMode, refreshMode ) ) );
	}

	static Stream<Arguments> versionedRefreshModes() {
		return Stream.concat( refreshModes(), Stream.of(
				Arguments.of( LockMode.PESSIMISTIC_WRITE, LockMode.PESSIMISTIC_READ ),
				Arguments.of( LockMode.PESSIMISTIC_FORCE_INCREMENT, LockMode.PESSIMISTIC_FORCE_INCREMENT ),
				Arguments.of( LockMode.PESSIMISTIC_FORCE_INCREMENT, LockMode.PESSIMISTIC_WRITE ),
				Arguments.of( LockMode.PESSIMISTIC_FORCE_INCREMENT, LockMode.PESSIMISTIC_READ ),
				Arguments.of( LockMode.PESSIMISTIC_FORCE_INCREMENT, LockMode.READ ),
				Arguments.of( LockMode.PESSIMISTIC_FORCE_INCREMENT, LockMode.NONE ) ) );
	}

	@ParameterizedTest(name = "load={0}, refresh={1}")
	@MethodSource("versionedRefreshModes")
	void refreshAfterConcurrentInsert(
			LockMode loadMode, LockMode refreshMode, SessionFactoryScope scope) {
		withSnapshot( scope, session -> {
			// Transaction B commits after A's snapshot, before A obtains any target-row lock.
			scope.inTransaction( other -> {
				assertIsolation( other );
				other.persist( new RefreshTarget( "inserted" ) );
			} );
			if ( hasFixedLockingSnapshot( scope ) ) {
				assertNull( session.find( RefreshTarget.class, 1, loadMode ),
						"Locking reads must not see rows inserted after the transaction snapshot" );
				return;
			}
			if ( isMariaSnapshotIsolation( scope, session ) ) {
				assertSnapshotConflict( () -> session.find( RefreshTarget.class, 1, loadMode ), 1020 );
				return;
			}
			assertRefreshRetainsState( session, loadMode, refreshMode, "inserted", 0 );
		} );
	}

	@ParameterizedTest(name = "load={0}, refresh={1}")
	@MethodSource("versionedRefreshModes")
	void refreshAfterConcurrentUpdate(
			LockMode loadMode, LockMode refreshMode, SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new RefreshTarget( "original" ) ) );
		withSnapshot( scope, session -> {
			scope.inTransaction( other -> {
				assertIsolation( other );
				final var target = other.get( RefreshTarget.class, 1 );
				target.data = "updated";
			} );
			if ( hasFixedLockingSnapshot( scope ) ) {
				final var failure = assertThrows( PersistenceException.class,
						() -> session.find( RefreshTarget.class, 1, loadMode ),
						"Locking a row updated since the transaction snapshot must fail" );
				final var sqlException = sqlException( failure );
				assertEquals( "40001", sqlException.getSQLState(), "Expected a serialization failure" );
				return;
			}
			if ( isMariaSnapshotIsolation( scope, session ) ) {
				assertSnapshotConflict( () -> session.find( RefreshTarget.class, 1, loadMode ), 1020 );
				return;
			}
			assertRefreshRetainsState( session, loadMode, refreshMode, "updated", 1 );
		} );
	}

	@ParameterizedTest(name = "load={0}, refresh={1}")
	@MethodSource("refreshModes")
	void repeatedRefreshAfterConcurrentUnversionedUpdate(
			LockMode loadMode, LockMode refreshMode, SessionFactoryScope scope) {
		final var concurrency = scope.getSessionFactory().getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
		final var blocking = concurrency.getBlockingDuration( Operation.READ, Operation.WRITE );
		assumeTrue( blocking == BlockingDuration.NONE || blocking == BlockingDuration.STATEMENT,
				"The initial ordinary read must allow the concurrent update: " + blocking );
		scope.inTransaction( session -> session.persist( new UnversionedTarget( "original" ) ) );
		withSnapshot( scope, session -> {
			final var target = session.get( UnversionedTarget.class, 1 );
			assertEquals( "original", target.data );
			scope.inTransaction( other -> {
				assertIsolation( other );
				other.get( UnversionedTarget.class, 1 ).data = "updated";
			} );
			if ( hasFixedLockingSnapshot( scope )
					|| isolationLevel == TRANSACTION_REPEATABLE_READ
							&& isH2( scope.getSessionFactory().getJdbcServices().getDialect() ) ) {
				// H2 also detects the conflict once the target itself has been read.
				final var failure = assertThrows( PersistenceException.class, () -> session.refresh( target, loadMode ) );
				assertEquals( "40001", sqlException( failure ).getSQLState() );
				return;
			}
			if ( isMariaSnapshotIsolation( scope, session ) ) {
				assertSnapshotConflict( () -> session.refresh( target, loadMode ), 1020 );
				return;
			}
			session.refresh( target, loadMode );
			for ( int refresh = 0; refresh < 2; refresh++ ) {
				assertEquals( "updated", target.data );
				session.refresh( target, refreshMode );
				assertEquals( "updated", target.data, "An unversioned refresh must not restore the earlier snapshot" );
				assertEquals( loadMode, session.getCurrentLockMode( target ) );
			}
		} );
	}

	private boolean isMariaSnapshotIsolation(SessionFactoryScope scope, Session session) {
		if ( isolationLevel != TRANSACTION_REPEATABLE_READ
				|| !(scope.getSessionFactory().getJdbcServices().getDialect() instanceof MariaDBDialect) ) {
			return false;
		}
		return session.doReturningWork( connection -> {
			try ( var statement = connection.createStatement();
					var rows = statement.executeQuery( "select @@session.innodb_snapshot_isolation" ) ) {
				rows.next();
				return rows.getBoolean( 1 );
			}
			catch (SQLException failure) {
				if ( failure.getErrorCode() == 1193 ) {
					return false; // Older MariaDB versions have no setting and use current locking reads.
				}
				throw failure;
			}
		} );
	}

	private static void assertSnapshotConflict(Runnable acquisition, int errorCode) {
		final var failure = assertThrows( PersistenceException.class, acquisition::run );
		assertEquals( errorCode, sqlException( failure ).getErrorCode() );
	}

	@Test
	void sameValueTimestampForceIncrementDoesNotProveOwnWriteVisibility(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TimestampTarget() ) );
		withSnapshot( scope, session -> {
			scope.inTransaction( other -> other.get( TimestampTarget.class, 1 ).data = "updated" );
			if ( hasFixedLockingSnapshot( scope ) ) {
				final var failure = assertThrows( PersistenceException.class,
						() -> session.find( TimestampTarget.class, 1, LockMode.PESSIMISTIC_FORCE_INCREMENT ) );
				assertEquals( "40001", sqlException( failure ).getSQLState() );
				return;
			}
			if ( isMariaSnapshotIsolation( scope, session ) ) {
				assertSnapshotConflict( () -> session.find( TimestampTarget.class, 1, LockMode.PESSIMISTIC_FORCE_INCREMENT ), 1020 );
				return;
			}
			final var target = session.find( TimestampTarget.class, 1, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertNotNull( target );
			assertEquals( "updated", target.data );
			assertEquals( Instant.parse( "2026-01-01T00:00:00Z" ), target.version,
					"The fixed clock models repeated generation at the timestamp's precision" );
			scope.getCollectingStatementObserver().clear();
			for ( var requested : new LockMode[] { LockMode.NONE, LockMode.PESSIMISTIC_FORCE_INCREMENT } ) {
				session.refresh( target, requested );
				assertEquals( "updated", target.data, "A same-value version update must not permit stale snapshot restoration" );
				assertEquals( Instant.parse( "2026-01-01T00:00:00Z" ), target.version );
				final var entry = ((SharedSessionContractImplementor) session).getPersistenceContextInternal().getEntry( target );
				assertEquals( target.version, entry.getVersion() );
				assertEquals( target.version, entry.getLoadedState()[entry.getPersister().getVersionPropertyIndex()] );
				assertEquals( LockMode.PESSIMISTIC_FORCE_INCREMENT, entry.getLockMode() );
			}
			org.assertj.core.api.Assertions.assertThat( scope.getCollectingStatementObserver().getSqlQueries() )
					.noneMatch( sql -> sql.startsWith( "update " ) );
		} );
	}

	private static SQLException sqlException(Throwable failure) {
		Throwable cause = failure;
		while ( cause != null && !(cause instanceof SQLException) ) {
			cause = cause.getCause();
		}
		return assertInstanceOf( SQLException.class, cause );
	}

	private boolean hasFixedLockingSnapshot(SessionFactoryScope scope) {
		final var dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		return dialect.getLockingSupport().getTransactionConcurrencyResolver()
				instanceof org.hibernate.dialect.lock.internal.PostgreSQLTransactionConcurrencyResolver
				&& isolationLevel == TRANSACTION_REPEATABLE_READ
				|| isH2( dialect ) && isolationLevel == 6;
	}

	// Classify the selected family profile so legacy Dialects use the same
	// native-isolation and acquisition-outcome rules as their current counterparts.
	static boolean isH2(org.hibernate.dialect.Dialect dialect) {
		return dialect.getLockingSupport().getTransactionConcurrencyResolver()
				instanceof org.hibernate.dialect.lock.internal.H2TransactionConcurrencyResolver;
	}

	static boolean isSQLServer(org.hibernate.dialect.Dialect dialect) {
		return dialect.getLockingSupport().getTransactionConcurrencyResolver()
				instanceof org.hibernate.dialect.lock.internal.SQLServerTransactionConcurrencyResolver;
	}

	private void assertIsolation(Session session) {
		assertEquals( isolationLevel, session.doReturningWork( Connection::getTransactionIsolation ),
				"Connection isolation must match the factory configuration" );
	}

	private void withSnapshot(SessionFactoryScope scope, Consumer<Session> work) {
		scope.inSession( session -> {
			assertIsolation( session );
			session.beginTransaction();
			try {
				// A separate table establishes the snapshot without locking the row B will change.
				assertEquals( 1, session.createQuery(
						"select m.id from SnapshotMarker m where m.id = 1", Integer.class ).getSingleResult() );
				work.accept( session );
				// A never commits, also covering expected acquisition failures.
			}
			finally {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
			}
		} );
	}

	private static void assertRefreshRetainsState(
			Session session, LockMode loadMode, LockMode refreshMode, String expectedData, int expectedVersion) {
		final var target = session.find( RefreshTarget.class, 1, loadMode );
		if ( loadMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			expectedVersion++;
		}
		assertNotNull( target, "Pessimistic load must see the concurrently committed row" );
		assertEquals( expectedData, target.data, "State after pessimistic load" );
		assertEquals( expectedVersion, target.version, "Version after pessimistic load" );
		assertEntryVersion( session, target, expectedVersion );
		assertEquals( loadMode, session.getCurrentLockMode( target ) );

		for ( int refresh = 1; refresh <= 2; refresh++ ) {
			if ( refreshMode == LockMode.NONE ) {
				session.refresh( target );
			}
			else {
				session.refresh( target, refreshMode );
			}
			assertEquals( expectedData, target.data, "State after refresh " + refresh );
			assertEquals( expectedVersion, target.version, "Version after refresh " + refresh );
			assertEntryVersion( session, target, expectedVersion );
			assertEquals( loadMode, session.getCurrentLockMode( target ), "Lock mode after refresh " + refresh );
		}
	}

	private static void assertEntryVersion(Session session, RefreshTarget target, int expectedVersion) {
		final var entry = ((SharedSessionContractImplementor) session).getPersistenceContextInternal().getEntry( target );
		assertEquals( expectedVersion, entry.getVersion() );
		assertEquals( expectedVersion, entry.getLoadedState()[entry.getPersister().getVersionPropertyIndex()] );
	}

	@Entity(name = "UnversionedRefreshTarget")
	public static class UnversionedTarget {
		@Id int id = 1;
		String data;
		public UnversionedTarget() {}
		UnversionedTarget(String data) { this.data = data; }
	}

	@Entity(name = "SnapshotMarker")
	public static class SnapshotMarker {
		@Id
		int id = 1;
	}

	@Entity(name = "TimestampRefreshTarget")
	public static class TimestampTarget {
		@Id int id = 1;
		String data = "original";
		@Version Instant version;
	}

	@Entity(name = "RefreshTarget")
	public static class RefreshTarget {
		@Id
		int id = 1;

		String data;

		@Version
		int version;

		public RefreshTarget() {
		}

		RefreshTarget(String data) {
			this.data = data;
		}
	}
}
