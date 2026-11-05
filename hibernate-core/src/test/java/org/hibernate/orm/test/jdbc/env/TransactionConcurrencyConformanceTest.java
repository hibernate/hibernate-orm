package org.hibernate.orm.test.jdbc.env;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.orm.test.locking.ConcurrencyConfiguration;
import org.hibernate.orm.test.locking.ConcurrencyTestSupport;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Checks database behavior independently of the descriptor being verified.
/// SQL rendering remains production rendering; expected visibility and conflicts
/// come from the selected database configuration, not from its resolved flags.
///
/// @author Steve Ebersole
@JiraKey("HHH-21035")
@Tag("db-locking")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
class TransactionConcurrencyConformanceTest {

	static Stream<ConcurrencyConfiguration> configurations() {
		return ConcurrencyConfiguration.configurations();
	}

	static Stream<Arguments> readerWriterOutcomes() {
		return configurations().flatMap( configuration -> Stream.of( true, false )
				.map( commit -> Arguments.of( configuration, commit ) ) );
	}

	static Stream<Arguments> currentReads() {
		return configurations().flatMap( configuration -> Stream.of( Operation.CURRENT_READ,
				Operation.SHARED_LOCK_READ, Operation.UPDATE_LOCK_READ ).map( operation -> Arguments.of( configuration, operation ) ) );
	}

	static Stream<Arguments> currentReadWriterOutcomes() {
		return configurations().flatMap( configuration -> Stream.of( Operation.CURRENT_READ,
				Operation.SHARED_LOCK_READ, Operation.UPDATE_LOCK_READ ).flatMap( operation -> Stream.of( true, false )
						.map( commit -> Arguments.of( configuration, operation, commit ) ) ) );
	}

