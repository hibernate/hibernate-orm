/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.Version;
import org.hibernate.Session;
import org.hibernate.StaleStateException;
import org.hibernate.dialect.lock.spi.LockingStrategyException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SnapshotIsolationException;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the version check performed just before commit for an entity
 * locked in {@link LockModeType#OPTIMISTIC} mode.
 *
 * @see org.hibernate.dialect.lock.spi.LockingSupport.Metadata#readsWaitForUncommittedWrites()
 */
@DomainModel(annotatedClasses = OptimisticLockVersionCheckTest.Doctor.class)
@SessionFactory(useCollectingStatementInspector = true)
public class OptimisticLockVersionCheckTest {
	private static final long ALICE = 1L;
	private static final long BOB = 2L;

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Doctor( ALICE, "Alice" ) );
			session.persist( new Doctor( BOB, "Bob" ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testVersionCheckIsCurrentRead(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> session.find( Doctor.class, ALICE, LockModeType.OPTIMISTIC ) );

		// the find, then the version check just before commit
		assertThat( inspector.getSqlQueries() ).hasSize( 2 );
		final String versionCheck = inspector.getSqlQueries().get( 1 );
		assertThat( versionCheck.toLowerCase() ).startsWith( "select" );

		final var lockingSupport = scope.getSessionFactory().getJdbcServices().getDialect().getLockingSupport();
		final String hint = lockingSupport.renderCurrentReadTableHint( "Doctor" );
		final String clause = lockingSupport.renderCurrentReadClause();
		if ( lockingSupport.getMetadata().readsWaitForUncommittedWrites() ) {
			// a plain read already waits for a concurrent writer, so nothing extra may be rendered
			assertThat( hint ).isEmpty();
			assertThat( clause ).isEmpty();
			assertThat( versionCheck.toLowerCase() )
					.doesNotContain( "for update", "for share", "share mode", "holdlock", "readcommittedlock",
							"keep share", "keep update", "wait for outcome" );
		}
		else {
			if ( lockingSupport.getMetadata().getPessimisticLockStyle() != PessimisticLockStyle.NONE ) {
				assertThat( hint + clause ).isNotEmpty();
			}
			if ( !hint.isEmpty() ) {
				assertThat( versionCheck ).contains( "Doctor" + hint );
			}
			if ( !clause.isEmpty() ) {
				assertThat( versionCheck ).endsWith( clause );
			}
		}
	}

	@ParameterizedTest(name = "queryManagedEntity = {0}")
	@ValueSource(booleans = { false, true })
	void testUncontendedUpdateCommits(boolean queryManagedEntity, SessionFactoryScope scope) throws Exception {
		final var versions = scope.fromTransaction( session -> new int[] {
				session.find( Doctor.class, ALICE ).version,
				session.find( Doctor.class, BOB ).version
		} );
		// Exercise the same flush and commit path without another transaction to conflict with.
		assertThat( goOffCall( scope, ALICE, BOB, new CyclicBarrier( 1 ), queryManagedEntity, true ) ).isTrue();
		scope.inTransaction( session -> {
			final var alice = session.find( Doctor.class, ALICE );
			final var bob = session.find( Doctor.class, BOB );
			assertThat( alice.onCall ).isFalse();
			assertThat( alice.version ).isEqualTo( versions[0] + 1 );
			assertThat( bob.onCall ).isTrue();
			assertThat( bob.version ).isEqualTo( versions[1] );
		} );
	}

	/**
	 * Both transactions finish reading before either writes. This also covers databases
	 * where reads wait for writers, including those which serialize writes to a table.
	 */
	@ParameterizedTest(name = "queryManagedEntity = {0}")
	@ValueSource(booleans = { false, true })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	void testWriteSkewPrevented(boolean queryManagedEntity, SessionFactoryScope scope) throws Exception {
		assertWriteSkewPrevented( scope, queryManagedEntity, false );
	}

	/**
	 * Two transactions, each reading the other doctor in {@code OPTIMISTIC} mode and
	 * then taking itself off call, both flush before either commits. Without a locking
	 * version check, on a database with multiversion reads both would commit and the
	 * invariant that at least one doctor is on call would be broken (write skew).
	 * Exactly one must commit; the other must be refused with a lock or version
	 * conflict. Any other failure fails the test.
	 */
	@ParameterizedTest(name = "queryManagedEntity = {0}")
	@ValueSource(booleans = { false, true })
	@RequiresDialectFeature(feature = DialectFeatureChecks.ReadsDoNotWaitForUncommittedWrites.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	void testWriteSkewPreventedAfterBothFlush(boolean queryManagedEntity, SessionFactoryScope scope) throws Exception {
		assertWriteSkewPrevented( scope, queryManagedEntity, true );
	}

	private static void assertWriteSkewPrevented(
			SessionFactoryScope scope, boolean queryManagedEntity, boolean waitForBothWrites) throws Exception {
		final var barrier = new CyclicBarrier( 2 );
		final ExecutorService executor = Executors.newFixedThreadPool( 2 );
		try {
			final Future<Boolean> alice = executor.submit(
					() -> goOffCall( scope, ALICE, BOB, barrier, queryManagedEntity, waitForBothWrites ) );
			final Future<Boolean> bob = executor.submit(
					() -> goOffCall( scope, BOB, ALICE, barrier, queryManagedEntity, waitForBothWrites ) );
			// any failure other than a lock or version conflict propagates and fails the test
			final boolean aliceCommitted = alice.get( 2, TimeUnit.MINUTES );
			final boolean bobCommitted = bob.get( 2, TimeUnit.MINUTES );
			assertThat( aliceCommitted ^ bobCommitted ).as( "exactly one transaction committed" ).isTrue();
		}
		finally {
			executor.shutdownNow();
		}
		scope.inTransaction( session -> {
			final var onCall = session.createSelectionQuery( "from Doctor where onCall", Doctor.class ).getResultList();
			assertThat( onCall ).as( "doctors still on call" ).isNotEmpty();
		} );
	}

	private static boolean goOffCall(
			SessionFactoryScope scope, long self, long colleague, CyclicBarrier barrier,
			boolean queryManagedEntity, boolean waitForBothWrites) throws Exception {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final var other = findOptimistically( session, colleague, queryManagedEntity );
				assertThat( other.onCall ).isTrue();
				session.find( Doctor.class, self ).onCall = false;
				// Neither transaction may write while the other is still reading.
				barrier.await( 30, TimeUnit.SECONDS );
				try {
					session.flush();
					if ( waitForBothWrites ) {
						// Force both validations to encounter an uncommitted write.
						barrier.await( 30, TimeUnit.SECONDS );
					}
					transaction.commit();
					return true;
				}
				catch (RuntimeException e) {
					// A conflict may be detected when writing or when verifying the colleague's version.
					if ( isLockConflict( e ) ) {
						return false;
					}
					throw e;
				}
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
	}

	/**
	 * Read Alice, commit an update to both doctors in another transaction, then read Bob.
	 * A transaction observing different on-call values must not commit. A database which
	 * supplies a consistent snapshot may instead return both original values.
	 */
	@ParameterizedTest(name = "queryManagedEntity = {0}")
	@ValueSource(booleans = { false, true })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	void testReadSkewPrevented(boolean queryManagedEntity, SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var transaction = session.beginTransaction();
			try {
				final boolean aliceOnCall = findOptimistically( session, ALICE, queryManagedEntity ).onCall;
				assertThat( aliceOnCall ).isTrue();
				scope.inTransaction( writer -> {
					writer.find( Doctor.class, ALICE ).onCall = false;
					writer.find( Doctor.class, BOB ).onCall = false;
				} );
				try {
					final boolean bobOnCall = findOptimistically( session, BOB, queryManagedEntity ).onCall;
					transaction.commit();
					assertThat( bobOnCall ).as( "a committed transaction must not observe read skew" )
							.isEqualTo( aliceOnCall );
				}
				catch (RuntimeException e) {
					if ( !isLockConflict( e ) ) {
						throw e;
					}
				}
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		} );
	}

	private static Doctor findOptimistically(Session session, long id, boolean queryManagedEntity) {
		if ( !queryManagedEntity ) {
			return session.find( Doctor.class, id, LockModeType.OPTIMISTIC );
		}
		final var managed = session.find( Doctor.class, id );
		final var result = session.createSelectionQuery( "from Doctor where id = :id", Doctor.class )
				.setParameter( "id", id )
				.setLockMode( LockModeType.OPTIMISTIC )
				.getSingleResult();
		assertThat( result ).isSameAs( managed );
		return result;
	}

	private static boolean isLockConflict(Throwable throwable) {
		for ( var cause = throwable; cause != null; cause = cause.getCause() ) {
			if ( cause instanceof LockAcquisitionException
					|| cause instanceof SnapshotIsolationException
					|| cause instanceof org.hibernate.exception.LockTimeoutException
					|| cause instanceof LockingStrategyException
					|| cause instanceof StaleStateException
					|| cause instanceof PessimisticLockException
					|| cause instanceof LockTimeoutException
					|| cause instanceof OptimisticLockException ) {
				return true;
			}
		}
		return false;
	}

	@Entity(name = "Doctor")
	public static class Doctor {
		@Id
		Long id;
		@Version
		int version;
		String name;
		boolean onCall = true;

		public Doctor() {
		}

		public Doctor(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
