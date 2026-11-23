package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.hibernate.LockMode;
import org.hibernate.StatementObserver;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.lock.internal.OptimisticEntityLockException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.locking.ConcurrencyConfiguration.Row;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Drives deferred optimistic validation through the ORM version-read path,
/// with a snapshot established before another transaction changes the row.
/// The raw writer deliberately leaves cached state old: validation must read
/// the database, using the factory's actual configured current-read strategy.
///
/// @author Steve Ebersole
@JiraKey("HHH-21035")
@Tag("db-locking")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
class OptimisticValidationConfigurationTest {
	static Stream<Arguments> scenarios() {
		return ConcurrencyConfiguration.configurations().flatMap( configuration ->
				Stream.of( "graph", "legacy" ).flatMap( queue -> Stream.of( false, true ).flatMap( managedQuery ->
						Stream.of( false, true ).map( commitWriter -> Arguments.of( configuration, queue, managedQuery, commitWriter ) ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("scenarios")
	void validatesAfterWriterCompletion(
			ConcurrencyConfiguration configuration, String queue, boolean managedQuery, boolean commitWriter) throws Exception {
		try ( var fixture = configuration.openWithQueue( queue ) ) {
			requireRace( configuration, fixture );
			final var reads = new AtomicInteger();
			try ( var reader = reader( fixture, reads ); var writer = fixture.factory.openSession() ) {
				final var transaction = reader.beginTransaction();
				writer.beginTransaction();
				try {
					request( fixture, reader, managedQuery );
					change( fixture, writer );
					if ( commitWriter ) writer.getTransaction().commit(); else writer.getTransaction().rollback();
					assertEquals( 0, reads.get(), "Validation must be deferred" );
					if ( commitWriter ) {
						final var failure = assertThrows( RuntimeException.class, transaction::commit,
								"A snapshot must not validate the obsolete entry version: " + configuration );
						if ( configuration.currentReadConflicts( fixture.factory.getJdbcServices().getDialect() ) ) {
							configuration.assertConflict( failure, fixture.factory.getJdbcServices().getDialect() );
						}
						else assertOptimisticFailure( failure );
					}
					else transaction.commit();
					assertEquals( 1, reads.get(), "Actual deferred persister version read" );
				}
				finally {
					if ( writer.getTransaction().isActive() ) writer.getTransaction().rollback();
					if ( transaction.isActive() ) transaction.rollback();
				}
			}
			assertDatabase( fixture, commitWriter ? 1 : 0 );
		}
	}

	@ParameterizedTest
	@MethodSource("scenarios")
	void refusesSnapshotValidationWhileWriterIsUncommitted(
			ConcurrencyConfiguration configuration, String queue, boolean managedQuery, boolean commitWriter) throws Exception {
		try ( var fixture = configuration.openWithQueue( queue ) ) {
			requireRace( configuration, fixture );
			assumeTrue( ConcurrencyTestSupport.supportsNativeLockWait( fixture.factory ),
					"Deferred version SELECT has no connection-level native lock-wait control" );
			final var reads = new AtomicInteger();
			try ( var reader = reader( fixture, reads ); var writer = fixture.factory.openSession() ) {
				final var transaction = reader.beginTransaction();
				writer.beginTransaction();
				final Connection connection = reader.doReturningWork( c -> c );
				final var lockWait = ConcurrencyTestSupport.boundedLockWait( connection, fixture.factory );
				final var executor = Executors.newSingleThreadExecutor();
				try {
					request( fixture, reader, managedQuery );
					change( fixture, writer );
					assertEquals( 0, reads.get() );
					final var completion = executor.submit( transaction::commit );
					final var failure = assertThrows( ExecutionException.class, () -> completion.get( 35, TimeUnit.SECONDS ),
							"Deferred validation completed while the writer was still uncommitted" );
					final var sql = ConcurrencyTestSupport.sqlException( failure );
					final var dialect = fixture.factory.getJdbcServices().getDialect();
					if ( configuration.currentReadConflicts( dialect ) && ("40001".equals( sql.getSQLState() )
							|| dialect instanceof MariaDBDialect && sql.getErrorCode() == 1020) ) {
						// A configured snapshot conflict is a valid refusal, but is
						// not counted as proof of retained row protection.
						configuration.assertConflict( failure, dialect );
					}
					else ConcurrencyTestSupport.assertLockConflict( failure, fixture.factory );
					assertEquals( 1, reads.get(), "Conflict must come from deferred version validation" );
				}
				finally {
					// Writer completion follows database evidence, never a scheduling timeout.
					if ( writer.getTransaction().isActive() ) {
						if ( commitWriter ) writer.getTransaction().commit(); else writer.getTransaction().rollback();
					}
					executor.shutdownNow();
					if ( !executor.awaitTermination( 5, TimeUnit.SECONDS ) ) {
						connection.abort( Runnable::run );
						assertTrue( executor.awaitTermination( 5, TimeUnit.SECONDS ) );
					}
					if ( transaction.isActive() ) transaction.rollback();
					connection.rollback();
					lockWait.close();
				}
			}
			assertDatabase( fixture, commitWriter ? 1 : 0 );
		}
	}

	private static void requireRace(ConcurrencyConfiguration configuration, ConcurrencyConfiguration.Fixture fixture) {
		final var dialect = fixture.factory.getJdbcServices().getDialect();
		assumeTrue( configuration.knownReadProfile( dialect ), "No independent current-read expectation for this configuration" );
		assumeTrue( !configuration.ordinaryProtection( dialect ), "An ordinary retained read already prevents the competing write" );
	}

	private static SessionImplementor reader(ConcurrencyConfiguration.Fixture fixture, AtomicInteger reads) {
		final var persister = (AbstractEntityPersister) fixture.factory.getMappingMetamodel().getEntityDescriptor( Row.class );
		return (SessionImplementor) fixture.factory.createEntityManager( (StatementObserver) (sql, batch) -> {
			if ( sql.equals( persister.getVersionSelectString() ) ) reads.incrementAndGet();
		} );
	}

	private static void request(ConcurrencyConfiguration.Fixture fixture, SessionImplementor session, boolean managedQuery) {
		session.doWork( fixture::assertConfiguration );
		final Row row;
		if ( managedQuery ) {
			final var managed = session.find( Row.class, 1 );
			row = session.createSelectionQuery( "from ConcurrencyRow where id=1", Row.class )
					.setHibernateLockMode( LockMode.OPTIMISTIC ).getSingleResult();
			assertSame( managed, row );
		}
		else row = session.find( Row.class, 1, LockMode.OPTIMISTIC );
		assertEquals( 0, row.revision );
		assertEquals( 0, session.createSelectionQuery( "select revision from ConcurrencyRow where id=1", Integer.class ).getSingleResult(),
				"Establish the database snapshot even if the entity came from cache" );
		assertTrue( session.getTransactionCompletionCallbacksImplementor().hasBeforeCompletionCallbacks() );
	}

	private static void change(ConcurrencyConfiguration.Fixture fixture, SessionImplementor writer) {
		writer.doWork( connection -> {
			fixture.assertConfiguration( connection );
			try ( var statement = connection.createStatement() ) {
				statement.setQueryTimeout( 30 );
				assertEquals( 1, statement.executeUpdate( "update concurrency_row set revision=1 where id=1" ) );
			}
		} );
	}

	private static void assertDatabase(ConcurrencyConfiguration.Fixture fixture, int expected) {
		try ( var session = fixture.factory.openSession() ) {
			session.beginTransaction();
			try {
				assertEquals( expected, session.createSelectionQuery( "select revision from ConcurrencyRow where id=1", Integer.class ).getSingleResult() );
			}
			finally { session.getTransaction().rollback(); }
		}
	}

	private static void assertOptimisticFailure(Throwable failure) {
		for ( var cause = failure; cause != null; cause = cause.getCause() ) {
			if ( cause instanceof OptimisticEntityLockException || cause instanceof jakarta.persistence.OptimisticLockException ) return;
		}
		throw new AssertionError( "Expected optimistic version conflict", failure );
	}
}
