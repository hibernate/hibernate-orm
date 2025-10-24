/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.RollbackException;
import jakarta.persistence.TransactionRequiredException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@Jpa(
		annotatedClasses = {Book.class},
		integrationSettings = {@Setting(name = AvailableSettings.JPA_TRANSACTION_COMPLIANCE, value = "true")},
		generateStatistics = true
)
public class FlushAndTransactionTest {

	@AfterEach
	public void afterEach(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testAlwaysTransactionalOperations(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
			entityManager -> {
				Book book = new Book();
				book.name = "Le petit prince";
				entityManager.getTransaction().begin();
				entityManager.persist( book );
				entityManager.getTransaction().commit();

				assertThrows(
						TransactionRequiredException.class,
						entityManager::flush
				);

				assertThrows(
						TransactionRequiredException.class,
						() -> entityManager.lock( book, LockModeType.READ )
				);
			}
		);
	}

	@Test
	public void testTransactionalOperationsWhenExtended(EntityManagerFactoryScope scope) {
		Statistics stats = scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getStatistics();
		stats.clear();

		scope.inEntityManager(
			entityManager -> {
				Book book = new Book();
				book.name = "Le petit prince";
				entityManager.persist( book );
				assertEquals( 0, stats.getEntityInsertCount() );

				entityManager.getTransaction().begin();
				entityManager.flush();
				entityManager.getTransaction().commit();
				assertEquals( 1, stats.getEntityInsertCount() );

				entityManager.clear();
				book.name = "Le prince";
				book = entityManager.merge( book );

				entityManager.refresh( book );
				assertEquals( 0, stats.getEntityUpdateCount() );
				entityManager.getTransaction().begin();
				entityManager.flush();
				entityManager.getTransaction().commit();
				assertEquals( 0, stats.getEntityUpdateCount() );

				book.name = "Le prince";
				entityManager.getTransaction().begin();
				entityManager.find( Book.class, book.id );
				entityManager.getTransaction().commit();
				assertEquals( 1, stats.getEntityUpdateCount() );

				entityManager.remove( book );
				assertEquals( 0, stats.getEntityDeleteCount() );
				entityManager.getTransaction().begin();
				entityManager.flush();
				entityManager.getTransaction().commit();
				assertEquals( 1, stats.getEntityDeleteCount() );
			}
		);
	}

	@Test
	public void testMergeWhenExtended(EntityManagerFactoryScope scope) {
		Statistics stats = scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getStatistics();

		scope.inEntityManager(
			entityManager -> {
				Book book = new Book();
				book.name = "Le petit prince";

				entityManager.getTransaction().begin();
				entityManager.persist( book );
				assertEquals( 0, stats.getEntityInsertCount() );
				entityManager.getTransaction().commit();

				entityManager.clear(); //persist and clear
				stats.clear();

				Book bookReloaded = entityManager.find( Book.class, book.id );
				book.name = "Le prince";
				assertEquals( entityManager.merge( book ), bookReloaded,
						"Merge should use the available entiies in the PC" );
				assertEquals( book.name, bookReloaded.name );

				assertEquals( 0, stats.getEntityDeleteCount() );
				assertEquals( 0, stats.getEntityInsertCount() );
				assertEquals( 0, stats.getEntityUpdateCount(), "Updates should have been queued" );

				entityManager.getTransaction().begin();
				Book bookReReloaded = entityManager.find( Book.class, bookReloaded.id );
				assertEquals( bookReReloaded, bookReloaded, "reload should return the object in PC" );
				assertEquals( bookReReloaded.name, bookReloaded.name );
				entityManager.getTransaction().commit();

				assertEquals( 0, stats.getEntityDeleteCount() );
				assertEquals( 0, stats.getEntityInsertCount() );
				assertEquals( 1, stats.getEntityUpdateCount(), "Work in Tx should flush" );
			}
		);
	}

	@Test
	public void testCloseAndTransaction(EntityManagerFactoryScope scope) {
		EntityManager entityManager = scope.fromEntityManager(
				em -> {
					em.getTransaction().begin();
					Book book = new Book();
					book.name = "Java for Dummies";
					return em;
				}
		);
		assertFalse( entityManager.isOpen() );

		assertThrows(
				IllegalStateException.class,
				entityManager::flush,
				"direct action on a closed em should fail"
		);
	}

