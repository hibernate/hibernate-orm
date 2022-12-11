package org.hibernate.orm.test.query.criteria;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

@Ignore("Manual performance test")
public class CriteriaPerformanceTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				CriteriaPerformanceTest.Author.class,
				CriteriaPerformanceTest.Book.class,
				CriteriaPerformanceTest.Other.class
		};
	}

	/**
	 * This test demonstrates that fetching entities with associations fires expensive initializers in 6.1 even
	 * if the entities are already present in the 1LC.
	 * <p>
	 * To verify this behavior, set a breakpoint in {@link StatefulPersistenceContext#getCollection(CollectionKey)}.
	 * In 6.1, breakpoint will be hit for every collection of books associated with each author.
	 * In 5.6, breakpoint will never be hit.
	 */
	@Test
	public void testFetchEntityWithAssociations() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 0; i < 1000; i++ ) {
				populateData( entityManager );
			}

			final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Author> query = builder.createQuery( Author.class );
			query.from( Author.class );

			final List<Author> author = entityManager.createQuery( query ).getResultList();
			assertNotNull( author );
		} );
	}

	/**
	 * This test demonstrates the difference in performance for a simple criteria query between 5.6 and 6.1.
	 * (5.6 is about 30% faster on my machine)
	 * <p>
	 * The difference in performance seems to have two main causes:
	 * <p>
	 * 1. Elevated access to the persistence context as demonstrated by the test above
	 * 2. Missing query plan cache for criteria queries (possibly)
	 */
	@Test
	public void testFetchEntityWithAssociationsPerformance() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 0; i < 1000; i++ ) {
				populateData( entityManager );
			}

			final Instant startTime = Instant.now();

			for ( int i = 0; i < 100_000; i++ ) {
				final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
				final CriteriaQuery<Author> query = builder.createQuery( Author.class );
				query.from( Author.class );
				final List<Author> authors = entityManager.createQuery( query ).getResultList();
				assertNotNull( authors );
			}

			System.out.println( MessageFormat.format(
					"{0} took {1}",
					"Query",
					Duration.between( startTime, Instant.now() )
			) );
		} );
	}

	/**
	 * This test demonstrates the difference in performance for a simple criteria query between 5.6 and 6.1.
	 * (5.6 is about 30% faster on my machine)
	 * <p>
	 * The difference in performance seems to have two main causes:
	 * <p>
	 * 1. Elevated access to the persistence context as demonstrated by the test above
	 * 2. Missing query plan cache for criteria queries (possibly)
	 */
	@Test
	public void testFetchEntityPerformance() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 0; i < 1000; i++ ) {
				populateSimpleData( entityManager );
			}

			final Instant startTime = Instant.now();

			for ( int i = 0; i < 100_000; i++ ) {
				final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
				final CriteriaQuery<Other> query = builder.createQuery( Other.class );
				query.from( Other.class );
				final List<Other> others = entityManager.createQuery( query ).getResultList();
				assertNotNull( others );
			}

			System.out.println( MessageFormat.format(
					"{0} took {1}",
					"Simple Query",
					Duration.between( startTime, Instant.now() )
			) );
		} );
	}

	/**
	 * This test demonstrates the difference in performance for a simple criteria query between 5.6 and 6.1.
	 * (5.6 is about 5-7% faster on my machine)
	 */
	@Test
	public void testFetchEntityPerformanceSmallTransactions() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 0; i < 1000; i++ ) {
				populateSimpleData( entityManager );
			}
		} );

		final Instant startTime = Instant.now();

		for ( int i = 0; i < 100_000; i++ ) {
			doInJPA( this::entityManagerFactory, entityManager -> {
				final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
				final CriteriaQuery<Other> query = builder.createQuery( Other.class );
				query.from( Other.class );
				final List<Other> others = entityManager.createQuery( query ).getResultList();
				assertNotNull( others );
			} );
		}

		System.out.println( MessageFormat.format(
				"{0} took {1}",
				"Simple Query Criteria",
				Duration.between( startTime, Instant.now() )
		) );
	}

	/**
	 * This test demonstrates the difference in performance for a simple HQL query between 5.6 and 6.1.
	 * (5.6 is about 5-7% faster on my machine)
	 */
	@Test
	public void testFetchEntityPerformanceSmallTransactionsHql() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 0; i < 1000; i++ ) {
				populateSimpleData( entityManager );
			}
		} );

		final Instant startTime = Instant.now();

		for ( int i = 0; i < 100_000; i++ ) {
			doInJPA( this::entityManagerFactory, entityManager -> {
				final List<Other> others = entityManager.createQuery( "SELECT p FROM Other p", Other.class )
						.getResultList();
				assertNotNull( others );
			} );
		}

		System.out.println( MessageFormat.format(
				"{0} took {1}",
				"Simple Query HQL",
				Duration.between( startTime, Instant.now() )
		) );
	}

	public void populateData(EntityManager entityManager) {
		final Book book = new Book();
		book.name = "HTTP Definitive guide";

		final Author author = new Author();
		author.name = "David Gourley";

		author.books.add( book );
		book.author = author;

		entityManager.persist( author );
	}

	public void populateSimpleData(EntityManager entityManager) {
		final Other other = new Other();
		other.name = "Other";

		entityManager.persist( other );
	}

	@Entity(name = "Author")
	@Table(name = "Author")
	public static class Author {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long authorId;

		@Column
		public String name;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
		public List<Book> books = new ArrayList<>();
	}

	@Entity(name = "Book")
	@Table(name = "Book")
	public static class Book {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long bookId;

		@Column
		public String name;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "author_id", nullable = false)
		public Author author;
	}

	@Entity(name = "Other")
	@Table(name = "Other")
	public static class Other {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long otherId;

		@Column
		public String name;
	}
}
