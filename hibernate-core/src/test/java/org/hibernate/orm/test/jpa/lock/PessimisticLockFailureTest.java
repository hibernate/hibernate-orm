/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.Version;
import org.hibernate.Session;
import org.hibernate.Timeouts;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies, against the database itself, that the exception thrown for a pessimistic lock
 * failure agrees with what the database did to the transaction. Per JPA, a
 * {@link LockTimeoutException} means the transaction survived the failure, so the test keeps
 * using it and commits, requiring a write flushed before the failure to have been kept. A
 * {@link PessimisticLockException} means the database rolled the transaction back, so the test
 * forces a commit past Hibernate through JDBC and requires that earlier write to have been lost.
 * A misclassification by {@link org.hibernate.dialect.Dialect#causesRollback} therefore shows
 * up, in either direction, as a difference in what the database retained.
 */
@Jpa(annotatedClasses = { PessimisticLockFailureTest.Account.class, PessimisticLockFailureTest.Ledger.class })
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
class PessimisticLockFailureTest {
	private static final String ORIGINAL = "original";
	private static final String BEFORE_FAILURE = "written before the failure";
	private static final String AFTER_FAILURE = "written after the failure";

	@BeforeEach
	void createTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new Account( 1 ) );
			em.persist( new Account( 2 ) );
			em.persist( new Ledger( 1 ) );
			em.persist( new Ledger( 2 ) );
		} );
	}

	@AfterEach
	void dropTestData(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	/**
	 * Another transaction holds the lock for longer than the requested timeout.
	 */
	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsLockTimeouts.class)
	void testLockTimeout(EntityManagerFactoryScope scope) throws Exception {
		final var locked = new CountDownLatch( 1 );
		final var release = new CountDownLatch( 1 );
		final var holder = new Thread( () -> scope.inTransaction( em -> {
			em.find( Account.class, 1, LockModeType.PESSIMISTIC_WRITE );
			locked.countDown();
			try {
				// hold the lock until the other transaction has given up, but not forever
				release.await( 1, TimeUnit.MINUTES );
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		} ), "lock holder" );
		holder.setDaemon( true );
		try ( var em = scope.getEntityManagerFactory().createEntityManager() ) {
			final var transaction = em.getTransaction();
			transaction.begin();
			try {
				em.find( Ledger.class, 1 ).setName( BEFORE_FAILURE );
				em.flush();
				// read the row before it is locked, since on some databases a plain read waits for a writer
				final var account = em.find( Account.class, 1 );
				holder.start();
				assertTrue( locked.await( 1, TimeUnit.MINUTES ), "the lock holder did not obtain the lock" );
				final PersistenceException failure;
				try {
					failure = assertThrows( PersistenceException.class,
							() -> em.lock( account, LockModeType.PESSIMISTIC_WRITE, Timeouts.ONE_SECOND ) );
				}
				finally {
					release.countDown();
				}
				holder.join( TimeUnit.MINUTES.toMillis( 1 ) );
				assertConsistentWithDatabase( scope, em, transaction, failure, Ledger.class, 1, 2 );
			}
			finally {
				release.countDown();
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
	}

	/**
	 * Two transactions each lock a row in a different table, then each asks for the other's row.
	 * The tables differ so that the pattern also deadlocks on databases which lock whole tables.
	 */
	@Test
	void testDeadlock(EntityManagerFactoryScope scope) throws Exception {
		final var barrier = new CyclicBarrier( 2 );
		final ExecutorService executor = Executors.newFixedThreadPool( 2, runnable -> {
			final var thread = new Thread( runnable, "deadlock participant" );
			thread.setDaemon( true );
			return thread;
		} );
		try {
			final Future<Boolean> first = executor.submit( () -> lockBoth( scope, barrier, Account.class, Ledger.class ) );
			final Future<Boolean> second = executor.submit( () -> lockBoth( scope, barrier, Ledger.class, Account.class ) );
			final boolean firstFailed = first.get( 2, TimeUnit.MINUTES );
			final boolean secondFailed = second.get( 2, TimeUnit.MINUTES );
			assertTrue( firstFailed || secondFailed, "neither transaction failed to obtain its second lock" );
		}
		finally {
			executor.shutdownNow();
		}
	}

	/**
	 * Locks and writes row 1 of {@code own}, then requests row 1 of {@code other}.
	 *
	 * @return whether the second lock request failed
	 */
	private static boolean lockBoth(
			EntityManagerFactoryScope scope, CyclicBarrier barrier,
			Class<? extends Row> own, Class<? extends Row> other) throws Exception {
		try ( var em = scope.getEntityManagerFactory().createEntityManager() ) {
			final var transaction = em.getTransaction();
			transaction.begin();
			try {
				em.find( own, 1, LockModeType.PESSIMISTIC_WRITE ).setName( BEFORE_FAILURE );
				em.flush();
				// both transactions hold their first lock before either requests its second
				barrier.await( 1, TimeUnit.MINUTES );
				PersistenceException failure = null;
				try {
					em.find( other, 1, LockModeType.PESSIMISTIC_WRITE );
				}
				catch (PersistenceException e) {
					failure = e;
				}
				if ( failure == null ) {
					transaction.commit();
					scope.inTransaction( check -> assertEquals( BEFORE_FAILURE, check.find( own, 1 ).getName() ) );
					return false;
				}
				else {
					assertConsistentWithDatabase( scope, em, transaction, failure, own, 1, 2 );
					return true;
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
	 * @param earlier the id of the row of {@code type} written and flushed before the failure
	 * @param later the id of a row of {@code type} not yet written, used to force a commit
	 */
	private static void assertConsistentWithDatabase(
			EntityManagerFactoryScope scope, EntityManager em, EntityTransaction transaction,
			PersistenceException failure, Class<? extends Row> type, int earlier, int later) {
		if ( failure instanceof LockTimeoutException ) {
			// the database is supposed to have kept the transaction, so keep using it
			assertFalse( transaction.getRollbackOnly(),
					"a LockTimeoutException must not mark the transaction for rollback" );
			em.find( type, later ).setName( AFTER_FAILURE );
			transaction.commit();
			scope.inTransaction( check -> {
				assertEquals( BEFORE_FAILURE, check.find( type, earlier ).getName(),
						"LockTimeoutException was thrown, but the database had rolled the transaction back" );
				assertEquals( AFTER_FAILURE, check.find( type, later ).getName() );
			} );
		}
		else if ( failure instanceof PessimisticLockException ) {
			// the database is supposed to have rolled the transaction back: commit past Hibernate
			// through JDBC, which would also commit the earlier write if the transaction survived
			assertTrue( transaction.getRollbackOnly(),
					"a PessimisticLockException must mark the transaction for rollback" );
			em.unwrap( Session.class ).doWork( connection -> {
				final String table = type.getSimpleName();
				try ( var statement = connection.prepareStatement( "update " + table + " set name = ? where id = ?" ) ) {
					statement.setString( 1, AFTER_FAILURE );
					statement.setInt( 2, later );
					statement.executeUpdate();
					connection.commit();
				}
				catch (SQLException e) {
					// some databases, such as PostgreSQL, refuse further statements in an aborted transaction
				}
			} );
			transaction.rollback();
			scope.inTransaction( check -> assertEquals( ORIGINAL, check.find( type, earlier ).getName(),
					"PessimisticLockException was thrown, but the database had kept the transaction" ) );
		}
		else {
			fail( "unexpected exception for a pessimistic lock failure: " + failure );
		}
	}

	@MappedSuperclass
	public abstract static class Row {
		@Id
		Integer id;
		String name = ORIGINAL;
		@Version
		int version;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Account")
	public static class Account extends Row {
		public Account() {
		}

		public Account(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Ledger")
	public static class Ledger extends Row {
		public Ledger() {
		}

		public Ledger(Integer id) {
			this.id = id;
		}
	}
}