	@Test
	public void testTransactionCommitDoesNotFlush(EntityManagerFactoryScope scope) {
		Book book = scope.fromTransaction( entityManager -> {
			Book _book = new Book();
			_book.name = "Java for Dummies";
			entityManager.persist( _book );
			return _book;
		} );
		scope.inTransaction( entityManager -> {
			List<Book> result = entityManager.createQuery( "select book from Book book where book.name = :title", Book.class ).
					setParameter( "title", book.name ).getResultList();
			assertEquals( 1, result.size(), "EntityManager.commit() should trigger a flush()" );
		} );
	}

	@Test
	public void testTransactionAndContains(EntityManagerFactoryScope scope) {
		Book book = scope.fromTransaction( entityManager -> {
			Book _book = new Book();
			_book.name = "Java for Dummies";
			entityManager.persist( _book );
			return _book;
		} );

		scope.inEntityManager( entityManager -> {
			entityManager.getTransaction().begin();
			List<Book> result = entityManager.createQuery( "select book from Book book where book.name = :title", Book.class ).
					setParameter( "title", book.name ).getResultList();
			assertEquals( 1, result.size(), "EntityManager.commit() should trigger a flush()" );
			assertTrue( entityManager.contains( result.get( 0 ) ) );
			entityManager.getTransaction().commit();
			assertTrue( entityManager.contains( result.get( 0 ) ) );
		} );
	}

	@Test
	public void testRollbackOnlyOnPersistenceException(EntityManagerFactoryScope scope) {
		Book book = new Book();
		book.name = "Stolen keys";
		book.id = null; //new Integer( 50 );
		scope.inEntityManager(
				entityManager -> {
				entityManager.getTransaction().begin();

				assertThrows(
						PersistenceException.class,
						() -> {
							entityManager.persist( book );
							entityManager.flush();
							entityManager.clear();
							book.setName( "kitty kid" );
							entityManager.merge( book );
							entityManager.flush();
							entityManager.clear();
							book.setName( "kitty kid2" ); //non updated version
							entityManager.merge( book );
							entityManager.flush();
						},
						"optimistic locking exception expected"
				);

				assertThrows(
						RollbackException.class,
						() -> entityManager.getTransaction().commit(),
						"Commit should be rolled back"
				);
			}
		);
	}

	@Test
	public void testRollbackExceptionOnOptimisticLockException(EntityManagerFactoryScope scope) {
		Book book = new Book();
		book.name = "Stolen keys";
		book.id = null; //new Integer( 50 );
		scope.inEntityManager(
			entityManager -> {
				entityManager.getTransaction().begin();
				entityManager.persist( book );
				entityManager.flush();
				entityManager.clear();
				book.setName( "kitty kid" );
				entityManager.merge( book );
				entityManager.flush();
				entityManager.clear();
				book.setName( "kitty kid2" ); //non updated version

				assertInstanceOf(
						StaleObjectStateException.class,
						assertThrows(
								OptimisticLockException.class,
								() -> {
									entityManager.unwrap( Session.class ).merge( book );
									entityManager.getTransaction().commit();
								},
								"Commit should be rolled back"
						).getCause(),
						"During flush a StateStateException is wrapped into a OptimisticLockException"
				);
			}
		);
	}

	@Test
	public void testRollbackClearPC(EntityManagerFactoryScope scope) {
		Book book = new Book();
		book.name = "Stolen keys";
		scope.inEntityManager(
			entityManager -> {
				entityManager.getTransaction().begin();
				entityManager.persist( book );
				entityManager.getTransaction().commit();
				entityManager.getTransaction().begin();
				book.name = "Recovered keys";
				entityManager.merge( book );
				entityManager.getTransaction().rollback();
				entityManager.getTransaction().begin();
				assertEquals( "Stolen keys", entityManager.find( Book.class, book.id ).name );
				entityManager.getTransaction().commit();
			}
		);
	}

	@Test
	public void testSetRollbackOnlyAndFlush(EntityManagerFactoryScope scope) {
		Book book = new Book();
		book.name = "The jungle book";
		scope.inEntityManager(
			entityManager -> {
				entityManager.getTransaction().begin();
				entityManager.getTransaction().setRollbackOnly();
				entityManager.persist( book );
				entityManager.flush();
				entityManager.getTransaction().rollback();
				entityManager.getTransaction().begin();
				Query query = entityManager.createQuery( "SELECT b FROM Book b WHERE b.name = :name" );
				query.setParameter( "name", book.name );
				assertEquals( 0, query.getResultList().size() );
				entityManager.getTransaction().commit();
			}
		);
	}

}
