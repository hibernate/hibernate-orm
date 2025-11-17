/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching.mocked;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.Configuration;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Frank Doherty
 */
public class ReadWriteCacheTest extends BaseCoreFunctionalTestCase {

	private static final String ORIGINAL_TITLE = "Original Title";
	private static final String UPDATED_TITLE = "Updated Title";

	private long bookId;
	private CountDownLatch endLatch;
	private AtomicBoolean interceptTransaction;

	@Override
	public void buildSessionFactory() {
		buildSessionFactory( getCacheConfig() );
	}

	@Before
	public void init() {
		endLatch = new CountDownLatch( 1 );
		interceptTransaction = new AtomicBoolean();
	}

	@Override
	public void rebuildSessionFactory() {
		rebuildSessionFactory( getCacheConfig() );
	}

	@Test
	@SkipForDialect(value = CockroachDialect.class, comment = "CockroachDB uses SERIALIZABLE isolation, and does not support this")
	@SkipForDialect(value = HSQLDialect.class, comment = "HSQLDB seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	@SkipForDialect(value = DerbyDialect.class, comment = "HSQLDB seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	@SkipForDialect(value = SybaseASEDialect.class, comment = "Sybase seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	public void testDelete() throws InterruptedException {
		bookId = 1L;

		doInHibernate( this::sessionFactory, session -> {
			createBook( bookId, session );
		} );

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Delete Book" );
			Book book = session.get( Book.class, bookId );
			session.remove( book );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, session -> {
			assertBookNotFound( bookId, session );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13792")
	public void testDeleteHQL() throws InterruptedException {
		bookId = 2L;

		doInHibernate( this::sessionFactory, session -> {
			createBook( bookId, session );
		} );

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Delete Book using HQL" );
			int numRows = session.createQuery( "delete from Book where id = :id" )
					.setParameter( "id", bookId )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, session -> {
			assertBookNotFound( bookId, session );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13792")
	public void testDeleteNativeQuery() throws InterruptedException {
		bookId = 3L;

		doInHibernate( this::sessionFactory, session -> {
			createBook( bookId, session );
		} );

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Delete Book using NativeQuery" );
			int numRows = session.createNativeQuery( "delete from Book where id = :id" )
					.setParameter( "id", bookId )
					.addSynchronizedEntityClass( Book.class )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, session -> {
			assertBookNotFound( bookId, session );
		} );
	}

	@Test
	@SkipForDialect(value = CockroachDialect.class, comment = "CockroachDB uses SERIALIZABLE isolation, and does not support this")
	@SkipForDialect(value = HSQLDialect.class, comment = "HSQLDB seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	@SkipForDialect(value = DerbyDialect.class, comment = "HSQLDB seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	@SkipForDialect(value = SybaseASEDialect.class, comment = "Sybase seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	public void testUpdate() throws InterruptedException {
		bookId = 4L;

		doInHibernate( this::sessionFactory, session -> {
			createBook( bookId, session );
		} );

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Update Book" );
			Book book = session.get( Book.class, bookId );
			book.setTitle( UPDATED_TITLE );
			session.persist( book );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, session -> {
			loadBook( bookId, session );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13792")
	public void testUpdateHQL() throws InterruptedException {
		bookId = 5L;

		doInHibernate( this::sessionFactory, session -> {
			createBook( bookId, session );
		} );

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Update Book using HQL" );
			int numRows = session.createQuery( "update Book set title = :title where id = :id" )
					.setParameter( "title", UPDATED_TITLE )
					.setParameter( "id", bookId )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, session -> {
			loadBook( bookId, session );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13792")
	public void testUpdateNativeQuery() throws InterruptedException {
		bookId = 6L;

		doInHibernate( this::sessionFactory, session -> {
			createBook( bookId, session );
		} );

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Update Book using NativeQuery" );
			int numRows = session.createNativeQuery( "update Book set title = :title where id = :id" )
					.setParameter( "title", UPDATED_TITLE )
					.setParameter( "id", bookId )
					.addSynchronizedEntityClass( Book.class )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, session -> {
			loadBook( bookId, session );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Book.class,
		};
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "read-write";
	}

	private void assertBookNotFound(long bookId, Session session) {
		log.info( "Load Book" );
		Book book = session.get( Book.class, bookId );
		assertNull( book );
	}

	private void createBook(long bookId, Session session) {
		log.info( "Create Book" );
		Book book = new Book();
		book.setId( bookId );
		book.setTitle( ORIGINAL_TITLE );
		session.persist( book );
	}

	private Consumer<Configuration> getCacheConfig() {
		return configuration -> configuration.setInterceptor( new TransactionInterceptor() );
	}

	private void loadBook(long bookId, Session session) {
		log.info( "Load Book" );
		Book book = session.get( Book.class, bookId );
		assertNotNull( book );
		assertEquals( "Found old value", UPDATED_TITLE, book.getTitle() );
	}

	@Entity(name = "Book")
	@Cacheable
	@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	private static final class Book {

		@Id
		private Long id;

		private String title;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String toString() {
			return "Book[id=" + id + ",title=" + title + "]";
		}
	}

	private final class TransactionInterceptor implements Interceptor {
		@Override
		public void beforeTransactionCompletion(Transaction tx) {
			if ( interceptTransaction.get() ) {
				try {
					log.info( "Fetch Book" );

					executeSync( () -> {
						Session session = sessionFactory()
								.openSession();
						Book book = session.get( Book.class, bookId );
						assertNotNull( book );
						log.infof( "Fetched %s", book );
						session.close();
					} );

					assertTrue( sessionFactory().getCache()
										.containsEntity( Book.class, bookId ) );
				}
				finally {
					endLatch.countDown();
				}
			}
		}
	}
}
