package org.hibernate.orm.test.stateless;

import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.StatelessSession;
import org.hibernate.dialect.lock.spi.Operation;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.orm.test.locking.ConcurrencyConfiguration;
import org.hibernate.orm.test.locking.ConcurrencyConfiguration.Row;
import org.hibernate.orm.test.locking.ConcurrencyTestSupport;
import org.hibernate.testing.orm.TransactionConcurrencyChecks;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

/// Stateless optimistic protection must survive result consumption, resource
/// closure, and temporary-context cleanup until its transaction completes.
///
/// @author Steve Ebersole
@JiraKey("HHH-21035")
@Tag("db-locking")
class StatelessOptimisticProtectionTest {
	// Deliberately supplied configuration: freshness alone must never be used
	// as proof of retained protection, even on a database capable of locking.
	private static final TransactionConcurrency CURRENT_ONLY = TransactionConcurrencies.builder( "current-only-test" )
			.read( Operation.READ, new TransactionConcurrencies.Guarantees( true, false, false, false, true ) )
			.read( Operation.CURRENT_READ, new TransactionConcurrencies.Guarantees( true, false, false, false, true ) ).build();

	enum Read { FIND, REFRESH, MULTIPLE, LIST, STREAM, EARLY_STREAM, LEGACY_ALIAS }

	static Stream<Arguments> protections() {
		return ConcurrencyConfiguration.configurations().flatMap( configuration ->
				Stream.of( LockModeType.OPTIMISTIC, LockModeType.OPTIMISTIC_FORCE_INCREMENT ).flatMap( mode ->
						Stream.of( Read.values() ).flatMap( read ->
								Stream.of( true, false ).flatMap( commit ->
										(read == Read.MULTIPLE || read == Read.LIST || read == Read.STREAM || read == Read.LEGACY_ALIAS
												? Stream.of( 1, 2 ) : Stream.of( 1 ))
										.map( id -> Arguments.of( configuration, mode, read, commit, id ) ) ) ) ) );
	}

	static Stream<Arguments> rejections() {
		// All public request routes are checked without multiplying a simulated
		// descriptor by snapshot variants that cannot influence its declared facts.
		return Stream.of( Read.values() ).map( read -> Arguments.of( read ) );
	}

	@ParameterizedTest
	@MethodSource("protections")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	void protectsExposedRowsUntilCompletion(
			ConcurrencyConfiguration configuration, LockModeType mode, Read read, boolean commit, int probeId) throws Exception {
		try ( var fixture = configuration.open() ) {
			// Warm the cache and require this to be a real cache fixture.
			try ( var warm = fixture.factory.openSession() ) {
				warm.beginTransaction();
				assertEquals( 0, warm.find( Row.class, 1 ).revision );
				assertEquals( 0, warm.find( Row.class, 2 ).revision );
				warm.getTransaction().commit();
			}
			assertTrue( fixture.factory.getCache().containsEntity( Row.class, probeId ) );
			try ( var session = fixture.factory.openStatelessSession() ) {
				final var transaction = session.beginTransaction();
				try {
					session.doWork( fixture::assertConfiguration );
					final var concurrency = fixture.factory.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
					final boolean supported = TransactionConcurrencyChecks.supportsStatelessOptimisticLocking( concurrency );
					if ( mode == LockModeType.OPTIMISTIC && !supported ) {
						assertFalse( configuration.knownReadProfile( fixture.factory.getJdbcServices().getDialect() ),
								"Required profile unexpectedly lost retained protection: " + configuration );
						assertThrows( HibernateException.class, () -> read( session, read, mode ) );
						return;
					}
					final var rows = read( session, read, mode );
					assertTrue( rows.stream().anyMatch( row -> row.id == probeId ) );
					final int exposedRevision = mode == LockModeType.OPTIMISTIC ? 0 : 1;
					rows.forEach( row -> assertEquals( exposedRevision, row.revision, "Increment must precede exposure" ) );
					ConcurrencyTestSupport.assertWriteProtected( fixture.factory, "concurrency_row", probeId, () -> {
						if ( commit ) transaction.commit(); else transaction.rollback();
					} );
					assertStored( fixture, probeId, commit ? exposedRevision : 0 );
					// No deferred increment and no replay in the next healthy transaction.
					session.beginTransaction().commit();
					assertStored( fixture, probeId, commit ? exposedRevision : 0 );
				}
				finally { if ( transaction.isActive() ) transaction.rollback(); }
			}
		}
	}

	@ParameterizedTest
	@MethodSource("rejections")
	void freshnessWithoutRetentionIsRejectedBeforeSql(Read read) throws Exception {
		final var configuration = ConcurrencyConfiguration.configurations().findFirst().orElseThrow();
		try ( var fixture = configuration.open( CURRENT_ONLY ) ) {
			final var statements = new java.util.ArrayList<String>();
			try ( var session = fixture.factory.createEntityAgent(
					(org.hibernate.StatementObserver) (sql, batch) -> statements.add( sql ) ) ) {
				final var transaction = session.beginTransaction();
				try {
					final var failure = assertThrows( HibernateException.class, () -> read( session, read, LockModeType.OPTIMISTIC ) );
					assertTrue( failure.getMessage().contains( "transaction-long row protection" ) );
					assertTrue( statements.isEmpty(), "Unsupported request executed SQL: " + statements );
				}
				finally { if ( transaction.isActive() ) transaction.rollback(); }
			}
		}
	}

	@SuppressWarnings("removal")
	private static List<Row> read(StatelessSession session, Read read, LockModeType mode) {
		return switch ( read ) {
			case FIND -> List.of( session.find( Row.class, 1, mode ) );
			case REFRESH -> {
				final var row = new Row( 1 );
				row.revision = -1;
				session.refresh( row, mode );
				yield List.of( row );
			}
			case MULTIPLE -> session.findMultiple( Row.class, List.of( 1, 2 ), mode );
			case LIST, LEGACY_ALIAS, STREAM, EARLY_STREAM -> {
				final var query = session.createSelectionQuery( "from ConcurrencyRow r order by r.id", Row.class );
				if ( read == Read.LEGACY_ALIAS ) {
					// The deprecated alias setter now promotes the global mode.
					((org.hibernate.query.internal.SelectionQueryImpl<Row>) query).getQueryOptions().getLockOptions()
							.setAliasSpecificLockMode( "r", LockMode.fromJpaLockMode( mode ) );
				}
				else query.setLockMode( mode );
				final List<Row> result;
				if ( read == Read.STREAM || read == Read.EARLY_STREAM ) {
					try ( var stream = query.getResultStream() ) {
						result = read == Read.EARLY_STREAM ? List.of( stream.findFirst().orElseThrow() ) : stream.toList();
					}
				}
				else result = query.getResultList();
				assertEquals( mode, query.getLockMode(), "Execution must not mutate caller-visible locking options" );
				yield result;
			}
		};
	}

	private static void assertStored(ConcurrencyConfiguration.Fixture fixture, int id, int revision) {
		try ( var session = fixture.factory.openSession() ) {
			session.beginTransaction();
			try {
				assertEquals( revision, session.find( Row.class, id ).revision, "Cache-visible revision" );
				assertEquals( revision, session.createSelectionQuery( "select revision from ConcurrencyRow where id=:id", Integer.class )
						.setParameter( "id", id ).getSingleResult(), "Database revision" );
			}
			finally { session.getTransaction().rollback(); }
		}
	}
}
