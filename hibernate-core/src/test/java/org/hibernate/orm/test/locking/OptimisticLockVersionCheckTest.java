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
import org.hibernate.StaleStateException;
import org.hibernate.dialect.lock.spi.LockingStrategyException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

	/**
	 * Two transactions, each reading the other doctor in {@code OPTIMISTIC} mode and
	 * then taking itself off call, both flush before either commits. Without a locking
	 * version check, on a database with multiversion reads both would commit and the
	 * invariant that at least one doctor is on call would be broken (write skew).
	 * Exactly one must commit; the other must be refused with a lock or version
	 * conflict. Any other failure fails the test.
	 */
	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.ReadsDoNotWaitForUncommittedWrites.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	void testWriteSkewPrevented(SessionFactoryScope scope) throws Exception {
		final var barrier = new CyclicBarrier( 2 );
		final ExecutorService executor = Executors.newFixedThreadPool( 2 );
		try {
			final Future<Boolean> alice = executor.submit( () -> goOffCall( scope, ALICE, BOB, barrier ) );
			final Future<Boolean> bob = executor.submit( () -> goOffCall( scope, BOB, ALICE, barrier ) );
			// any failure other than a lock or version conflict at commit propagates and fails the test
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

	private static boolean goOffCall(SessionFactoryScope scope, long self, long colleague, CyclicBarrier barrier)
			throws Exception {
		try ( var session = scope.getSessionFactory().openSession() ) {
			final var transaction = session.beginTransaction();
			try {
				final var other = session.find( Doctor.class, colleague, LockModeType.OPTIMISTIC );
				assertThat( other.onCall ).isTrue();
				session.find( Doctor.class, self ).onCall = false;
				session.flush();
				// wait until both transactions have written, then verify and commit
				barrier.await( 30, TimeUnit.SECONDS );
				try {
					transaction.commit();
					return true;
				}
				catch (RuntimeException e) {
					// the only acceptable reason for not committing is a conflict
					// detected while verifying the version of the colleague
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

	private static boolean isLockConflict(Throwable throwable) {
		for ( var cause = throwable; cause != null; cause = cause.getCause() ) {
			if ( cause instanceof LockAcquisitionException
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
