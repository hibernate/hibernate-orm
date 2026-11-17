package org.hibernate.orm.test.query.criteria;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Count and exists queries derived from a criteria query which has
 * no explicit selection, and so implicitly selects its single root.
 */
@DomainModel(annotatedClasses = {
		CriteriaImplicitSelectionCountTest.Author.class,
		CriteriaImplicitSelectionCountTest.Book.class
})
@SessionFactory
@JiraKey("HHH-21153")
public class CriteriaImplicitSelectionCountTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author first = new Author( 1L, "Gavin" );
			final Author second = new Author( 2L, "Steve" );
			session.persist( first );
			session.persist( second );
			session.persist( new Author( 3L, "Nobody" ) );
			session.persist( new Book( 1L, "Hibernate in Action", first ) );
			session.persist( new Book( 2L, "Java Persistence with Hibernate", first ) );
			session.persist( new Book( 3L, "Hibernate Internals", second ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testResultCountOfDistinctQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final JpaCriteriaQuery<Author> query = distinctAuthorsWithBooks( session.getCriteriaBuilder() );
			assertEquals( 2L, session.createSelectionQuery( query ).getResultCount() );
		} );
	}

	@Test
	public void testCountQueryOfDistinctQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final JpaCriteriaQuery<Author> query = distinctAuthorsWithBooks( session.getCriteriaBuilder() );
			assertEquals( 2L, session.createQuery( query.createCountQuery() ).getSingleResult() );
		} );
	}

	@Test
	public void testResultCountOfGroupedQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final JpaCriteriaQuery<Author> query = groupedAuthorsWithBooks( session.getCriteriaBuilder() );
			assertEquals( 2L, session.createSelectionQuery( query ).getResultCount() );
		} );
	}

	@Test
	public void testCountQueryOfGroupedQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final JpaCriteriaQuery<Author> query = groupedAuthorsWithBooks( session.getCriteriaBuilder() );
			assertEquals( 2L, session.createQuery( query.createCountQuery() ).getSingleResult() );
		} );
	}

	@Test
	public void testExistsQueryOfGroupedQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final JpaCriteriaQuery<Author> query = groupedAuthorsWithBooks( session.getCriteriaBuilder() );
			assertTrue( session.createQuery( query.createExistsQuery() ).getSingleResult() );
		} );
	}

	@Test
	public void testCountQueryOfDistinctQueryWithTwoRoots(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Author> query = cb.createQuery( Author.class );
			query.from( Author.class );
			query.from( Book.class );
			// no explicit selection, so which root is selected is ambiguous
			query.distinct( true );
			final IllegalArgumentException directFailure =
					assertThrows( IllegalArgumentException.class, () -> session.createQuery( query ) );
			final IllegalArgumentException countFailure =
					assertThrows( IllegalArgumentException.class, query::createCountQuery );
			assertEquals( directFailure.getMessage(), countFailure.getMessage() );
		} );
	}

	private static JpaCriteriaQuery<Author> distinctAuthorsWithBooks(HibernateCriteriaBuilder cb) {
		final JpaCriteriaQuery<Author> query = cb.createQuery( Author.class );
		final JpaRoot<Author> author = query.from( Author.class );
		author.join( "books" );
		// no explicit selection
		query.distinct( true ).orderBy( cb.asc( author.get( "id" ) ) );
		return query;
	}

	private static JpaCriteriaQuery<Author> groupedAuthorsWithBooks(HibernateCriteriaBuilder cb) {
		final JpaCriteriaQuery<Author> query = cb.createQuery( Author.class );
		final JpaRoot<Author> author = query.from( Author.class );
		author.join( "books" );
		// no explicit selection
		query.groupBy( author );
		return query;
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		private Long id;
		private String name;
		@OneToMany(mappedBy = "author")
		private List<Book> books = new ArrayList<>();

		public Author() {
		}

		public Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Long id;
		private String title;
		@ManyToOne
		private Author author;

		public Book() {
		}

		public Book(Long id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}
	}
}
