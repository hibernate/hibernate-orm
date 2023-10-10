package org.hibernate.orm.test.query.criteria;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.FlushModeType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.loader.BatchFetchStyle;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class CriteriaPerformanceTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				CriteriaPerformanceTest.Author.class,
				CriteriaPerformanceTest.AuthorDetails.class,
				CriteriaPerformanceTest.Book.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.DYNAMIC );
		options.put( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, 10 );
	}

	@Test
	public void testFetchEntityWithAssociationsPerformance() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "delete Author" ).executeUpdate();
			entityManager.createQuery( "delete AuthorDetails" ).executeUpdate();
			entityManager.createQuery( "delete Book" ).executeUpdate();
			for ( int i = 0; i < 1000; i++ ) {
				populateData( entityManager );
			}
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.setFlushMode( FlushModeType.COMMIT );

			final Instant startTime = Instant.now();

			for ( int i = 0; i < 100_000; i++ ) {
				final List<Author> authors = entityManager.createQuery( "from Author", Author.class ).getResultList();
				assertNotNull( authors );
				authors.forEach( author -> assertFalse( author.books.isEmpty() ) );
			}

			System.out.println( MessageFormat.format(
					"{0} took {1}",
					"Query",
					Duration.between( startTime, Instant.now() )
			) );
		} );
	}

	@Test
	public void testFetchEntityWithAssociationsPerformanceSmallTransactions() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "delete Author" ).executeUpdate();
			entityManager.createQuery( "delete AuthorDetails" ).executeUpdate();
			entityManager.createQuery( "delete Book" ).executeUpdate();
			for ( int i = 0; i < 1000; i++ ) {
				populateData( entityManager );
			}
		} );

		final Instant startTime = Instant.now();

		for ( int i = 0; i < 1_000; i++ ) {
			doInJPA( this::entityManagerFactory, entityManager -> {
				final List<Author> authors = entityManager.createQuery( "from Author", Author.class ).getResultList();
				assertNotNull( authors );
				authors.forEach( author -> assertFalse( author.books.isEmpty() ) );
			} );
		}

		System.out.println( MessageFormat.format(
				"{0} took {1}",
				"Query",
				Duration.between( startTime, Instant.now() )
		) );
	}

	public void populateData(EntityManager entityManager) {
		final Book book = new Book();
		book.name = "HTTP Definitive guide";

		final Author author = new Author();
		author.name = "David Gourley";

		final AuthorDetails details = new AuthorDetails();
		details.name = "Author Details";
		details.author = author;
		author.details = details;

		author.books.add( book );
		book.author = author;

		entityManager.persist( author );
		entityManager.persist( book );
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

		@OneToOne(fetch = FetchType.EAGER, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
		public AuthorDetails details;

	}

	@Entity(name = "AuthorDetails")
	@Table(name = "AuthorDetails")
	public static class AuthorDetails {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long detailsId;

		@Column
		public String name;

		@OneToOne(fetch = FetchType.LAZY, mappedBy = "details", optional = false)
		public Author author;
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
}
