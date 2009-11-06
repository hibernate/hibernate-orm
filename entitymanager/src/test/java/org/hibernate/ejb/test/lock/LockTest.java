//$Id$
package org.hibernate.ejb.test.lock;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class LockTest extends TestCase {

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
	
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Lock.class,
				UnversionedLock.class
		};
	}
}
