package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Timeout;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Physical contention controls shared by the concurrency-contract tests.
/// A contender performs an actual modification, rather than a locking read
/// whose lock might be compatible with the protection being tested.
///
/// @author Steve Ebersole
public final class ConcurrencyTestSupport {

	private ConcurrencyTestSupport() {
	}

	/// Requires a native lock conflict while the owner remains active, then a
	/// successful UPDATE in a fresh transaction after release. Elapsed time or a
	/// Java-side readiness signal is never evidence of database protection.
	/// Oracle uses an explicitly exclusive FOR UPDATE NOWAIT probe for the
	/// conflict step because it has no connection-level DML timeout strategy.
	/// The contender's actual post-release UPDATE always rolls back.
	public static void assertWriteProtected(
			SessionFactoryImplementor factory, String table, int id, Runnable release) throws Exception {
		assertWriteProtection( factory, table, id, true, release );
	}

	public static void assertWriteAllowed(SessionFactoryImplementor factory, String table, int id) throws Exception {
		assertWriteProtection( factory, table, id, false, () -> {} );
	}

	private static void assertWriteProtection(
			SessionFactoryImplementor factory, String table, int id, boolean protectedRow, Runnable release) throws Exception {
		final boolean oracle = factory.getJdbcServices().getDialect() instanceof OracleDialect;
		assumeTrue( !protectedRow || oracle || supportsNativeLockWait( factory ),
				"No native lock-conflict proof available for " + factory.getJdbcServices().getDialect() );
		final var access = factory.getJdbcServices().getBootstrapJdbcConnectionAccess();
		final var connection = access.obtainConnection();
		final boolean autoCommit = connection.getAutoCommit();
		final var executor = Executors.newSingleThreadExecutor();
		PreparedStatement statement = null;
		LockWait lockWait = null;
		try {
			connection.setAutoCommit( false );
			if ( supportsNativeLockWait( factory ) ) lockWait = boundedLockWait( connection, factory );
			assertEquals( factory.getJdbcServices().getJdbcEnvironment().getJdbcMetadata().getTransactionIsolation(),
					connection.getTransactionIsolation(), "Contender must share the factory configuration" );
			statement = connection.prepareStatement( protectedRow && oracle
					? "select revision from " + table + " where id=? for update nowait"
					: "update " + table + " set revision=revision+1 where id=?" );
			statement.setQueryTimeout( 30 ); // watchdog cancellation is not a native lock conflict
			statement.setInt( 1, id );
			final var probe = statement;
			final var result = executor.submit( () -> {
				if ( protectedRow && oracle ) {
					// Oracle FOR UPDATE acquires exclusive row locks. This is not a
					// generic pessimistic-write SELECT (whose U locks may coexist with S).
					try ( var rows = probe.executeQuery() ) { assertTrue( rows.next() ); }
					return 1;
				}
				return probe.executeUpdate();
			} );
			try {
				if ( protectedRow ) {
					final var conflict = assertThrows( ExecutionException.class, () -> result.get( 35, TimeUnit.SECONDS ),
							"Database did not reject the contender while the owner was active: " + table + "/" + id );
					assertLockConflict( conflict, factory );
				}
				else {
					assertEquals( 1, result.get( 35, TimeUnit.SECONDS ), "Completed read unexpectedly retains write protection" );
				}
			}
			finally {
				// Even delayed JDBC dispatch must finish or fail before releasing the
				// owner. Always release on assertion failure so teardown cannot deadlock.
				release.run();
			}
			connection.rollback(); // a failed contender may have aborted its transaction
			try ( var update = connection.prepareStatement( "update " + table + " set revision=revision+1 where id=?" ) ) {
				update.setQueryTimeout( 30 );
				update.setInt( 1, id );
				assertEquals( 1, update.executeUpdate(), "Fresh contender must progress after release" );
			}
		}
		finally {
			try {
				executor.shutdownNow();
				if ( !executor.awaitTermination( 5, TimeUnit.SECONDS ) ) {
					connection.abort( Runnable::run );
					assertTrue( executor.awaitTermination( 5, TimeUnit.SECONDS ), "Contender survived connection abort" );
				}
				if ( !connection.isClosed() ) {
					if ( statement != null ) statement.close();
					connection.rollback();
					if ( lockWait != null ) lockWait.close();
					connection.setAutoCommit( autoCommit );
				}
			}
			finally { access.releaseConnection( connection ); }
		}
	}

	public static boolean supportsNativeLockWait(SessionFactoryImplementor factory) {
		final var dialect = factory.getJdbcServices().getDialect();
		return dialect instanceof H2Dialect || isDb2Luw( dialect )
				|| dialect.getLockingSupport().getConnectionLockTimeoutStrategy().getSupportedLevel() != ConnectionLockTimeoutStrategy.Level.NONE;
	}

