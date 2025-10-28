/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.jboss.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that we can upgrade locks
 *
 * @author Scott Marlow
 */
@Jpa(annotatedClasses = {
		Lock.class
})
public class UpgradeLockTest {

	protected final Logger log = Logger.getLogger( getClass() );

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	/**
	 * Initially in tx1, get a LockModeType.READ and upgrade to LockModeType.OPTIMISTIC_FORCE_INCREMENT.
	 * To prove success, tx2, will modify the entity which should cause a failure in tx1.
	 */
	@Test
	public void testUpgradeReadLockToOptimisticForceIncrement(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				em -> {
					final EntityManager em2 = scope.getEntityManagerFactory().createEntityManager();
					try {
						Lock lock = new Lock();  //
						lock.setName( "name" );

						em.getTransaction().begin(); // create the test entity first
						em.persist( lock );

						em.getTransaction().commit();

						em.getTransaction().begin(); // start tx1
						lock = em.getReference( Lock.class, lock.getId() );
						final Integer id = lock.getId();

						em.lock( lock, LockModeType.READ );  // start with READ lock in tx1
						// upgrade to OPTIMISTIC_FORCE_INCREMENT in tx1
						em.lock( lock, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
						lock.setName( "surname" );           // don't end tx1 yet

						final CountDownLatch latch = new CountDownLatch( 1 );
						Thread t = new Thread( () -> {
							try {
								em2.getTransaction().begin();  // start tx2
								Lock lock2 = em2.getReference( Lock.class, id );
								lock2.setName( "renamed" );       // change entity
							}
							finally {
								em2.getTransaction().commit();
								em2.close();
								latch.countDown();    // signal that tx2 is committed
							}
						} );

						t.setDaemon( true );
						t.setName( "testUpgradeReadLockToOptimisticForceIncrement tx2" );
						t.start();
						log.info( "testUpgradeReadLockToOptimisticForceIncrement:  wait on BG thread" );
						boolean latchSet = latch.await( 10, TimeUnit.SECONDS );
						assertTrue( latchSet, "background test thread finished (lock timeout is broken)" );
						// tx2 is complete, try to commit tx1
						try {
							em.getTransaction().commit();
						}
						catch (Throwable expectedToFail) {
							while ( expectedToFail != null &&
									!( expectedToFail instanceof jakarta.persistence.OptimisticLockException ) ) {
								expectedToFail = expectedToFail.getCause();
							}
							assertTrue(
									expectedToFail instanceof jakarta.persistence.OptimisticLockException,
									"upgrade to OPTIMISTIC_FORCE_INCREMENT is expected to fail at end of transaction1 since transaction2 already updated the entity"
							);
						}
					}
					catch (Exception e) {
						if ( em.getTransaction().isActive() ) {
							em.getTransaction().rollback();
						}
						if ( em2.getTransaction().isActive() ) {
							em2.getTransaction().rollback();
						}
						throw new RuntimeException(e);
					}
				}
		);
	}
}
