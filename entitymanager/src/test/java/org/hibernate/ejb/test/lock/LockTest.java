//$Id$
package org.hibernate.ejb.test.lock;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.ejb.test.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Emmanuel Bernard
 */
public class LockTest extends TestCase {

	private static final Log log = LogFactory.getLog( LockTest.class );
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
			lock.setName( "contendedLock" );

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

					em2.getTransaction().begin();
					log.info("testContendedPessimisticLock: (BG) about to read write-locked entity");
					// we should block on the following read
					Lock lock2 = em2.getReference( Lock.class, id );
					lock2.getName();		//  force entity to be read
					log.info("testContendedPessimisticLock: (BG) read write-locked entity");
					em2.lock( lock2, LockModeType.PESSIMISTIC_READ);
					log.info("testContendedPessimisticLock: (BG) got read lock on entity");
					em2.getTransaction().commit();
					latch.countDown();	// signal that we got the read lock
				}
			} );

			// t.setDaemon( true );
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

	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Lock.class,
				UnversionedLock.class
		};
	}
}
