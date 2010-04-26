//$Id$
package org.hibernate.ejb.test.lock;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;
import javax.persistence.QueryTimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.test.TestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * @author Emmanuel Bernard
 */
public class LockTest extends TestCase {
	private static final Log log = LogFactory.getLog( LockTest.class );

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
			fail("expected OptimisticLockException exception");
		} catch(OptimisticLockException expected) {}
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
		 assertEquals( "lock mode should be OPTIMISTIC ", LockModeType.OPTIMISTIC, em2.getLockMode(lock) );
		 em2.lock( lock, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
		 assertEquals( "lock mode should be OPTIMISTIC_FORCE_INCREMENT ", LockModeType.OPTIMISTIC_FORCE_INCREMENT, em2.getLockMode(lock) );
		 em2.getTransaction().commit();
		 em2.getTransaction().begin();
		 em2.remove( lock );
		 em2.getTransaction().commit();
		 em2.close();
	}

	public void testContendedPessimisticLock() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		final EntityManager em2 = createIsolatedEntityManager();
		// TODO:  replace dialect instanceof test with a Dialect.hasCapability (e.g. supportsPessimisticWriteLock)
		if ( getDialect() instanceof HSQLDialect) {
			log.info("skipping testContendedPessimisticLock");
			return;
		}
		Lock lock = new Lock();
		Thread t = null;
		try {
			lock.setName( "testContendedPessimisticLock" );

			em.getTransaction().begin();
			em.persist( lock );
			em.getTransaction().commit();
			em.clear();

			em.getTransaction().begin();
			lock = em.getReference( Lock.class, lock.getId() );
			em.lock( lock, LockModeType.PESSIMISTIC_WRITE );
			final Integer id = lock.getId();
			lock.getName();		// force entity to be read
			log.info("testContendedPessimisticLock: got write lock");
			final CountDownLatch latch = new CountDownLatch(1);

			t = new Thread( new Runnable() {
				public void run() {
					try {
						em2.getTransaction().begin();
						log.info("testContendedPessimisticLock: (BG) about to issue (PESSIMISTIC_READ) query against write-locked entity");
						// we should block on the following read
						Query query = em2.createQuery(
								  "select L from Lock_ L where L.id < 10000 ");
						query.setLockMode(LockModeType.PESSIMISTIC_READ);
						List<Lock> resultList = query.getResultList();
						resultList.get(0).getName(); //  force entity to be read
					}
					finally {
						em2.getTransaction().commit();
						latch.countDown();	// signal that we got the read lock
					}
				}
			} );

			t.setDaemon( true );
			t.setName("LockTest read lock");
			t.start();
			log.info("testContendedPessimisticLock:  wait on BG thread");
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );
			// latchSet should be false (timeout) because the background thread
			// shouldn't be able to get a read lock on write locked entity.
			log.info("testContendedPessimisticLock:  BG thread completed transaction");
			assertFalse( "shouldn't be able to get read lock while another transaction has write lock",latchSet );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null) {	  // wait for background thread to finish before deleting entity
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

	public void testContendedPessimisticReadLockTimeout() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		final EntityManager em2 = createIsolatedEntityManager();
		// TODO:  replace dialect instanceof test with a Dialect.hasCapability (e.g. supportsPessimisticLockTimeout)
		if ( ! (getDialect() instanceof Oracle10gDialect)) {
			log.info("skipping testContendedPessimisticReadLockTimeout");
			return;
		}
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask = null;
		final CountDownLatch latch = new CountDownLatch(1);
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
			log.info("testContendedPessimisticReadLockTimeout: got write lock");

			bgTask = new FutureTask<Boolean>( new Callable() {
				public Boolean call() {
					try {
						boolean timedOut = false;	// true (success) if LockTimeoutException occurred  
						em2.getTransaction().begin();
						log.info("testContendedPessimisticReadLockTimeout: (BG) about to read write-locked entity");
						// we should block on the following read
						Lock lock2 = em2.getReference( Lock.class, id );
						lock2.getName();		//  force entity to be read
						log.info("testContendedPessimisticReadLockTimeout: (BG) read write-locked entity");
						Map<String,Object> props = new HashMap<String, Object>();
						// timeout is in milliseconds
						props.put("javax.persistence.lock.timeout", new Integer(1000));
						try {
							em2.lock( lock2, LockModeType.PESSIMISTIC_READ, props);
						}
						catch( LockTimeoutException e) {
							// success
							log.info("testContendedPessimisticReadLockTimeout: (BG) got expected timeout exception");
							 timedOut = true;
						}
						catch ( Throwable e) {
							log.info("Expected LockTimeoutException but got unexpected exception", e);
						}
						em2.getTransaction().commit();
						return new Boolean(timedOut);
					}
					finally {
						latch.countDown();	// signal that we finished
					}
				}
			} );
			t = new Thread(bgTask);
			t.setDaemon( true );
			t.setName("Lock timeout Test (bg)");
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet);
			assertTrue( "background test thread timed out on lock attempt", bgTask.get().booleanValue() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null) {	  // wait for background thread to finish before deleting entity
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

	public void testContendedPessimisticWriteLockTimeout() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		final EntityManager em2 = createIsolatedEntityManager();
		// TODO:  replace dialect instanceof test with a Dialect.hasCapability (e.g. supportsPessimisticLockTimeout)
		if ( ! (getDialect() instanceof Oracle10gDialect)) {
			log.info("skipping testContendedPessimisticWriteLockTimeout");
			return;
		}
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask = null;
		final CountDownLatch latch = new CountDownLatch(1);
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
			log.info("testContendedPessimisticWriteLockTimeout: got write lock");

			bgTask = new FutureTask<Boolean>( new Callable() {
				public Boolean call() {
					try {
						boolean timedOut = false;	// true (success) if LockTimeoutException occurred
						em2.getTransaction().begin();
						log.info("testContendedPessimisticWriteLockTimeout: (BG) about to read write-locked entity");
						// we should block on the following read
						Lock lock2 = em2.getReference( Lock.class, id );
						lock2.getName();		//  force entity to be read
						log.info("testContendedPessimisticWriteLockTimeout: (BG) read write-locked entity");
						Map<String,Object> props = new HashMap<String, Object>();
						// timeout is in milliseconds
						props.put("javax.persistence.lock.timeout", new Integer(1000));
						try {
							em2.lock( lock2, LockModeType.PESSIMISTIC_WRITE, props);
						}
						catch( LockTimeoutException e) {
							// success
							log.info("testContendedPessimisticWriteLockTimeout: (BG) got expected timeout exception");
							 timedOut = true;
						}
						catch ( Throwable e) {
							log.info("Expected LockTimeoutException but got unexpected exception", e);
						}
						em2.getTransaction().commit();
						return new Boolean(timedOut);
					}
					finally {
						latch.countDown();	// signal that we finished
					}
				}
			} );
			t = new Thread(bgTask);
			t.setDaemon( true );
			t.setName("Lock timeout Test (bg)");
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet);
			assertTrue( "background test thread timed out on lock attempt", bgTask.get().booleanValue() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null) {	  // wait for background thread to finish before deleting entity
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

	public void testContendedPessimisticWriteLockNoWait() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		final EntityManager em2 = createIsolatedEntityManager();
		// TODO:  replace dialect instanceof test with a Dialect.hasCapability (e.g. supportsPessimisticLockTimeout)
		if ( ! (getDialect() instanceof Oracle10gDialect)) {
			log.info("skipping testContendedPessimisticWriteLockNoWait");
			return;
		}
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask = null;
		final CountDownLatch latch = new CountDownLatch(1);
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
			log.info("testContendedPessimisticWriteLockNoWait: got write lock");

			bgTask = new FutureTask<Boolean>( new Callable() {
				public Boolean call() {
					try {
						boolean timedOut = false;	// true (success) if LockTimeoutException occurred
						em2.getTransaction().begin();
						log.info("testContendedPessimisticWriteLockNoWait: (BG) about to read write-locked entity");
						// we should block on the following read
						Lock lock2 = em2.getReference( Lock.class, id );
						lock2.getName();		//  force entity to be read
						log.info("testContendedPessimisticWriteLockNoWait: (BG) read write-locked entity");
						Map<String,Object> props = new HashMap<String, Object>();
						// timeout of zero means no wait (for lock)
						props.put("javax.persistence.lock.timeout", new Integer(0));
						try {
							em2.lock( lock2, LockModeType.PESSIMISTIC_WRITE, props);
						}
						catch( LockTimeoutException e) {
							// success
							log.info("testContendedPessimisticWriteLockNoWait: (BG) got expected timeout exception");
							 timedOut = true;
						}
						catch ( Throwable e) {
							log.info("Expected LockTimeoutException but got unexpected exception", e);
						}
						em2.getTransaction().commit();
						return new Boolean(timedOut);
					}
					finally {
						latch.countDown();	// signal that we finished
					}
				}
			} );
			t = new Thread(bgTask);
			t.setDaemon( true );
			t.setName("Lock timeout Test (bg)");
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet);
			assertTrue( "background test thread timed out on lock attempt", bgTask.get().booleanValue() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null) {	  // wait for background thread to finish before deleting entity
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

	public void testQueryTimeout() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		final EntityManager em2 = createIsolatedEntityManager();
		// TODO:  replace dialect instanceof test with a Dialect.hasCapability
		if ( ! (getDialect() instanceof Oracle10gDialect)) {
			log.info("skipping testQueryTimeout");
			return;
		}
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask = null;
		final CountDownLatch latch = new CountDownLatch(1);
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
			log.info("testQueryTimeout: got write lock");

			bgTask = new FutureTask<Boolean>( new Callable() {
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
									  "select L from Lock_ L where L.id < 10000 ");
							query.setLockMode( LockModeType.PESSIMISTIC_READ );
							query.setHint( "javax.persistence.query.timeout", new Integer(500) ); // 1 sec timeout
							List<Lock> resultList = query.getResultList();
							String name = resultList.get(0).getName(); //  force entity to be read
							log.info( "testQueryTimeout: name read =" + name );
						}
						catch( QueryTimeoutException e) {
							// success
							log.info( "testQueryTimeout: (BG) got expected timeout exception" );
							 timedOut = true;
						}
						catch ( Throwable e) {
							log.info( "testQueryTimeout: Expected LockTimeoutException but got unexpected exception", e );
						}
						em2.getTransaction().commit();
						return new Boolean( timedOut );
					}
					finally {
						latch.countDown();	// signal that we finished
					}
				}
			} );
			t = new Thread(bgTask);
			t.setDaemon( true );
			t.setName( "testQueryTimeout (bg)" );
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet);
			assertTrue( "background test thread timed out on lock attempt", bgTask.get().booleanValue() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null) {	  // wait for background thread to finish before deleting entity
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

	public void testQueryTimeoutEMProps() throws Exception {
		// TODO:  replace dialect instanceof test with a Dialect.hasCapability
		if ( ! (getDialect() instanceof Oracle10gDialect)) {
			log.info("skipping testQueryTimeout");
			return;
 		}
		EntityManager em = getOrCreateEntityManager();
		Map queryTimeoutProps = new HashMap();
		queryTimeoutProps.put("javax.persistence.query.timeout", new Integer(500) ); // 1 sec timeout (should round up)
		final EntityManager em2 = createIsolatedEntityManager(queryTimeoutProps);
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask = null;
		final CountDownLatch latch = new CountDownLatch(1);
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
			log.info("testQueryTimeout: got write lock");

			bgTask = new FutureTask<Boolean>( new Callable() {
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
									  "select L from Lock_ L where L.id < 10000 ");
							query.setLockMode( LockModeType.PESSIMISTIC_READ );
							List<Lock> resultList = query.getResultList();
							String name = resultList.get(0).getName(); //  force entity to be read
							log.info( "testQueryTimeout: name read =" + name );
						}
						catch( QueryTimeoutException e) {
							// success
							log.info( "testQueryTimeout: (BG) got expected timeout exception" );
							 timedOut = true;
						}
						catch ( Throwable e) {
							log.info( "testQueryTimeout: Expected LockTimeoutException but got unexpected exception", e );
						}
						em2.getTransaction().commit();
						return new Boolean( timedOut );
					}
					finally {
						latch.countDown();	// signal that we finished
					}
				}
			} );
			t = new Thread(bgTask);
			t.setDaemon( true );
			t.setName( "testQueryTimeout (bg)" );
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet);
			assertTrue( "background test thread timed out on lock attempt", bgTask.get().booleanValue() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null) {	  // wait for background thread to finish before deleting entity
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


	public void testLockTimeoutEMProps() throws Exception {

		EntityManager em = getOrCreateEntityManager();
		Map TimeoutProps = new HashMap();
		TimeoutProps.put("javax.persistence.lock.timeout", new Integer(1000) ); // 1 second timeout
		final EntityManager em2 = createIsolatedEntityManager(TimeoutProps);
		// TODO:  replace dialect instanceof test with a Dialect.hasCapability (e.g. supportsPessimisticLockTimeout)
		if ( ! (getDialect() instanceof Oracle10gDialect)) {
			log.info("skipping testLockTimeoutEMProps");
			return;
		}
		Lock lock = new Lock();
		Thread t = null;
		FutureTask<Boolean> bgTask = null;
		final CountDownLatch latch = new CountDownLatch(1);
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
			log.info("testLockTimeoutEMProps: got write lock");

			bgTask = new FutureTask<Boolean>( new Callable() {
				public Boolean call() {
					try {
						boolean timedOut = false;	// true (success) if LockTimeoutException occurred
						em2.getTransaction().begin();
						log.info("testLockTimeoutEMProps: (BG) about to read write-locked entity");
						// we should block on the following read
						Lock lock2 = em2.getReference( Lock.class, id );
						lock2.getName();		//  force entity to be read
						log.info("testLockTimeoutEMProps: (BG) read write-locked entity");
						// em2 already has javax.persistence.lock.timeout of 1 second applied
						try {
							em2.lock( lock2, LockModeType.PESSIMISTIC_WRITE);
						}
						catch( LockTimeoutException e) {
							// success
							log.info("testLockTimeoutEMProps: (BG) got expected timeout exception");
							 timedOut = true;
						}
						catch ( Throwable e) {
							log.info("Expected LockTimeoutException but got unexpected exception", e);
						}
						em2.getTransaction().commit();
						return new Boolean(timedOut);
					}
					finally {
						latch.countDown();	// signal that we finished
					}
				}
			} );
			t = new Thread(bgTask);
			t.setDaemon( true );
			t.setName("Lock timeout Test (bg)");
			t.start();
			boolean latchSet = latch.await( 10, TimeUnit.SECONDS );  // should return quickly on success
			assertTrue( "background test thread finished (lock timeout is broken)", latchSet);
			assertTrue( "background test thread timed out on lock attempt", bgTask.get().booleanValue() );
			em.getTransaction().commit();
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( t != null) {	  // wait for background thread to finish before deleting entity
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


	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Lock.class,
				UnversionedLock.class
		};
	}
}