	private static boolean isDb2Luw(org.hibernate.dialect.Dialect dialect) {
		return dialect instanceof DB2Dialect && !(dialect instanceof DB2iDialect || dialect instanceof DB2zDialect);
	}

	public static LockWait boundedLockWait(Connection connection, SessionFactoryImplementor factory) throws SQLException {
		assumeTrue( supportsNativeLockWait( factory ), "No connection-level native lock-wait control for this fixture" );
		return new LockWait( connection, factory );
	}

	/// Test-local timeout controls, including H2 and DB2 controls not exposed by
	/// their production connection strategies. Roll back before restoring after
	/// a database error, since PostgreSQL rejects commands in an aborted transaction.
	/// @author Steve Ebersole
	public static final class LockWait implements AutoCloseable {
		private final Connection connection;
		private final SessionFactoryImplementor factory;
		private final String restore;
		private final Timeout original;

		private LockWait(Connection connection, SessionFactoryImplementor factory) throws SQLException {
			this.connection = connection;
			this.factory = factory;
			final var dialect = factory.getJdbcServices().getDialect();
			if ( dialect instanceof H2Dialect || isDb2Luw( dialect ) ) {
				final boolean h2 = dialect instanceof H2Dialect;
				final String command = h2 ? "set lock_timeout " : "set current lock timeout ";
				try ( var query = connection.createStatement(); var rows = query.executeQuery(
						h2 ? "call lock_timeout()" : "values current lock timeout" ) ) {
					assertTrue( rows.next() );
					restore = command + rows.getInt( 1 );
				}
				execute( command + (h2 ? 100 : 1) );
				original = null;
			}
			else {
				restore = null;
				final var strategy = dialect.getLockingSupport().getConnectionLockTimeoutStrategy();
				original = strategy.getLockTimeout( connection, factory );
				strategy.setLockTimeout( Timeout.milliseconds( 100 ), connection, factory );
			}
		}

		private void execute(String sql) throws SQLException {
			try ( var statement = connection.createStatement() ) { statement.execute( sql ); }
		}

		@Override
		public void close() throws SQLException {
			if ( restore != null ) execute( restore );
			else factory.getJdbcServices().getDialect().getLockingSupport().getConnectionLockTimeoutStrategy()
					.setLockTimeout( original, connection, factory );
		}
	}

	public static void assertLockConflict(Throwable failure, SessionFactoryImplementor factory) {
		final var sql = sqlException( failure );
		final var dialect = factory.getJdbcServices().getDialect();
		if ( dialect instanceof OracleDialect ) assertEquals( 54, sql.getErrorCode(), "Expected Oracle NOWAIT conflict" );
		else if ( dialect instanceof H2Dialect ) assertEquals( 50200, sql.getErrorCode(), "Expected H2 lock timeout" );
		else if ( dialect instanceof PostgreSQLDialect ) assertEquals( "55P03", sql.getSQLState(), "Expected PostgreSQL lock conflict" );
		else if ( dialect instanceof MySQLDialect ) assertEquals( 1205, sql.getErrorCode(), "Expected InnoDB lock timeout" );
		else if ( dialect instanceof SQLServerDialect ) assertEquals( 1222, sql.getErrorCode(), "Expected SQL Server lock timeout" );
		else if ( dialect instanceof SybaseDialect ) assertEquals( 12205, sql.getErrorCode(), "Expected ASE lock timeout" );
		else if ( isDb2Luw( dialect ) ) {
			assertTrue( sql.getErrorCode() == -911 || sql.getErrorCode() == -913, "Expected DB2 locking conflict: " + sql );
		}
		else {
			assertTrue( factory.getJdbcServices().getSqlExceptionHelper().convert( sql, "Contender", null )
					instanceof org.hibernate.exception.LockTimeoutException, "Expected native lock timeout" );
		}
	}

	public static SQLException sqlException(Throwable failure) {
		for ( var cause = failure; cause != null; cause = cause.getCause() ) {
			if ( cause instanceof SQLException sqlException ) {
				return sqlException;
			}
		}
		throw new AssertionError( "Expected a database conflict", failure );
	}

	public static int revision(Connection connection, String sql, int id) throws SQLException {
		try ( var statement = connection.prepareStatement( sql ) ) {
			statement.setQueryTimeout( 30 );
			statement.setInt( 1, id );
			try ( var rows = statement.executeQuery() ) {
				assertTrue( rows.next(), "Missing fixture row " + id );
				final int result = rows.getInt( 1 );
				assertTrue( !rows.next(), "Duplicate fixture row " + id );
				return result;
			}
		}
	}
}
