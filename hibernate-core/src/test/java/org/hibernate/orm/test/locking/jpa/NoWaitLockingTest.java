/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.Timeout;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				NoWaitLockingTest.Foo.class
		}
)
public class NoWaitLockingTest {

	@Entity
	public static class Foo {
		@Id
		public int id;
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(OracleDialect.class)
	@JiraKey( "HHH-19898" )
	void hhh19898TestLock(EntityManagerFactoryScope scope) throws Exception {
		scope.inTransaction( em -> {
			Foo foo = new Foo();
			foo.id = 1;
			em.persist( foo );
		});

		final CountDownLatch lAcquireLock = new CountDownLatch( 1 );
		final CountDownLatch lPastNowait = new CountDownLatch( 1 );

		final Thread t = new Thread(() -> {
			scope.inTransaction( em -> {
				Foo foo = em.find( Foo.class, 1, LockModeType.PESSIMISTIC_WRITE );
				assertEquals( LockModeType.PESSIMISTIC_WRITE, em.getLockMode( foo ) );
				assertNotNull( foo );

				// We have the lock!
				lAcquireLock.countDown();

				// Now wait until we are done with NOWAIT
				awaitLatch( lPastNowait );

				// Now we are done, can commit, and free the lock
			} );
		});

		// Start the thread that locks the entity and waits until we have executed the first LOCK NOWAIT
		t.start();

		scope.inTransaction( em -> {
			// Wait until the thread has the lock
			awaitLatch( lAcquireLock );

			Foo foo = em.find( Foo.class, 1 );

			// Sanity check
			assertTrue( em.getTransaction().isActive() );

			// Do the nowait!
			assertThrows( LockTimeoutException.class, () -> em.lock( foo, LockModeType.PESSIMISTIC_WRITE, Timeout.ms( 0 )) );
			assertEquals( LockModeType.NONE, em.getLockMode( foo ) );

			lPastNowait.countDown();
			joinThread( t );

			// Only the statement should be rolled back, not the transaction!
			assertTrue( em.getTransaction().isActive() );

			// Try persisting a new entity to check that the transaction is stil alive
			Foo foo2 = new Foo();
			foo2.id = 42;
			em.persist( foo2 );
			em.flush(); // Force the insert

			// Now try locking again!
			em.lock( foo, LockModeType.PESSIMISTIC_WRITE, Timeout.ms( 0 ));
			assertEquals( LockModeType.PESSIMISTIC_WRITE, em.getLockMode( foo ) );
		} );
	}

	// The same test again, but now with refresh instead of lock
	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(OracleDialect.class)
	@JiraKey( "HHH-19898" )
	void hhh19898TestRefresh(EntityManagerFactoryScope scope) throws Exception {
		scope.inTransaction( em -> {
			Foo foo = new Foo();
			foo.id = 2;
			em.persist( foo );
		});

		final CountDownLatch lAcquireLock = new CountDownLatch( 1 );
		final CountDownLatch lPastNowait = new CountDownLatch( 1 );

		final Thread t = new Thread(() -> {
			scope.inTransaction( em -> {
				Foo foo = em.find( Foo.class, 2, LockModeType.PESSIMISTIC_WRITE );
				assertEquals( LockModeType.PESSIMISTIC_WRITE, em.getLockMode( foo ) );
				assertNotNull( foo );

				// We have the lock!
				lAcquireLock.countDown();

				// Now wait until we are done with NOWAIT
				awaitLatch( lPastNowait );

				// Now we are done, can commit, and free the lock
			} );
		});

		// Start the thread that locks the entity and waits until we have executed the first LOCK NOWAIT
		t.start();

		scope.inTransaction( em -> {
			// Wait until the thread has the lock
			awaitLatch( lAcquireLock );

			Foo foo = em.find( Foo.class, 2 );

			// Sanity check
			assertTrue( em.getTransaction().isActive() );

			// Do the nowait!
			assertThrows( LockTimeoutException.class, () -> em.refresh( foo, LockModeType.PESSIMISTIC_WRITE, Timeout.ms( 0 )) );
			assertEquals( LockModeType.NONE, em.getLockMode( foo ) );

			lPastNowait.countDown();
			joinThread( t );

			// Only the statement should be rolled back, not the transaction!
			assertTrue( em.getTransaction().isActive() );

			// Try persisting a new entity to check that the transaction is stil alive
			Foo foo2 = new Foo();
			foo2.id = 21;
			em.persist( foo2 );
			em.flush(); // Force the insert

			// Now try locking again!
			em.refresh( foo, LockModeType.PESSIMISTIC_WRITE, Timeout.ms( 0 ));
			assertEquals( LockModeType.PESSIMISTIC_WRITE, em.getLockMode( foo ) );
		} );
	}

	private static void awaitLatch(CountDownLatch l) {
		try {
			if(!l.await(1000, TimeUnit.MILLISECONDS)) {
				throw new RuntimeException("TIMEOUT!");
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException( e );
		}
	}

	private static void joinThread(Thread t) {
		try {
			t.join(1000);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException( e );
		}
	}
}
