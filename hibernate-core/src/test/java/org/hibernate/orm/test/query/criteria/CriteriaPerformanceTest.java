package org.hibernate.orm.test.query.criteria;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

//@Disabled("Manual performance test")
@Jpa(
		annotatedClasses = {
				CriteriaPerformanceTest.Author.class,
				CriteriaPerformanceTest.Book.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "10")
		}
)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
public class CriteriaPerformanceTest {

	@Test
	public void testFetchEntityWithAssociationsPerformance(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			for ( int i = 0; i < 1000; i++ ) {
				populateData( entityManager );
			}
		} );
		scope.inTransaction( entityManager -> {
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
	public void testFetchEntityWithAssociationsPerformanceSmallTransactions(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			for ( int i = 0; i < 1000; i++ ) {
				populateData( entityManager );
			}
		} );

		final Instant startTime = Instant.now();

		for ( int i = 0; i < 1_000; i++ ) {
			scope.inTransaction( entityManager -> {
				final List<Author> authors = entityManager.createQuery( "from Author", Author.class )
						.getResultList();
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

	@AfterEach
	public void cleanUpTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "delete Book" ).executeUpdate();
			session.createQuery( "delete Author" ).executeUpdate();
		} );
	}

	public void populateData(EntityManager entityManager) {
		final Book book = new Book();
		book.name = "HTTP Definitive guide";

		final Author author = new Author();
		author.name = "David Gourley";

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
