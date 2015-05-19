/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.transaction;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.persistence.TransactionRequiredException;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.Statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class FlushAndTransactionTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testAlwaysTransactionalOperations() throws Exception {
		Book book = new Book();
		book.name = "Le petit prince";
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( book );
		em.getTransaction().commit();
		try {
			em.flush();
			fail( "flush has to be inside a Tx" );
		}
		catch ( TransactionRequiredException e ) {
			//success
		}
		try {
			em.lock( book, LockModeType.READ );
			fail( "lock has to be inside a Tx" );
		}
		catch ( TransactionRequiredException e ) {
			//success
		}
		em.getTransaction().begin();
		em.remove( em.find( Book.class, book.id ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testTransactionalOperationsWhenExtended() throws Exception {
		Book book = new Book();
		book.name = "Le petit prince";
		EntityManager em = getOrCreateEntityManager();
		Statistics stats = ( ( HibernateEntityManagerFactory ) entityManagerFactory() ).getSessionFactory().getStatistics();
		stats.clear();
		stats.setStatisticsEnabled( true );

		em.persist( book );
		assertEquals( 0, stats.getEntityInsertCount() );
		em.getTransaction().begin();
		em.flush();
		em.getTransaction().commit();
		assertEquals( 1, stats.getEntityInsertCount() );

		em.clear();
		book.name = "Le prince";
		book = em.merge( book );

		em.refresh( book );
		assertEquals( 0, stats.getEntityUpdateCount() );
		em.getTransaction().begin();
		em.flush();
		em.getTransaction().commit();
		assertEquals( 0, stats.getEntityUpdateCount() );

		book.name = "Le prince";
		em.getTransaction().begin();
		em.find( Book.class, book.id );
		em.getTransaction().commit();
		assertEquals( 1, stats.getEntityUpdateCount() );

		em.remove( book );
		assertEquals( 0, stats.getEntityDeleteCount() );
		em.getTransaction().begin();
		em.flush();
		em.getTransaction().commit();
		assertEquals( 1, stats.getEntityDeleteCount() );

		em.close();
		stats.setStatisticsEnabled( false );
	}

	@Test
	public void testMergeWhenExtended() throws Exception {
		Book book = new Book();
		book.name = "Le petit prince";
		EntityManager em = getOrCreateEntityManager();
		Statistics stats = ( ( HibernateEntityManagerFactory ) entityManagerFactory() ).getSessionFactory().getStatistics();

		em.getTransaction().begin();
		em.persist( book );
		assertEquals( 0, stats.getEntityInsertCount() );
		em.getTransaction().commit();

		em.clear(); //persist and clear
		stats.clear();
		stats.setStatisticsEnabled( true );

		Book bookReloaded = em.find( Book.class, book.id );

		book.name = "Le prince";
		assertEquals( "Merge should use the available entiies in the PC", em.merge( book ), bookReloaded );
		assertEquals( book.name, bookReloaded.name );

		assertEquals( 0, stats.getEntityDeleteCount() );
		assertEquals( 0, stats.getEntityInsertCount() );
		assertEquals( "Updates should have been queued", 0, stats.getEntityUpdateCount() );

		em.getTransaction().begin();
		Book bookReReloaded = em.find( Book.class, bookReloaded.id );
		assertEquals( "reload should return the object in PC", bookReReloaded, bookReloaded );
		assertEquals( bookReReloaded.name, bookReloaded.name );
		em.getTransaction().commit();

		assertEquals( 0, stats.getEntityDeleteCount() );
		assertEquals( 0, stats.getEntityInsertCount() );
		assertEquals( "Work on Tx should flush", 1, stats.getEntityUpdateCount() );

		em.getTransaction().begin();
		em.remove( bookReReloaded );
		em.getTransaction().commit();

		em.close();
		stats.setStatisticsEnabled( false );
	}

	@Test
	public void testCloseAndTransaction() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Book book = new Book();
		book.name = "Java for Dummies";
		em.close();

		assertFalse( em.isOpen() );
		try {
			em.flush();
			fail( "direct action on a closed em should fail" );
		}
		catch ( IllegalStateException e ) {
			//success
			em.getTransaction().rollback();
		}
	}

	@Test
	public void testTransactionCommitDoesNotFlush() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Book book = new Book();
		book.name = "Java for Dummies";
		em.persist( book );
		em.getTransaction().commit();
		em.close();
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List result = em.createQuery( "select book from Book book where book.name = :title" ).
				setParameter( "title", book.name ).getResultList();
		assertEquals( "EntityManager.commit() should trigger a flush()", 1, result.size() );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testTransactionAndContains() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Book book = new Book();
		book.name = "Java for Dummies";
		em.persist( book );
		em.getTransaction().commit();
		em.close();
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List result = em.createQuery( "select book from Book book where book.name = :title" ).
				setParameter( "title", book.name ).getResultList();
		assertEquals( "EntityManager.commit() should trigger a flush()", 1, result.size() );
		assertTrue( em.contains( result.get( 0 ) ) );
		em.getTransaction().commit();
		assertTrue( em.contains( result.get( 0 ) ) );
		em.close();
	}

	@Test
	public void testRollbackOnlyOnPersistenceException() throws Exception {
		Book book = new Book();
		book.name = "Stolen keys";
		book.id = null; //new Integer( 50 );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( book );
			em.flush();
			em.clear();
			book.setName( "kitty kid" );
			em.merge( book );
			em.flush();
			em.clear();
			book.setName( "kitty kid2" ); //non updated version
			em.merge( book );
			em.flush();
			fail( "optimistic locking exception" );
		}
		catch ( PersistenceException e ) {
			//success
		}
		try {
			em.getTransaction().commit();
			fail( "Commit should be rollbacked" );
		}
		catch ( RollbackException e ) {
			//success
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testRollbackExceptionOnOptimisticLockException() throws Exception {
		Book book = new Book();
		book.name = "Stolen keys";
		book.id = null; //new Integer( 50 );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( book );
		em.flush();
		em.clear();
		book.setName( "kitty kid" );
		em.merge( book );
		em.flush();
		em.clear();
		book.setName( "kitty kid2" ); //non updated version
		em.unwrap( Session.class ).update( book );
		try {
			em.getTransaction().commit();
			fail( "Commit should be rollbacked" );
		}
		catch ( RollbackException e ) {
			assertTrue(
					"During flush a StateStateException is wrapped into a OptimisticLockException",
					e.getCause() instanceof OptimisticLockException
			);
		}
		finally {
			em.close();
		}

	}

	@Test
	public void testRollbackClearPC() throws Exception {
		Book book = new Book();
		book.name = "Stolen keys";
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( book );
		em.getTransaction().commit();
		em.getTransaction().begin();
		book.name = "Recovered keys";
		em.merge( book );
		em.getTransaction().rollback();
		assertEquals( "Stolen keys", em.find( Book.class, book.id ).name );
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Book.class
		};
	}
}
