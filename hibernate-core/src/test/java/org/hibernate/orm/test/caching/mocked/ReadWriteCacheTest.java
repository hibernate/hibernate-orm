/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.caching.mocked;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

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
	private static final Long BOOK_ID = 1L;

	private CountDownLatch endLatch;

	private AtomicBoolean interceptTransaction;

	@Before
	public void init() {
		endLatch = new CountDownLatch( 1 );
		interceptTransaction = new AtomicBoolean();

		doInHibernate( this::sessionFactory, this::saveBook );
	}

	@Test
	@SkipForDialect(value = CockroachDialect.class, comment = "CockroachDB uses SERIALIZABLE isolation, and does not support this")
	@SkipForDialect(value = HSQLDialect.class, comment = "HSQLDB seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	@SkipForDialect(value = SybaseASEDialect.class, comment = "Sybase seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	public void testDelete() throws InterruptedException {

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Delete Book" );
			Book book = session.get( Book.class, BOOK_ID );
			session.delete( book );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, this::assertBookNotFound );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13792")
	public void testDeleteHQL() throws InterruptedException {

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Delete Book using HQL" );
			int numRows = session.createQuery( "delete from Book where id = :id" )
					.setParameter( "id", BOOK_ID )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, this::assertBookNotFound );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13792")
	public void testDeleteNativeQuery() throws InterruptedException {

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Delete Book using NativeQuery" );
			int numRows = session.createNativeQuery( "delete from Book where id = :id" )
					.setParameter( "id", BOOK_ID )
					.addSynchronizedEntityClass( Book.class )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, this::assertBookNotFound );
	}

	@Test
	@SkipForDialect(value = CockroachDialect.class, comment = "CockroachDB uses SERIALIZABLE isolation, and does not support this")
	@SkipForDialect(value = HSQLDialect.class, comment = "HSQLDB seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	@SkipForDialect(value = SybaseASEDialect.class, comment = "Sybase seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	public void testUpdate() throws InterruptedException {

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Update Book" );
			Book book = session.get( Book.class, BOOK_ID );
			book.setTitle( UPDATED_TITLE );
			session.save( book );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, this::assertBookUpdated );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13792")
	public void testUpdateHQL() throws InterruptedException {

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Update Book using HQL" );
			int numRows = session.createQuery( "update Book set title = :title where id = :id" )
					.setParameter( "title", UPDATED_TITLE )
					.setParameter( "id", BOOK_ID )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, this::assertBookUpdated );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13792")
	public void testUpdateNativeQuery() throws InterruptedException {

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Update Book using NativeQuery" );
			int numRows = session.createNativeQuery( "update Book set title = :title where id = :id" )
					.setParameter( "title", UPDATED_TITLE )
					.setParameter( "id", BOOK_ID )
					.addSynchronizedEntityClass( Book.class )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		doInHibernate( this::sessionFactory, this::assertBookUpdated );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14983")
	public void testLoadUponHQLUpdate() {
		assertTrue(
				"The second-level cache should contain a book before running the test",
				sessionFactory().getCache().containsEntity( Book.class, BOOK_ID )
		);

		doInHibernate( this::sessionFactory, session -> {
			log.info( "Updating book using HQL..." );

			boolean isBookUpdated = session.createQuery( "update Book set title = :title where id = :id" )
					.setParameter( "title", UPDATED_TITLE )
					.setParameter( "id", BOOK_ID )
					.executeUpdate() == 1;
			assertTrue( isBookUpdated );

			assertBookUpdated( session );
		} );
	}

	@Override
	public void buildSessionFactory() {
		buildSessionFactory( getCacheConfig() );
	}

	@Override
	public void rebuildSessionFactory() {
		rebuildSessionFactory( getCacheConfig() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Book.class,
		};
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "read-write";
	}

	private Consumer<Configuration> getCacheConfig() {
		return configuration -> configuration.setInterceptor( new TransactionInterceptor() );
	}

	private void saveBook(Session session) {
		Book book = new Book();
		book.setId( BOOK_ID );
		book.setTitle( ORIGINAL_TITLE );
		log.infof( "Saving %s before running tests", book );
		session.save( book );
	}

	private void assertBookNotFound(Session session) {
		Book book = session.get( Book.class, BOOK_ID );
		assertNull( book );
	}

	private void assertBookUpdated(Session session) {
		Book book = session.get( Book.class, BOOK_ID );
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

	private final class TransactionInterceptor extends EmptyInterceptor {
		@Override
		public void beforeTransactionCompletion(Transaction tx) {
			if ( interceptTransaction.get() ) {
				try {
					log.info( "Fetch Book" );

					executeSync( () -> {
						Session session = sessionFactory()
								.openSession();
						Book book = session.get( Book.class, BOOK_ID );
						assertNotNull( book );
						log.infof( "Fetched %s", book );
						session.close();
					} );

					assertTrue( sessionFactory().getCache()
										.containsEntity( Book.class, BOOK_ID ) );
				}
				finally {
					endLatch.countDown();
				}
			}
		}
	}
}
