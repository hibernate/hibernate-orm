/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.lock;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.QueryHints;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.*;
import org.jboss.logging.Logger;
import org.junit.Test;

import javax.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author Emmanuel Bernard
 */
public class LockTest extends BaseEntityManagerFunctionalTestCase {
	private static final Logger log = Logger.getLogger( LockTest.class );



	@Test
	public void testFindWithTimeoutHint() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lock lock = new Lock();
		lock.setName( "name" );
		em.persist( lock );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( AvailableSettings.LOCK_TIMEOUT, 0L );
		em.find( Lock.class, 1, LockModeType.PESSIMISTIC_WRITE, properties );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		lock = em.find( Lock.class, lock.getId() );
		em.remove( lock );
		em.getTransaction().commit();
		em.close();
	}
	
	@Test(timeout = 5 * 60 * 1000) //5 minutes
	@TestForIssue( jiraKey = "HHH-7252" )
	@RequiresDialectFeature( value = DialectChecks.SupportsLockTimeouts.class, 
		                    comment = "Test verifies proper exception throwing when a lock timeout is specified.",
                              jiraKey = "HHH-7252" )
	public void testFindWithPessimisticWriteLockTimeoutException() {
		Lock lock = new Lock();
		lock.setName( "name" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( lock );
		em.getTransaction().commit();
		em.close();

		EntityManager em2 = createIsolatedEntityManager();
		em2.getTransaction().begin();
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( AvailableSettings.LOCK_TIMEOUT, 0L );
		Lock lock2 = em2.find( Lock.class, lock.getId(), LockModeType.PESSIMISTIC_WRITE, properties );
		assertEquals( "lock mode should be PESSIMISTIC_WRITE ", LockModeType.PESSIMISTIC_WRITE, em2.getLockMode( lock2 ) );
		
		EntityManager em3 = createIsolatedEntityManager();
		em3.getTransaction().begin();
		try {
			em3.find( Lock.class, lock.getId(), LockModeType.PESSIMISTIC_WRITE, properties );
			fail( "Exception should be thrown" );
		}
		catch (LockTimeoutException lte) {
			// Proper exception thrown for dialect supporting lock timeouts when an immediate timeout is set.
		}
		catch (PessimisticLockException pe) {
			fail( "Find with immediate timeout should have thrown LockTimeoutException." );
		}
		catch (PersistenceException pe) {
			log.info("EntityManager.find() for PESSIMISTIC_WRITE with timeout of 0 threw a PersistenceException.\n" +
				      "This is likely a consequence of " + getDialect().getClass().getName() + " not properly mapping SQL errors into the correct HibernateException subtypes.\n" +
				      "See HHH-7251 for an example of one such situation.", pe);
			fail( "EntityManager should be throwing LockTimeoutException." );
		}
		finally {
			if (em3.getTransaction().getRollbackOnly()) {
				em3.getTransaction().rollback();
			}
			else {
				em3.getTransaction().commit();
			}
			em3.close();
		}
		
		em2.getTransaction().commit();
		em2.getTransaction().begin();
		em2.remove( lock2 );
		em2.getTransaction().commit();
		em2.close();
	}
	
	@Test
	public void testLockRead() throws Exception {
		Lock lock = new Lock();
		lock.setName( "name" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( lock );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.getReference( Lock.class, lock.getId() );
		em.lock( lock, LockModeType.READ );
		lock.setName( "surname" );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.find( Lock.class, lock.getId() );
		assertEquals( "surname", lock.getName() );
		em.remove( lock );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testLockOptimistic() throws Exception {
		Lock lock = new Lock();
		lock.setName( "name" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( lock );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.getReference( Lock.class, lock.getId() );
		em.lock( lock, LockModeType.OPTIMISTIC );
		lock.setName( "surname" );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.find( Lock.class, lock.getId() );
		assertEquals( "surname", lock.getName() );
		em.remove( lock );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testLockWrite() throws Exception {
		Lock lock = new Lock();
		lock.setName( "second" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( lock );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.getReference( Lock.class, lock.getId() );
		Integer version = lock.getVersion();
		em.lock( lock, LockModeType.WRITE );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.getReference( Lock.class, lock.getId() );
		try {
			assertEquals( "should increase the version number EJB-106", 1, lock.getVersion() - version );
		}
		finally {
			em.remove( lock );
			em.getTransaction().commit();
		}
		em.close();
	}

	@Test
	public void testLockWriteOnUnversioned() throws Exception {
		UnversionedLock lock = new UnversionedLock();
		lock.setName( "second" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( lock );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.getReference( UnversionedLock.class, lock.getId() );
		try {
			// getting a READ (optimistic) lock on unversioned entity is not expected to work.
			// To get the same functionality as prior release, change the  LockModeType.READ lock to:
			// em.lock(lock,LockModeType.PESSIMISTIC_READ);
			em.lock( lock, LockModeType.READ );
			fail( "expected OptimisticLockException exception" );
		}
		catch ( OptimisticLockException expected ) {
		}
		em.getTransaction().rollback();

		// the previous code block can be rewritten as follows (to get the previous behavior)
		em.getTransaction().begin();
		lock = em.getReference( UnversionedLock.class, lock.getId() );
		em.lock( lock, LockModeType.PESSIMISTIC_READ );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.getReference( UnversionedLock.class, lock.getId() );
		em.remove( lock );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testLockPessimisticForceIncrement() throws Exception {
		Lock lock = new Lock();
		lock.setName( "force" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( lock );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.getReference( Lock.class, lock.getId() );
		Integer version = lock.getVersion();
		em.lock( lock, LockModeType.PESSIMISTIC_FORCE_INCREMENT );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.getReference( Lock.class, lock.getId() );
		try {
			assertEquals( "should increase the version number ", 1, lock.getVersion() - version );
		}
		finally {
			em.remove( lock );
			em.getTransaction().commit();
		}
		em.close();
	}

	@Test
	public void testLockOptimisticForceIncrement() throws Exception {
		Lock lock = new Lock();
		lock.setName( "force" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( lock );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.getReference( Lock.class, lock.getId() );
		Integer version = lock.getVersion();
		em.lock( lock, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
		em.getTransaction().commit();

		em.getTransaction().begin();
		lock = em.getReference( Lock.class, lock.getId() );
		try {
			assertEquals( "should increase the version number ", 1, lock.getVersion() - version );
		}
		finally {
			em.remove( lock );
			em.getTransaction().commit();
		}
		em.close();
	}

	@Test
	public void testLockOptimisticForceIncrementDifferentEm() throws Exception {
		Lock lock = new Lock();
		lock.setName( "force" );
		EntityManager em1 = createIsolatedEntityManager();
		em1.getTransaction().begin();
		em1.persist( lock );
		em1.getTransaction().commit();
		em1.close();

		EntityManager em2 = createIsolatedEntityManager();
		em2.getTransaction().begin();
		lock = em2.find( Lock.class, lock.getId(), LockModeType.OPTIMISTIC );
		assertEquals( "lock mode should be OPTIMISTIC ", LockModeType.OPTIMISTIC, em2.getLockMode( lock ) );
		em2.lock( lock, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
		assertEquals(
				"lock mode should be OPTIMISTIC_FORCE_INCREMENT ",
				LockModeType.OPTIMISTIC_FORCE_INCREMENT,
				em2.getLockMode( lock )
		);
		em2.getTransaction().commit();
		em2.getTransaction().begin();
		em2.remove( lock );
		em2.getTransaction().commit();
		em2.close();
	}

	@Test
	
	@SkipForDialects({
			@SkipForDialect(HSQLDialect.class),
			// ASE15.5 will generate select...holdlock and fail at this test, but ASE15.7 passes it. Skip it for ASE15.5
			// only.
			@SkipForDialect(value = { SybaseASE15Dialect.class }, strictMatching = true, jiraKey = "HHH-6820"),
			// TODO Remove once HHH-8001 is fixed.
			@SkipForDialect(value = { Oracle8iDialect.class }, jiraKey = "HHH-8001") })
	public void testContendedPessimisticLock() throws Exception {
		final EntityManager em = getOrCreateEntityManager();
		final EntityManager isolatedEntityManager = createIsolatedEntityManager();

		Lock lock = createAndPersistLockInstance( em );

		try {
			inFirstTransactionReloadAndModifyLockInstance( em, lock );

			final CountDownLatch latch = new CountDownLatch( 1 );
			FutureTask<Boolean> future = inBackgroundThreadStartSecondTransactionAndReadLockInstance(
					latch,
					isolatedEntityManager
			);

			// wait with timeout on the background thread
			log.debug( "testContendedPessimisticLock:  wait on BG thread" );
			boolean backGroundThreadCompleted = latch.await( 3, TimeUnit.SECONDS );

			if ( backGroundThreadCompleted ) {
				// the background thread read a value. At the very least we need to assert that he did not see the
				// changed value
				boolean backgroundThreadHasReadNewValue = future.get();
				assertFalse(
						"The background thread is not allowed to see the updated value while the first transaction has not committed yet",
						backgroundThreadHasReadNewValue
				);
				em.getTransaction().commit();
			}
			else {
				log.debug( "The background thread was blocked" );
				// commit first transaction so that background thread can continue
				em.getTransaction().commit();
				boolean backgroundThreadHasReadNewValue = future.get();
				assertTrue(
						"Background thread should read the new value after being unblocked",
						backgroundThreadHasReadNewValue
				);
			}
		}
		finally {
			cleanup( em, isolatedEntityManager, lock );
		}
	}

	private void cleanup(EntityManager em, EntityManager isolatedEntityManager, Lock lock) throws InterruptedException {
		// only commit the second transaction after the first one completed
		isolatedEntityManager.getTransaction().commit();
		isolatedEntityManager.close();

        // cleanup test data
		em.getTransaction().begin();
		lock = em.getReference( Lock.class, lock.getId() );
		em.remove( lock );
		em.getTransaction().commit();
		em.close();
	}

	private FutureTask<Boolean> inBackgroundThreadStartSecondTransactionAndReadLockInstance(final CountDownLatch latch, final EntityManager isolatedEntityManager) {
		FutureTask<Boolean> bgTask = new FutureTask<Boolean>(
				new Callable<Boolean>() {
					public Boolean call() {
						try {
							isolatedEntityManager.getTransaction().begin();
							log.debug(
									"testContendedPessimisticLock: (BG) about to issue (PESSIMISTIC_READ) query against write-locked entity"
							);
							// we should block on the following read
							Query query = isolatedEntityManager.createQuery(
									"select L from Lock_ L where L.id < 10000 "
							);
							query.setLockMode( LockModeType.PESSIMISTIC_READ );
							List<Lock> resultList = query.getResultList();
							Lock lock = resultList.get( 0 );
							return lock.getName().equals( "foo" );
						}
						catch ( RuntimeException e ) {
							fail( "An error occurred waiting while attempting to read the entity: " + e.getMessage() );
							throw e;
						}
						finally {
							latch.countDown();	// signal that we got the read lock
						}
					}
				}
		);

		Thread thread = new Thread( bgTask );
		thread.setDaemon( true );
		thread.setName( "LockTest read lock" );
		thread.start();

		return bgTask;
	}

	private void inFirstTransactionReloadAndModifyLockInstance(EntityManager em, Lock lock) {
		em.getTransaction().begin();
		lock = em.getReference( Lock.class, lock.getId() );
		em.lock( lock, LockModeType.PESSIMISTIC_WRITE );
		// modify and flush, but don't commit the transaction
		lock.setName( "foo" );
		em.flush();
		log.debug( "testContendedPessimisticLock: got write lock" );
	}

	private Lock createAndPersistLockInstance(EntityManager em) {
		Lock lock = new Lock();
		lock.setName( "testContendedPessimisticLock" );
		em.getTransaction().begin();
		em.persist( lock );
		em.getTransaction().commit();
		em.clear();
		return lock;
	}

	@Test
	@RequiresDialect( Oracle10gDialect.class )
	@RequiresDialectFeature( DialectChecks.SupportsLockTimeouts.class )
	public void testContendedPessimisticReadLockTimeout() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		final EntityManager em2 = createIsolatedEntityManager();
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask = null;
		final CountDownLatch latch = new CountDownLatch( 1 );
		try {
			lock.setName( "testContendedPessimisticReadLockTimeout" );

			em.getTransaction().begin();
			em.persist( lock );
			em.getTransaction().commit();
			em.clear();

			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.lock( lock, LockModeType.PESSIMISTIC_WRITE );
			final Integer id = lock.getId();
			lock.getName();		// force entity to be read
			log.info( "testContendedPessimisticReadLockTimeout: got write lock" );

			bgTask = new FutureTask<Boolean>(
					new Callable<Boolean>() {
						public Boolean call() {
							try {
								boolean timedOut = false;	// true (success) if LockTimeoutException occurred
								em2.getTransaction().begin();
								log.info(
										"testContendedPessimisticReadLockTimeout: (BG) about to read write-locked entity"
								);
								// we should block on the following read
								Lock lock2 = em2.getReference( Lock.class, id );
								lock2.getName();		//  force entity to be read
								log.info( "testContendedPessimisticReadLockTimeout: (BG) read write-locked entity" );
								Map<String, Object> props = new HashMap<String, Object>();
								// timeout is in milliseconds
								props.put( AvailableSettings.LOCK_TIMEOUT, 1000 );
								try {
									em2.lock( lock2, LockModeType.PESSIMISTIC_READ, props );
								}
								catch ( LockTimeoutException e ) {
									// success
									log.info(
											"testContendedPessimisticReadLockTimeout: (BG) got expected timeout exception"
									);
									timedOut = true;
									em2.getTransaction().rollback();
									return timedOut;
								}
								catch ( Throwable e ) {
									log.info( "Expected LockTimeoutException but got unexpected exception", e );
									throw new RuntimeException(
											"Expected LockTimeoutException but got unexpected exception", e
									);
								}
								em2.getTransaction().commit();
								return timedOut;
							}
							finally {
								latch.countDown();	// signal that we finished
							}
						}
					}
			);
			t = new Thread( bgTask );
			t.setDaemon( true );
			t.setName( "Lock timeout Test (bg)" );
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet );
			assertTrue( "background test thread timed out on lock attempt", bgTask.get() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null ) {	  // wait for background thread to finish before deleting entity
				t.join();
			}
			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.remove( lock );
			em.getTransaction().commit();
			em.close();
			em2.close();
		}
	}

	@Test
	@RequiresDialect( Oracle10gDialect.class )
	@RequiresDialectFeature( DialectChecks.SupportsLockTimeouts.class )
	public void testContendedPessimisticWriteLockTimeout() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		final EntityManager em2 = createIsolatedEntityManager();
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask;
		final CountDownLatch latch = new CountDownLatch( 1 );
		try {
			lock.setName( "testContendedPessimisticWriteLockTimeout" );

			em.getTransaction().begin();
			em.persist( lock );
			em.getTransaction().commit();
			em.clear();

			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.lock( lock, LockModeType.PESSIMISTIC_WRITE );
			final Integer id = lock.getId();
			lock.getName();		// force entity to be read
			log.info( "testContendedPessimisticWriteLockTimeout: got write lock" );

			bgTask = new FutureTask<Boolean>(
					new Callable<Boolean>() {
						public Boolean call() {
							try {
								boolean timedOut = false;	// true (success) if LockTimeoutException occurred
								em2.getTransaction().begin();
								log.info(
										"testContendedPessimisticWriteLockTimeout: (BG) about to read write-locked entity"
								);
								// we should block on the following read
								Lock lock2 = em2.getReference( Lock.class, id );
								lock2.getName();		//  force entity to be read
								log.info( "testContendedPessimisticWriteLockTimeout: (BG) read write-locked entity" );
								Map<String, Object> props = new HashMap<String, Object>();
								// timeout is in milliseconds
								props.put( AvailableSettings.LOCK_TIMEOUT, 1000 );
								try {
									em2.lock( lock2, LockModeType.PESSIMISTIC_WRITE, props );
								}
								catch ( LockTimeoutException e ) {
									// success
									log.info(
											"testContendedPessimisticWriteLockTimeout: (BG) got expected timeout exception"
									);
									timedOut = true;
								}
								catch ( Throwable e ) {
									log.info( "Expected LockTimeoutException but got unexpected exception", e );
								}
								em2.getTransaction().commit();
								return timedOut;
							}
							finally {
								latch.countDown();	// signal that we finished
							}
						}
					}
			);
			t = new Thread( bgTask );
			t.setDaemon( true );
			t.setName( "Lock timeout Test (bg)" );
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet );
			assertTrue( "background test thread timed out on lock attempt", bgTask.get() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null ) {	  // wait for background thread to finish before deleting entity
				t.join();
			}
			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.remove( lock );
			em.getTransaction().commit();
			em.close();
			em2.close();
		}
	}

	@Test
	@RequiresDialect( { Oracle10gDialect.class, PostgreSQL81Dialect.class })
    @RequiresDialectFeature( DialectChecks.SupportsLockTimeouts.class )
	public void testContendedPessimisticWriteLockNoWait() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		final EntityManager em2 = createIsolatedEntityManager();
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask;
		final CountDownLatch latch = new CountDownLatch( 1 );
		try {
			lock.setName( "testContendedPessimisticWriteLockNoWait" );

			em.getTransaction().begin();
			em.persist( lock );
			em.getTransaction().commit();
			em.clear();

			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.lock( lock, LockModeType.PESSIMISTIC_WRITE );
			final Integer id = lock.getId();
			lock.getName();		// force entity to be read
			log.info( "testContendedPessimisticWriteLockNoWait: got write lock" );

			bgTask = new FutureTask<Boolean>(
					new Callable<Boolean>() {
						public Boolean call() {
							try {
								boolean timedOut = false;	// true (success) if LockTimeoutException occurred
								em2.getTransaction().begin();
								log.info(
										"testContendedPessimisticWriteLockNoWait: (BG) about to read write-locked entity"
								);
								// we should block on the following read
								Lock lock2 = em2.getReference( Lock.class, id );
								lock2.getName();		//  force entity to be read
								log.info( "testContendedPessimisticWriteLockNoWait: (BG) read write-locked entity" );
								Map<String, Object> props = new HashMap<String, Object>();
								// timeout of zero means no wait (for lock)
								props.put( AvailableSettings.LOCK_TIMEOUT, 0 );
								try {
									em2.lock( lock2, LockModeType.PESSIMISTIC_WRITE, props );
								}
								catch ( LockTimeoutException e ) {
									// success
									log.info(
											"testContendedPessimisticWriteLockNoWait: (BG) got expected timeout exception"
									);
									timedOut = true;
								}
								catch ( Throwable e ) {
									log.info( "Expected LockTimeoutException but got unexpected exception", e );
								}
								em2.getTransaction().commit();
								return timedOut;
							}
							finally {
								latch.countDown();	// signal that we finished
							}
						}
					}
			);
			t = new Thread( bgTask );
			t.setDaemon( true );
			t.setName( "Lock timeout Test (bg)" );
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet );
			assertTrue( "background test thread timed out on lock attempt", bgTask.get() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null ) {	  // wait for background thread to finish before deleting entity
				t.join();
			}
			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.remove( lock );
			em.getTransaction().commit();
			em.close();
			em2.close();
		}
	}

	@Test
	@RequiresDialect( Oracle10gDialect.class )
	@RequiresDialectFeature( DialectChecks.SupportsLockTimeouts.class )
	@FailureExpected( jiraKey = "HHH-8001" )
	public void testQueryTimeout() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		final EntityManager em2 = createIsolatedEntityManager();
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask;
		final CountDownLatch latch = new CountDownLatch( 1 );
		try {
			lock.setName( "testQueryTimeout" );

			em.getTransaction().begin();
			em.persist( lock );
			em.getTransaction().commit();
			em.clear();

			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.lock( lock, LockModeType.PESSIMISTIC_WRITE );
			final Integer id = lock.getId();
			lock.getName();		// force entity to be read
			log.info( "testQueryTimeout: got write lock" );

			bgTask = new FutureTask<Boolean>(
					new Callable<Boolean>() {
						public Boolean call() {
							try {
								boolean timedOut = false;	// true (success) if LockTimeoutException occurred
								em2.getTransaction().begin();
								log.info( "testQueryTimeout: (BG) about to read write-locked entity" );
								// we should block on the following read
								Lock lock2 = em2.getReference( Lock.class, id );
								lock2.getName();		//  force entity to be read
								log.info( "testQueryTimeout: (BG) read write-locked entity" );
								try {
									// we should block on the following read
									Query query = em2.createQuery(
											"select L from Lock_ L where L.id < 10000 "
									);
									query.setLockMode( LockModeType.PESSIMISTIC_READ );
									query.setHint( QueryHints.SPEC_HINT_TIMEOUT, 500 ); // 1 sec timeout
									List<Lock> resultList = query.getResultList();
									String name = resultList.get( 0 ).getName(); //  force entity to be read
									log.info( "testQueryTimeout: name read =" + name );
								}
								catch ( QueryTimeoutException e ) {
									// success
									log.info( "testQueryTimeout: (BG) got expected timeout exception" );
									timedOut = true;
								}
								catch ( Throwable e ) {
									log.info(
											"testQueryTimeout: Expected LockTimeoutException but got unexpected exception",
											e
									);
								}
								em2.getTransaction().commit();
								return timedOut;
							}
							finally {
								latch.countDown();	// signal that we finished
							}
						}
					}
			);
			t = new Thread( bgTask );
			t.setDaemon( true );
			t.setName( "testQueryTimeout (bg)" );
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet );
			assertTrue( "background test thread timed out on lock attempt", bgTask.get() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null ) {	  // wait for background thread to finish before deleting entity
				t.join();
			}
			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.remove( lock );
			em.getTransaction().commit();
			em.close();
			em2.close();
		}
	}

	@Test
	@RequiresDialect( Oracle10gDialect.class )
	@RequiresDialectFeature( DialectChecks.SupportsLockTimeouts.class )
	@FailureExpected( jiraKey = "HHH-8001" )
	public void testQueryTimeoutEMProps() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		Map<String, Object> queryTimeoutProps = new HashMap<String, Object>();
		queryTimeoutProps.put( QueryHints.SPEC_HINT_TIMEOUT, 500 ); // 1 sec timeout (should round up)
		final EntityManager em2 = createIsolatedEntityManager( queryTimeoutProps );
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask;
		final CountDownLatch latch = new CountDownLatch( 1 );
		try {
			lock.setName( "testQueryTimeout" );

			em.getTransaction().begin();
			em.persist( lock );
			em.getTransaction().commit();
			em.clear();

			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.lock( lock, LockModeType.PESSIMISTIC_WRITE );
			final Integer id = lock.getId();
			lock.getName();		// force entity to be read
			log.info( "testQueryTimeout: got write lock" );

			bgTask = new FutureTask<Boolean>(
					new Callable<Boolean>() {
						public Boolean call() {
							try {
								boolean timedOut = false;	// true (success) if LockTimeoutException occurred
								em2.getTransaction().begin();
								log.info( "testQueryTimeout: (BG) about to read write-locked entity" );
								// we should block on the following read
								Lock lock2 = em2.getReference( Lock.class, id );
								lock2.getName();		//  force entity to be read
								log.info( "testQueryTimeout: (BG) read write-locked entity" );
								try {
									// we should block on the following read
									Query query = em2.createQuery(
											"select L from Lock_ L where L.id < 10000 "
									);
									query.setLockMode( LockModeType.PESSIMISTIC_READ );
									List<Lock> resultList = query.getResultList();
									String name = resultList.get( 0 ).getName(); //  force entity to be read
									log.info( "testQueryTimeout: name read =" + name );
								}
								catch ( QueryTimeoutException e ) {
									// success
									log.info( "testQueryTimeout: (BG) got expected timeout exception" );
									timedOut = true;
								}
								catch ( Throwable e ) {
									log.info(
											"testQueryTimeout: Expected LockTimeoutException but got unexpected exception",
											e
									);
								}
								em2.getTransaction().commit();
								return timedOut;
							}
							finally {
								latch.countDown();	// signal that we finished
							}
						}
					}
			);
			t = new Thread( bgTask );
			t.setDaemon( true );
			t.setName( "testQueryTimeout (bg)" );
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet );
			assertTrue( "background test thread timed out on lock attempt", bgTask.get() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null ) {	  // wait for background thread to finish before deleting entity
				t.join();
			}
			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.remove( lock );
			em.getTransaction().commit();
			em.close();
			em2.close();
		}
	}

	@Test
	@RequiresDialect( Oracle10gDialect.class )
	@RequiresDialectFeature( DialectChecks.SupportsLockTimeouts.class )
	public void testLockTimeoutEMProps() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		Map<String, Object> TimeoutProps = new HashMap<String, Object>();
		TimeoutProps.put( AvailableSettings.LOCK_TIMEOUT, 1000 ); // 1 second timeout
		final EntityManager em2 = createIsolatedEntityManager( TimeoutProps );
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask;
		final CountDownLatch latch = new CountDownLatch( 1 );
		try {
			lock.setName( "testLockTimeoutEMProps" );

			em.getTransaction().begin();
			em.persist( lock );
			em.getTransaction().commit();
			em.clear();

			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.lock( lock, LockModeType.PESSIMISTIC_WRITE );
			final Integer id = lock.getId();
			lock.getName();		// force entity to be read
			log.info( "testLockTimeoutEMProps: got write lock" );

			bgTask = new FutureTask<Boolean>(
					new Callable<Boolean>() {
						public Boolean call() {
							try {
								boolean timedOut = false;	// true (success) if LockTimeoutException occurred
								em2.getTransaction().begin();
								log.info( "testLockTimeoutEMProps: (BG) about to read write-locked entity" );
								// we should block on the following read
								Lock lock2 = em2.getReference( Lock.class, id );
								lock2.getName();		//  force entity to be read
								log.info( "testLockTimeoutEMProps: (BG) read write-locked entity" );
								// em2 already has AvailableSettings.LOCK_TIMEOUT of 1 second applied
								try {
									em2.lock( lock2, LockModeType.PESSIMISTIC_WRITE );
								}
								catch ( LockTimeoutException e ) {
									// success
									log.info( "testLockTimeoutEMProps: (BG) got expected timeout exception" );
									timedOut = true;
								}
								catch ( Throwable e ) {
									log.info( "Expected LockTimeoutException but got unexpected exception", e );
								}
								em2.getTransaction().commit();
								return timedOut;
							}
							finally {
								latch.countDown();	// signal that we finished
							}
						}
					}
			);
			t = new Thread( bgTask );
			t.setDaemon( true );
			t.setName( "Lock timeout Test (bg)" );
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet );
			assertTrue( "background test thread timed out on lock attempt", bgTask.get() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null ) {	  // wait for background thread to finish before deleting entity
				t.join();
			}
			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.remove( lock );
			em.getTransaction().commit();
			em.close();
			em2.close();
		}
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Lock.class,
				UnversionedLock.class
		};
	}
}
