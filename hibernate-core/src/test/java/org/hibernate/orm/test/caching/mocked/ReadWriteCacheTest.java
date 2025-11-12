/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching.mocked;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.Transaction;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Frank Doherty
 */
@DomainModel(
		annotatedClasses = {
				ReadWriteCacheTest.Book.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, value = "read-write"),
				@Setting(name = AvailableSettings.JAKARTA_SHARED_CACHE_MODE, value = "ALL"),
		}
)
@SessionFactory(
		sessionFactoryConfigurer = ReadWriteCacheTest.Configurer.class
)
public class ReadWriteCacheTest {

	private static final String ORIGINAL_TITLE = "Original Title";
	private static final String UPDATED_TITLE = "Updated Title";

	private static long bookId;
	private static CountDownLatch endLatch = new CountDownLatch( 1 );
	private static AtomicBoolean interceptTransaction;

	@BeforeEach
	public void init() {
		endLatch = new CountDownLatch( 1 );
		interceptTransaction = new AtomicBoolean();
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		scope.getSessionFactory().getCache().evictAll();
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "CockroachDB uses SERIALIZABLE isolation, and does not support this")
	@SkipForDialect(dialectClass = HSQLDialect.class,
			reason = "HSQLDB seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	@SkipForDialect(dialectClass = DerbyDialect.class,
			reason = "HSQLDB seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	@SkipForDialect(dialectClass = SybaseASEDialect.class,
			reason = "Sybase seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	public void testDelete(SessionFactoryScope scope) throws InterruptedException {
		bookId = 1L;

		scope.inTransaction( session -> {
			createBook( bookId, session );
		} );

		scope.inTransaction( session -> {
			Book book = session.get( Book.class, bookId );
			session.remove( book );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		scope.inTransaction( session -> {
			assertBookNotFound( bookId, session );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13792")
	public void testDeleteHQL(SessionFactoryScope scope) throws InterruptedException {
		bookId = 2L;

		scope.inTransaction( session -> {
			createBook( bookId, session );
		} );

		scope.inTransaction( session -> {
			int numRows = session.createQuery( "delete from Book where id = :id" )
					.setParameter( "id", bookId )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		scope.inTransaction( session -> {
			assertBookNotFound( bookId, session );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13792")
	public void testDeleteNativeQuery(SessionFactoryScope scope) throws InterruptedException {
		bookId = 3L;

		scope.inTransaction( session -> {
			createBook( bookId, session );
		} );

		scope.inTransaction( session -> {
			int numRows = session.createNativeQuery( "delete from Book where id = :id" )
					.setParameter( "id", bookId )
					.addSynchronizedEntityClass( Book.class )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		scope.inTransaction( session -> {
			assertBookNotFound( bookId, session );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "CockroachDB uses SERIALIZABLE isolation, and does not support this")
	@SkipForDialect(dialectClass = HSQLDialect.class,
			reason = "HSQLDB seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	@SkipForDialect(dialectClass = DerbyDialect.class,
			reason = "HSQLDB seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	@SkipForDialect(dialectClass = SybaseASEDialect.class,
			reason = "Sybase seems to block on acquiring a SHARE lock when a different TX upgraded a SHARE to EXCLUSIVE lock, maybe the upgrade caused a table lock?")
	public void testUpdate(SessionFactoryScope scope) throws InterruptedException {
		bookId = 4L;

		scope.inTransaction( session -> {
			createBook( bookId, session );
		} );

		scope.inTransaction( session -> {
			Book book = session.get( Book.class, bookId );
			book.setTitle( UPDATED_TITLE );
			session.persist( book );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		scope.inTransaction( session -> {
			loadBook( bookId, session );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13792")
	public void testUpdateHQL(SessionFactoryScope scope) throws InterruptedException {
		bookId = 5L;

		scope.inTransaction( session -> {
			createBook( bookId, session );
		} );

		scope.inTransaction( session -> {
			int numRows = session.createQuery( "update Book set title = :title where id = :id" )
					.setParameter( "title", UPDATED_TITLE )
					.setParameter( "id", bookId )
					.executeUpdate();
			assertEquals( 1, numRows );
			interceptTransaction.set( true );
		} );

		endLatch.await();
		interceptTransaction.set( false );

		scope.inTransaction( session -> {
			loadBook( bookId, session );
		} );
	}

	@Test
	@JiraKey(value = "HHH-13792")
	public void testUpdateNativeQuery(SessionFactoryScope scope) throws InterruptedException {
		bookId = 6L;

		scope.inTransaction( session -> {
			createBook( bookId, session );
		} );

		scope.inTransaction( session -> {
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

		scope.inTransaction( session -> {
			loadBook( bookId, session );
		} );
	}

	private void assertBookNotFound(long bookId, Session session) {
		Book book = session.get( Book.class, bookId );
		assertNull( book );
	}

	private void createBook(long bookId, Session session) {
		Book book = new Book();
		book.setId( bookId );
		book.setTitle( ORIGINAL_TITLE );
		session.persist( book );
	}

	private Consumer<Configuration> getCacheConfig() {
		return configuration -> configuration.setInterceptor( new TransactionInterceptor() );
	}

	private void loadBook(long bookId, Session session) {
		Book book = session.get( Book.class, bookId );
		assertNotNull( book );
		assertEquals( UPDATED_TITLE, book.getTitle(), "Found old value" );
	}

	@Entity(name = "Book")
	@Cacheable
	@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static final class Book {

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

	public static class Configurer implements Consumer<SessionFactoryBuilder> {

		@Override
		public void accept(SessionFactoryBuilder sessionFactoryBuilder) {
			TransactionInterceptor transactionInterceptor = new TransactionInterceptor();
			sessionFactoryBuilder.addSessionFactoryObservers( transactionInterceptor );
			sessionFactoryBuilder.applyInterceptor( transactionInterceptor );
		}
	}

	private static class TransactionInterceptor implements Interceptor, SessionFactoryObserver {
		private org.hibernate.SessionFactory factory;

		@Override
		public void sessionFactoryCreated(org.hibernate.SessionFactory factory) {
			this.factory = factory;
		}

		@Override
		public void beforeTransactionCompletion(Transaction tx) {
			if ( interceptTransaction.get() ) {
				try {
					Session session = factory.openSession();
					Book book = session.get( Book.class, bookId );
					assertNotNull( book );
					session.close();

					assertTrue( factory.getCache()
							.containsEntity( Book.class, bookId ) );
				}
				finally {
					endLatch.countDown();
				}
			}
		}
	}
}
