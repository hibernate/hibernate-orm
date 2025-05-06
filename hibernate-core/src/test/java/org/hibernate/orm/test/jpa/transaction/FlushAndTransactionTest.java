/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.Statistics;

import org.junit.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.RollbackException;
import jakarta.persistence.TransactionRequiredException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class FlushAndTransactionTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testAlwaysTransactionalOperations() {
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
		catch (TransactionRequiredException e) {
			//success
		}
		try {
			em.lock( book, LockModeType.READ );
			fail( "lock has to be inside a Tx" );
		}
		catch (TransactionRequiredException e) {
			//success
		}
		em.getTransaction().begin();
		em.remove( em.find( Book.class, book.id ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testTransactionalOperationsWhenExtended() {
		Book book = new Book();
		book.name = "Le petit prince";
		EntityManager em = getOrCreateEntityManager();
		Statistics stats = entityManagerFactory().unwrap( SessionFactory.class ).getStatistics();
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
	public void testMergeWhenExtended() {
		Book book = new Book();
		book.name = "Le petit prince";
		EntityManager em = getOrCreateEntityManager();
		Statistics stats = entityManagerFactory().unwrap( SessionFactory.class ).getStatistics();

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
	public void testCloseAndTransaction() {
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
		catch (IllegalStateException e) {
			//success
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	public void testTransactionCommitDoesNotFlush() {
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
	public void testTransactionAndContains() {
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
	public void testRollbackOnlyOnPersistenceException() {
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
		catch (PersistenceException e) {
			//success
		}

		try {
			em.getTransaction().commit();
			fail( "Commit should be rollbacked" );
		}
		catch (RollbackException e) {
			//success
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testRollbackExceptionOnOptimisticLockException() {
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
		try {
			em.unwrap( Session.class ).merge( book );
			em.getTransaction().commit();
			fail( "Commit should be rollbacked" );
		}
		catch (OptimisticLockException e) {
			assertTrue(
					"During flush a StateStateException is wrapped into a OptimisticLockException",
					e.getCause() instanceof StaleObjectStateException
			);
		}
		finally {
			em.close();
		}

	}

	@Test
	public void testRollbackClearPC() {
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
		em.getTransaction().begin();
		assertEquals( "Stolen keys", em.find( Book.class, book.id ).name );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testSetRollbackOnlyAndFlush() {
		Book book = new Book();
		book.name = "The jungle book";
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.getTransaction().setRollbackOnly();
		em.persist( book );
		em.flush();
		em.getTransaction().rollback();
		em.getTransaction().begin();
		Query query = em.createQuery( "SELECT b FROM Book b WHERE b.name = :name" );
		query.setParameter( "name", book.name );
		assertEquals( 0, query.getResultList().size() );
		em.getTransaction().commit();
		em.close();
	}


	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Book.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "true" );
	}
}
