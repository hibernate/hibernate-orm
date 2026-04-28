/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.cfg.QuerySettings;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pagination of a many-to-many fetch join must still page authors rather than
 * the cartesian product of authors, join-table rows, and books.
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationManyToManyTest.Author.class,
		CollectionFetchPaginationManyToManyTest.Book.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory(useCollectingStatementInspector = true)
@SkipForDialect(dialectClass = SybaseASEDialect.class)
public class CollectionFetchPaginationManyToManyTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book0 = new Book( "ssn-0", "Book 0" );
			final var book1 = new Book( "ssn-1", "Book 1" );
			final var book2 = new Book( "ssn-2", "Book 2" );
			final var book3 = new Book( "ssn-3", "Book 3" );
			final var book4 = new Book( "ssn-4", "Book 4" );

			final var author0 = new Author( 0L, "Author 0" );
			author0.addBook( book0 );
			author0.addBook( book1 );
			author0.addBook( book2 );

			final var author1 = new Author( 1L, "Author 1" );
			author1.addBook( book1 );
			author1.addBook( book2 );
			author1.addBook( book3 );

			final var author2 = new Author( 2L, "Author 2" );
			author2.addBook( book2 );
			author2.addBook( book3 );
			author2.addBook( book4 );

			final var author3 = new Author( 3L, "Author 3" );
			author3.addBook( book0 );
			author3.addBook( book3 );
			author3.addBook( book4 );

			final var author4 = new Author( 4L, "Author 4" );

			session.persist( author0 );
			session.persist( author1 );
			session.persist( author2 );
			session.persist( author3 );
			session.persist( author4 );

			session.persist( book0 );
			session.persist( book1 );
			session.persist( book2 );
			session.persist( book3 );
			session.persist( book4 );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void fetchJoinWithMaxResults(SessionFactoryScope scope) {
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			sql.clear();

			final var authors = session.createSelectionQuery(
					"from Author join fetch books order by authorId",
					Author.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, authors.size() );
			assertEquals( 0L, authors.get( 0 ).getAuthorId() );
			assertEquals( 1L, authors.get( 1 ).getAuthorId() );
			assertEquals( 3, authors.get( 0 ).getBooks().size() );
			assertEquals( 3, authors.get( 1 ).getBooks().size() );
			assertTrue( authors.get( 0 ).containsBook( "ssn-0" ) );
			assertTrue( authors.get( 0 ).containsBook( "ssn-1" ) );
			assertTrue( authors.get( 0 ).containsBook( "ssn-2" ) );
			assertTrue( authors.get( 1 ).containsBook( "ssn-1" ) );
			assertTrue( authors.get( 1 ).containsBook( "ssn-2" ) );
			assertTrue( authors.get( 1 ).containsBook( "ssn-3" ) );

			assertEquals( 1, sql.getSqlQueries().size() );
			final String generated = normalized( sql.getSqlQueries().get( 0 ) );
			assertTrue( generated.contains( "from (select" ) );
			assertTrue( generated.contains( "author_book_link" ) );
			assertTrue( generated.contains( "book_entity" ) );
			var dialect = scope.getSessionFactory().getJdbcServices().getDialect();
			if ( !(dialect instanceof HSQLDialect ) && !(dialect instanceof MariaDBDialect) ) {
				final int existsStart = generated.indexOf( "exists(select 1 from author_book_link" );
				final int existsWhere = generated.indexOf( " where", existsStart );
				assertTrue( existsStart >= 0 );
				assertTrue( existsWhere > existsStart );
				assertFalse( generated.substring( existsStart, existsWhere ).contains( "book_entity" ) );
			}
		} );
	}

	@Test
	void fetchLeftJoinWithMaxResults(SessionFactoryScope scope) {
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			sql.clear();

			final var authors = session.createSelectionQuery(
					"from Author left join fetch books order by authorId",
					Author.class
			).setFirstResult( 3 ).setMaxResults( 2 ).list();

			assertEquals( 2, authors.size() );
			assertEquals( 3L, authors.get( 0 ).getAuthorId() );
			assertEquals( 4L, authors.get( 1 ).getAuthorId() );
			assertEquals( 3, authors.get( 0 ).getBooks().size() );
			assertEquals( 0, authors.get( 1 ).getBooks().size() );

			assertEquals( 1, sql.getSqlQueries().size() );
			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertTrue( generated.contains( "from (select" ) );
			assertTrue( generated.contains( "author_book_link" ) );
			assertTrue( generated.contains( "book_entity" ) );
		} );
	}

	private static String normalized(String sql) {
		return sql.toLowerCase( Locale.ROOT ).replaceAll( "\\s+", " " );
	}

	@Entity(name = "Author")
	@Table(name = "author_entity")
	public static class Author {
		@Id
		private Long authorId;
		private String name;

		@ManyToMany(mappedBy = "authors")
		private Set<Book> books = new HashSet<>();

		public Author() {
		}

		public Author(Long id, String name) {
			this.authorId = id;
			this.name = name;
		}

		public Long getAuthorId() {
			return authorId;
		}

		public Set<Book> getBooks() {
			return books;
		}

		public void addBook(Book book) {
			books.add( book );
			book.authors.add( this );
		}

		public boolean containsBook(String bookSsn) {
			return books.stream().anyMatch( book -> book.ssn.equals( bookSsn ) );
		}
	}

	@Entity(name = "Book")
	@Table(name = "book_entity")
	public static class Book {
		@Id
		private String ssn;
		private String title;

		@ManyToMany
		@JoinTable(
				name = "author_book_link",
				joinColumns = @JoinColumn(name = "book_id"),
				inverseJoinColumns = @JoinColumn(name = "author_id")
		)
		private Set<Author> authors = new HashSet<>();

		public Book() {
		}

		public Book(String ssn, String title) {
			this.ssn = ssn;
			this.title = title;
		}
	}
}
