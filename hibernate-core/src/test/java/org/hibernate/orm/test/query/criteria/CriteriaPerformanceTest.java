package org.hibernate.orm.test.query.criteria;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.spi.CollectionKey;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertNotNull;

@Ignore
public class CriteriaPerformanceTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Author.class, Book.class, Other.class };
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					for ( int i = 0; i < 1000; i++ ) {
						populateData( session );
					}
					for ( int i = 0; i < 1000; i++ ) {
						populateSimpleData( session );
					}
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Book" ).executeUpdate();
					session.createQuery( "delete from Author" ).executeUpdate();
					session.createQuery( "delete from Other" ).executeUpdate();
				}
		);
	}

	/**
	 * This test demonstrates that fetching entities with associations fires expensive initializers in 6.1 even
	 * if the entities are already present in the 1LC.
	 * <p>
	 * To verify this behavior, set a breakpoint in {@link StatefulPersistenceContext#getCollection(CollectionKey)}}.
	 * In 6.1, breakpoint will be hit for every collection of books associated with each author.
	 * In 5.6, breakpoint will never be hit.
	 */
	@Test
	public void testFetchEntityWithAssociations() {
		inTransaction(
				session -> {

					final Instant startTime = Instant.now();

					final CriteriaBuilder builder = session.getCriteriaBuilder();
					final CriteriaQuery<Author> query = builder.createQuery( Author.class );
					query.from( Author.class );

					final List<Author> authors = session.createQuery( query ).getResultList();
					assertNotNull( authors );


					System.out.println( MessageFormat.format(
							"{0} took {1}",
							"6.1 testFetchEntityWithAssociations ",
							Duration.between( startTime, Instant.now() )
					) );

					assertThat( authors.size() ).isEqualTo( 1000 );
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
		inTransaction(  session -> {
			final Instant startTime = Instant.now();

			for ( int i = 0; i < 100_000; i++ ) {
				final CriteriaBuilder builder = session.getCriteriaBuilder();
				final CriteriaQuery<Author> query = builder.createQuery( Author.class );
				query.from( Author.class );
				final List<Author> authors = session.createQuery( query ).getResultList();
				assertNotNull( authors );
			}

			System.out.println( MessageFormat.format(
					"{0} took {1}",
					"6.1 testFetchEntityWithAssociationsPerformance",
					Duration.between( startTime, Instant.now() )
			) );
		} );
	}


	@Test
	public void testFetchEntityWithAssociationsPerformanceWithParameters() {
		inTransaction(  session -> {
			final Instant startTime = Instant.now();

			for ( int i = 0; i < 100_000; i++ ) {
				final CriteriaBuilder builder = session.getCriteriaBuilder();
				final CriteriaQuery<Author> criteriaQuery = builder.createQuery( Author.class );
				Root<Author> from = criteriaQuery.from( Author.class );
				final ParameterExpression<String> stringValueParameter = builder.parameter( String.class );

				criteriaQuery.where(
						builder.like( from.get( "name" ), stringValueParameter )

				);
				TypedQuery<Author> query = session.createQuery( criteriaQuery );
				query.setParameter( stringValueParameter, "David Gourley" );
				final List<Author> authors = query.getResultList();
				assertNotNull( authors );
			}

			System.out.println( MessageFormat.format(
					"{0} took {1}",
					"6.1 testFetchEntityWithAssociationsPerformanceWithParameters",
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
		inTransaction( session -> {

			final Instant startTime = Instant.now();

			for ( int i = 0; i < 100_000; i++ ) {
				final CriteriaBuilder builder = session.getCriteriaBuilder();
				final CriteriaQuery<Other> query = builder.createQuery( Other.class );
				query.from( Other.class );
				final List<Other> others = session.createQuery( query ).getResultList();
				assertNotNull( others );
			}

			System.out.println( MessageFormat.format(
					"{0} took {1}",
					"6.1 testFetchEntityPerformance",
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

		final Instant startTime = Instant.now();

		for ( int i = 0; i < 100_000; i++ ) {
			inTransaction(  session -> {
				final CriteriaBuilder builder = session.getCriteriaBuilder();
				final CriteriaQuery<Other> query = builder.createQuery( Other.class );
				query.from( Other.class );
				final List<Other> others = session.createQuery( query ).getResultList();
				assertNotNull( others );
			} );
		}

		System.out.println( MessageFormat.format(
				"{0} took {1}",
				"6.1 testFetchEntityPerformanceSmallTransactions",
				Duration.between( startTime, Instant.now() )
		) );
	}

	/**
	 * This test demonstrates the difference in performance for a simple HQL query between 5.6 and 6.1.
	 * (5.6 is about 5-7% faster on my machine)
	 */
	@Test
	public void testFetchEntityPerformanceSmallTransactionsHql() {

		final Instant startTime = Instant.now();

		for ( int i = 0; i < 100_000; i++ ) {
			inTransaction( session -> {
				final List<Other> others = session.createQuery( "SELECT p FROM Other p", Other.class )
						.getResultList();
				assertNotNull( others );
			} );
		}

		System.out.println( MessageFormat.format(
				"{0} took {1}",
				"6.1 testFetchEntityPerformanceSmallTransactionsHql",
				Duration.between( startTime, Instant.now() )
		) );
	}

	public static void populateData(Session session) {
		final Book book = new Book();
		book.name = "HTTP Definitive guide";

		final Author author = new Author();
		author.name = "David Gourley";

		author.books.add( book );
		book.author = author;

		session.persist( author );
	}

	public static void populateSimpleData(Session session) {
		final Other other = new Other();
		other.name = "Other";

		session.persist( other );
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

		public Long getAuthorId() {
			return authorId;
		}

		public String getName() {
			return name;
		}

		public List<Book> getBooks() {
			return books;
		}
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

		public Long getBookId() {
			return bookId;
		}

		public String getName() {
			return name;
		}

		public Author getAuthor() {
			return author;
		}
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