	static Stream<Arguments> lockingReads() {
		return configurations().flatMap( configuration -> Stream.of( Operation.CURRENT_READ,
				Operation.SHARED_LOCK_READ, Operation.UPDATE_LOCK_READ, Operation.READ )
				.flatMap( operation -> Stream.of( true, false )
						.map( commit -> Arguments.of( configuration, operation, commit ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("currentReads")
	void committedChangeDistinguishesSnapshotFromCurrentRead(ConcurrencyConfiguration configuration, Operation operation) throws Exception {
		try ( var fixture = configuration.open(); var reader = fixture.factory.openSession();
				var writer = fixture.factory.openSession() ) {
			final var dialect = fixture.factory.getJdbcServices().getDialect();
			assumeTrue( configuration.knownReadProfile( dialect ), "No independent read-visibility fixture for " + dialect );
			requireOperation( configuration, operation, dialect );
			assumeTrue( !configuration.ordinaryProtection( dialect ), "Ordinary retained reads tested with the write contender" );
			reader.beginTransaction();
			writer.beginTransaction();
			try {
				reader.doWork( fixture::assertConfiguration );
				writer.doWork( fixture::assertConfiguration );
				assertEquals( 0, read( reader, Operation.READ, 1 ) );
				assertEquals( 1, update( writer, "update concurrency_row set revision=1 where id=1" ) );
				writer.getTransaction().commit();
				assertEquals( configuration.stableSnapshot( dialect ) ? 0 : 1, read( reader, Operation.READ, 1 ),
						"Ordinary read visibility: " + diagnostic( fixture ) );
				if ( configuration.currentReadConflicts( dialect ) ) {
					configuration.assertConflict( assertThrows( RuntimeException.class,
							() -> read( reader, operation, 1 ) ), dialect );
				}
				else {
					try {
						assertEquals( 1, read( reader, operation, 1 ), "Current read: " + diagnostic( fixture ) );
					}
					catch (RuntimeException conflict) {
						assertTrue( configuration.mayConflict( dialect, operation ), diagnostic( fixture ) );
						configuration.assertConflict( conflict, dialect );
					}
				}
				final var resolved = fixture.factory.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
				assertEquals( configuration.stableSnapshot( dialect ), resolved.getReadGuarantees( Operation.READ ).hasStableRowView() );
				assertTrue( resolved.supports( operation ), diagnostic( fixture ) );
				assertTrue( resolved.getReadGuarantees( operation ).isCurrentRead(), diagnostic( fixture ) );
				assertTrue( resolved.getReadGuarantees( Operation.READ ).preventsDirtyReads(), diagnostic( fixture ) );
				if ( configuration.ordinaryCurrentRead( dialect ) != null ) {
					assertEquals( configuration.ordinaryCurrentRead( dialect ), resolved.getReadGuarantees( Operation.READ ).isCurrentRead() );
				}
			}
			finally {
				rollback( writer );
				rollback( reader );
			}
		}
	}

	@ParameterizedTest
	@MethodSource("currentReadWriterOutcomes")
	void currentReadResolvesUncommittedWriter(ConcurrencyConfiguration configuration, Operation operation, boolean commit) throws Exception {
		try ( var fixture = configuration.open(); var reader = fixture.factory.openSession();
				var writer = fixture.factory.openSession() ) {
			final var dialect = fixture.factory.getJdbcServices().getDialect();
			assumeTrue( configuration.knownReadProfile( dialect ), "No independent current-read fixture for " + dialect );
			requireOperation( configuration, operation, dialect );
			reader.beginTransaction();
			writer.beginTransaction();
			final var executor = Executors.newSingleThreadExecutor();
			try {
				reader.doWork( fixture::assertConfiguration );
				writer.doWork( fixture::assertConfiguration );
				// A retained ordinary read must not prevent the writer from starting.
				assertEquals( 0, read( reader, Operation.READ, configuration.ordinaryProtection( dialect ) ? 2 : 1 ) );
				assertEquals( 1, update( writer, "update concurrency_row set revision=1 where id=1" ) );
				if ( Boolean.FALSE.equals( configuration.ordinaryCurrentRead( dialect ) ) ) {
					assertEquals( 0, read( reader, Operation.READ, 1 ), "Ordinary MVCC read excludes uncommitted values" );
				}
				final var started = new CountDownLatch( 1 );
				final var result = executor.submit( () -> {
					started.countDown();
					return read( reader, operation, 1 );
				} );
				try {
					assertTrue( started.await( 10, TimeUnit.SECONDS ) );
					try {
						final int premature = result.get( 200, TimeUnit.MILLISECONDS );
						fail( "Current read returned before writer completion: revision=" + premature + "; " + diagnostic( fixture ) );
					}
					catch (TimeoutException expectedWait) {
						// Outcome/value checks below distinguish real freshness from an old snapshot.
					}
					catch (ExecutionException earlyConflict) {
						assertTrue( configuration.mayConflict( dialect, operation ), diagnostic( fixture ) );
						configuration.assertConflict( earlyConflict, dialect );
					}
				}
				finally {
					if ( commit ) writer.getTransaction().commit(); else writer.getTransaction().rollback();
				}
				try {
					assertEquals( commit ? 1 : 0, result.get( 30, TimeUnit.SECONDS ), diagnostic( fixture ) );
					assertFalse( commit && configuration.currentReadConflicts( dialect ), "Expected snapshot conflict" );
				}
				catch (ExecutionException conflict) {
					assertTrue( configuration.mayConflict( dialect, operation ), diagnostic( fixture ) );
					configuration.assertConflict( conflict, dialect );
				}
			}
			finally {
				rollback( writer );
				executor.shutdownNow();
				assertTrue( executor.awaitTermination( 30, TimeUnit.SECONDS ), "Current read survived writer completion" );
				rollback( reader );
			}
		}
	}

	@ParameterizedTest
	@MethodSource("lockingReads")
	void readProtectionSurvivesStatementClosure(
			ConcurrencyConfiguration configuration, Operation operation, boolean commit) throws Exception {
		try ( var fixture = configuration.open(); var owner = fixture.factory.openSession() ) {
			final var dialect = fixture.factory.getJdbcServices().getDialect();
			assumeTrue( configuration.knownReadProfile( dialect ), "No independent lock fixture for " + dialect );
			requireOperation( configuration, operation, dialect );
			final boolean protection = operation == Operation.READ ? configuration.ordinaryProtection( dialect )
					: operation != Operation.CURRENT_READ || !configuration.shortCurrentRead( dialect );
			owner.beginTransaction();
			try {
				owner.doWork( fixture::assertConfiguration );
				assertEquals( 0, read( owner, operation, 1 ) ); // closes all JDBC read resources
				final var resolved = fixture.factory.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
				assertTrue( resolved.supports( operation ), diagnostic( fixture ) );
				assertEquals( protection, resolved.getReadGuarantees( operation ).preventsConcurrentModification(), diagnostic( fixture ) );
				assertEquals( protection, resolved.getReadGuarantees( operation ).holdsRowLockUntilTransactionCompletion(), diagnostic( fixture ) );
				// DB2's unobserved currently-committed option may leave this pair
				// UNKNOWN; that underclaim does not promise retained protection.
				if ( operation == Operation.READ && dialect instanceof org.hibernate.dialect.DB2Dialect ) {
					assertNotEquals( BlockingDuration.TRANSACTION, resolved.getBlockingDuration( operation, Operation.WRITE ) );
				}
				// SQL Server's ordinary versioned reads do not block writers;
				// its explicit current reads still use statement-duration locking.
				else assertEquals( protection ? BlockingDuration.TRANSACTION
						: operation == Operation.CURRENT_READ || dialect instanceof SQLServerDialect
								&& Boolean.TRUE.equals( configuration.ordinaryCurrentRead( dialect ) )
								|| dialect instanceof org.hibernate.dialect.SybaseDialect ? BlockingDuration.STATEMENT : BlockingDuration.NONE,
						resolved.getBlockingDuration( operation, Operation.WRITE ), diagnostic( fixture ) );
				if ( protection ) {
					ConcurrencyTestSupport.assertWriteProtected( fixture.factory, "concurrency_row", 1, () -> {
						if ( commit ) owner.getTransaction().commit(); else owner.getTransaction().rollback();
					} );
				}
				else {
					ConcurrencyTestSupport.assertWriteAllowed( fixture.factory, "concurrency_row", 1 );
				}
			}
			finally { rollback( owner ); }
		}
	}

	private static void requireOperation(ConcurrencyConfiguration configuration, Operation operation, Dialect dialect) {
		assumeTrue( operation != Operation.SHARED_LOCK_READ || configuration.sharedRead( dialect ),
				"This database has no actual shared-lock-read operation" );
		assumeTrue( operation != Operation.UPDATE_LOCK_READ || !(dialect instanceof org.hibernate.dialect.SybaseDialect),
				"Sybase pessimistic-write syntax acquires a shared lock" );
	}

	@ParameterizedTest
	@MethodSource("readerWriterOutcomes")
	void writeRetainsProtectionUntilCompletion(ConcurrencyConfiguration configuration, boolean commit) throws Exception {
		try ( var fixture = configuration.open(); var owner = fixture.factory.openSession() ) {
			assumeTrue( configuration.knownReadProfile( fixture.factory.getJdbcServices().getDialect() ),
					"No independent write-exclusion fixture for this database" );
			owner.beginTransaction();
			try {
				owner.doWork( fixture::assertConfiguration );
				assertEquals( 1, update( owner, "update concurrency_row set revision=1 where id=1 and revision=0" ) );
				assertEquals( BlockingDuration.TRANSACTION, fixture.factory.getJdbcServices().getJdbcEnvironment()
						.getTransactionConcurrency().getBlockingDuration( Operation.WRITE, Operation.WRITE ), diagnostic( fixture ) );
				ConcurrencyTestSupport.assertWriteProtected( fixture.factory, "concurrency_row", 1, () -> {
					if ( commit ) owner.getTransaction().commit(); else owner.getTransaction().rollback();
				} );
			}
			finally { rollback( owner ); }
		}
	}

	@ParameterizedTest
	@MethodSource("readerWriterOutcomes")
	void conditionalWriteResolvesUncommittedWriter(ConcurrencyConfiguration configuration, boolean commit) throws Exception {
		try ( var fixture = configuration.open(); var contender = fixture.factory.openSession();
				var writer = fixture.factory.openSession() ) {
			final var dialect = fixture.factory.getJdbcServices().getDialect();
			assumeTrue( !configuration.ordinaryProtection( dialect ), "Retained ordinary reads tested separately" );
			requireWriteAfterRead( configuration, fixture );
			contender.beginTransaction();
			writer.beginTransaction();
			final var executor = Executors.newSingleThreadExecutor();
			try {
				contender.doWork( fixture::assertConfiguration );
				writer.doWork( fixture::assertConfiguration );
				assertEquals( 0, read( contender, Operation.READ, 1 ) );
				assertEquals( 1, update( writer, "update concurrency_row set revision=1 where id=1 and revision=0" ) );
				final var started = new CountDownLatch( 1 );
				final var result = executor.submit( () -> {
					started.countDown();
					return update( contender, "update concurrency_row set revision=2 where id=1 and revision=0" );
				} );
				try {
					assertTrue( started.await( 10, TimeUnit.SECONDS ) );
					try {
						fail( "Conditional write returned before writer completion: " + result.get( 200, TimeUnit.MILLISECONDS ) );
					}
					catch (TimeoutException expectedWait) {
					}
					catch (ExecutionException earlyConflict) {
						assertTrue( configuration.conditionalWriteConflicts( dialect ), diagnostic( fixture ) );
						configuration.assertConflict( earlyConflict, dialect );
					}
				}
				finally {
					if ( commit ) writer.getTransaction().commit(); else writer.getTransaction().rollback();
				}
				try {
					assertEquals( commit ? 0 : 1, result.get( 30, TimeUnit.SECONDS ), diagnostic( fixture ) );
				}
				catch (ExecutionException conflict) {
					assertTrue( configuration.conditionalWriteConflicts( dialect ), diagnostic( fixture ) );
					configuration.assertConflict( conflict, dialect );
				}
			}
			finally {
				rollback( writer );
				executor.shutdownNow();
				assertTrue( executor.awaitTermination( 30, TimeUnit.SECONDS ), "Conditional write survived writer completion" );
				rollback( contender );
			}
			try ( var control = fixture.factory.openSession() ) {
				control.beginTransaction();
				try { assertEquals( commit ? 1 : 0, read( control, Operation.READ, 1 ) ); }
				finally { rollback( control ); }
			}
		}
	}

	@ParameterizedTest
	@MethodSource("configurations")
	void conditionalWriteRejectsObsoleteRevision(ConcurrencyConfiguration configuration) throws Exception {
		try ( var fixture = configuration.open(); var stale = fixture.factory.openSession();
				var writer = fixture.factory.openSession() ) {
			final var dialect = fixture.factory.getJdbcServices().getDialect();
			assumeTrue( !configuration.ordinaryProtection( dialect ), "Retained ordinary reads tested separately" );
			requireWriteAfterRead( configuration, fixture );
			stale.beginTransaction();
			writer.beginTransaction();
			try {
				stale.doWork( fixture::assertConfiguration );
				writer.doWork( fixture::assertConfiguration );
				assertEquals( 0, read( stale, Operation.READ, 1 ) );
				assertEquals( 1, update( writer, "update concurrency_row set revision=1 where id=1 and revision=0" ) );
				writer.getTransaction().commit();
				try {
					assertEquals( 0, update( stale, "update concurrency_row set revision=2 where id=1 and revision=0" ), diagnostic( fixture ) );
				}
				catch (RuntimeException failure) {
					assertTrue( configuration.conditionalWriteConflicts( dialect ), diagnostic( fixture ) );
					configuration.assertConflict( failure, dialect );
				}
			}
			finally { rollback( writer ); rollback( stale ); }
			try ( var control = fixture.factory.openSession() ) {
				control.beginTransaction();
				try {
					assertEquals( 1, read( control, Operation.READ, 1 ) );
					assertEquals( 1, update( control, "update concurrency_row set revision=2 where id=1 and revision=1" ) );
				}
				finally { rollback( control ); }
			}
		}
	}

	private static int read(Session session, Operation operation, int id) {
		final var factory = session.getSessionFactory().unwrap( org.hibernate.engine.spi.SessionFactoryImplementor.class );
		if ( operation == Operation.CURRENT_READ ) {
			// SimpleSelect may satisfy a current-read *request* with READ when it
			// is already current. Here we test the explicit CURRENT_READ operation,
			// whose retention can differ even when both return current values.
			final var support = factory.getJdbcServices().getDialect().getLockingSupport();
			final var concurrency = factory.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
			final var sql = "select revision from concurrency_row"
					+ support.renderCurrentReadTableHint( "concurrency_row", concurrency ) + " where id=?"
					+ support.renderCurrentReadClause( concurrency );
			return session.doReturningWork( connection -> ConcurrencyTestSupport.revision( connection, sql, id ) );
		}
		final var select = new SimpleSelect( factory )
				.setTableName( "concurrency_row" ).addColumn( "revision" ).addRestriction( "id" )
				.setLockMode( operation == Operation.SHARED_LOCK_READ ? LockMode.PESSIMISTIC_READ
						: operation == Operation.UPDATE_LOCK_READ ? LockMode.PESSIMISTIC_WRITE : LockMode.NONE )
				.toStatementString();
		return session.doReturningWork( connection -> ConcurrencyTestSupport.revision( connection, select, id ) );
	}

	private static int update(Session session, String sql) {
		return session.doReturningWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.setQueryTimeout( 30 ); // a watchdog failure must fail the test
				return statement.executeUpdate( sql );
			}
		} );
	}

	private static void requireWriteAfterRead(ConcurrencyConfiguration configuration, ConcurrencyConfiguration.Fixture fixture) {
		if ( !configuration.knownReadProfile( fixture.factory.getJdbcServices().getDialect() ) ) {
			// This is eligibility for a CAS test, not the READ guarantee under test.
			// In particular, a retained Spanner read cannot participate in this interleaving.
			final var duration = fixture.factory.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency()
					.getBlockingDuration( Operation.READ, Operation.WRITE );
			assumeTrue( duration == BlockingDuration.NONE || duration == BlockingDuration.STATEMENT,
					"CAS race requires a completed ordinary read to allow a competing writer: " + duration );
		}
	}

	private static void rollback(Session session) {
		if ( session.getTransaction().isActive() ) session.getTransaction().rollback();
	}

	private static String diagnostic(ConcurrencyConfiguration.Fixture fixture) {
		try ( var session = fixture.factory.openSession() ) {
			return session.doReturningWork( connection -> {
				final var metadata = connection.getMetaData();
				return metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion()
						+ "; " + metadata.getDriverName() + " " + metadata.getDriverVersion()
						+ "; " + fixture + "; isolation=" + connection.getTransactionIsolation() + "; "
						+ fixture.factory.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency().getName();
			} );
		}
	}
}
