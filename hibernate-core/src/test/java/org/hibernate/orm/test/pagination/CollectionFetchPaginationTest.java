/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.cfg.QuerySettings;

import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Pagination of a fetch join over a many-valued association must apply the
 * limit/offset to the parent entities (not to the cartesian product of SQL
 * rows). The intended strategy is to push the limit into a derived table
 * containing only the root, and place the fetch joins outside it, e.g.:
 * <pre>
 * select ... from (select * from Book b ... limit ?,?) b_
 *                 left join Author a_ on a_.book_isbn = b_.isbn
 * </pre>
 * The previous behaviour of fetching every row and slicing the parent list
 * in memory must no longer fire — verified here with
 * {@link QuerySettings#FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH} switched on,
 * which throws when the in-memory fallback would otherwise be applied.
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationTest.Book.class,
		CollectionFetchPaginationTest.Author.class,
		CollectionFetchPaginationTest.Publisher.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory(useCollectingStatementInspector = true)
@SkipForDialect( dialectClass = SybaseASEDialect.class )
public class CollectionFetchPaginationTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final Publisher acme = new Publisher( 1L, "Acme" );
			final Publisher zenith = new Publisher( 2L, "Zenith" );
			s.persist( acme );
			s.persist( zenith );
			for ( int i = 0; i < 5; i++ ) {
				final Book b = new Book( "isbn-" + i, "Book " + i );
				b.setPublisher( i % 2 == 0 ? acme : zenith );
				for ( int j = 0; j < 3; j++ ) {
					b.addAuthor( new Author( i * 10L + j, "Author " + i + "/" + j ) );
				}
				s.persist( b );
			}
			// Two extra books with no authors — used by innerFetchJoin* tests to
			// verify that 'inner join fetch' filters them out even after pagination
			// is pushed into the derived table.
			s.persist( new Book( "no-authors-1", "Lonely 1" ) );
			s.persist( new Book( "no-authors-2", "Lonely 2" ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void fetchJoinWithMaxResults(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Book> books = s.createSelectionQuery(
					"from Book b left join fetch b.authors order by b.isbn",
					Book.class
			).setMaxResults( 2 ).list();

			assertThat( books.size(), is( 2 ) );
			assertThat( books.get( 0 ).getIsbn(), is( "isbn-0" ) );
			assertThat( books.get( 1 ).getIsbn(), is( "isbn-1" ) );
			assertThat( books.get( 0 ).getAuthors().size(), is( 3 ) );
			assertThat( books.get( 1 ).getAuthors().size(), is( 3 ) );

			assertThat( sql.getSqlQueries().size(), is( 1 ) );
			// limit must be inside a derived table, not on the outer fetch join
			assertThat( sql.getSqlQueries().get( 0 ).toLowerCase(), containsString( "from (select" ) );
		} );
	}

	@Test
	void fetchJoinWithFirstAndMaxResults(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final List<Book> books = s.createSelectionQuery(
					"from Book b left join fetch b.authors order by b.isbn",
					Book.class
			).setFirstResult( 1 ).setMaxResults( 2 ).list();

			assertThat( books.size(), is( 2 ) );
			assertThat( books.get( 0 ).getIsbn(), is( "isbn-1" ) );
			assertThat( books.get( 1 ).getIsbn(), is( "isbn-2" ) );
			assertThat( books.get( 0 ).getAuthors().size(), is( 3 ) );
			assertThat( books.get( 1 ).getAuthors().size(), is( 3 ) );
		} );
	}

	@Test
	void fetchJoinWithMaxResultsAndRootPredicate(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final List<Book> books = s.createSelectionQuery(
					"from Book b left join fetch b.authors where b.isbn like 'isbn-%' order by b.isbn",
					Book.class
			).setMaxResults( 3 ).list();

			assertThat( books.size(), is( 3 ) );
			assertThat( books.get( 0 ).getIsbn(), is( "isbn-0" ) );
			assertThat( books.get( 2 ).getIsbn(), is( "isbn-2" ) );
			for ( Book b : books ) {
				assertThat( b.getAuthors().size(), is( 3 ) );
			}
		} );
	}

	/**
	 * {@code inner join fetch} acts as a parent-row filter: only books that have
	 * at least one author should appear. After the fetch join moves to the outer,
	 * the rewrite preserves that filter by adding an {@code EXISTS} subquery to
	 * the inner — otherwise the inner pagination could pick up books with no
	 * authors and the outer inner-join would silently drop them, leaving fewer
	 * than {@code maxResults} rows in the result.
	 */
	@Test
	void innerFetchJoinWithMaxResults(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			// "no-authors-1" and "no-authors-2" come first when ordered by isbn —
			// without the EXISTS filter in the inner, they'd be picked up by the
			// inner LIMIT and the outer inner-join would drop them, leaving 0
			// results. With the filter, the inner skips them and we get isbn-0/1.
			final List<Book> books = s.createSelectionQuery(
					"from Book b inner join fetch b.authors order by b.isbn",
					Book.class
			).setMaxResults( 2 ).list();

			assertThat( books.size(), is( 2 ) );
			assertThat( books.get( 0 ).getIsbn(), is( "isbn-0" ) );
			assertThat( books.get( 1 ).getIsbn(), is( "isbn-1" ) );
			for ( Book b : books ) {
				assertThat( b.getAuthors().size(), is( 3 ) );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertThat( generated, containsString( "from (select" ) );
			// The inner derived table carries an EXISTS predicate to mirror the
			// inner-join filter that's now sitting on the outer.
			assertThat( generated, containsString( "exists" ) );
		} );
	}

	/**
	 * A plural fetch alongside a singular {@code @ManyToOne} fetch. The
	 * singular fetch is also moved to the outer query (it would otherwise leave
	 * dangling references in the outer SELECT, since the inner only projects
	 * the root's primary-table columns).
	 */
	@Test
	void fetchJoinWithSingularFetchToo(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Book> books = s.createSelectionQuery(
					"from Book b left join fetch b.publisher left join fetch b.authors order by b.isbn",
					Book.class
			).setMaxResults( 2 ).list();

			assertThat( books.size(), is( 2 ) );
			assertThat( books.get( 0 ).getIsbn(), is( "isbn-0" ) );
			assertThat( books.get( 1 ).getIsbn(), is( "isbn-1" ) );
			for ( Book b : books ) {
				assertThat( b.getAuthors().size(), is( 3 ) );
				assertThat( b.getPublisher(), org.hamcrest.CoreMatchers.notNullValue() );
				assertThat( b.getPublisher().getName(),
						is( b == books.get( 0 ) ? "Acme" : "Zenith" ) );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertThat( generated, containsString( "from (select" ) );
			// publisher and author joins both attached to the outer derived table
			assertThat( generated, containsString( "publisher" ) );
			assertThat( generated, containsString( "author" ) );
		} );
	}

	/**
	 * Two root entities, paginated. Both roots end up in the inner derived
	 * table; {@code Publisher}'s columns are absorbed (renamed in the column
	 * list) and the outer's references to {@code p1_0.*} are rewritten to use
	 * the derived table.
	 */
	@Test
	void fetchJoinWithMultipleRoots(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			record BookPublisher(Book book, Publisher publisher) {}

			final List<BookPublisher> rows = s.createSelectionQuery(
					"select b, p from Book b left join fetch b.authors, Publisher p "
							+ "where b.publisher = p order by b.isbn",
					BookPublisher.class
			).setMaxResults( 2 ).list()
					.stream().distinct().toList();

			assertThat( rows.size(), is( 2 ) );
			assertThat( rows.get( 0 ).book().getIsbn(), is( "isbn-0" ) );
			assertThat( rows.get( 1 ).book().getIsbn(), is( "isbn-1" ) );
			assertThat( rows.get( 0 ).publisher().getName(), is( "Acme" ) );
			assertThat( rows.get( 1 ).publisher().getName(), is( "Zenith" ) );

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertThat( generated, containsString( "from (select" ) );
		} );
	}

	/**
	 * Non-fetch association join {@code join b.publisher p} alongside a plural
	 * fetch. The publisher is only used as a filter (not loaded), so it stays
	 * inside the inner derived table while the authors fetch moves to the outer.
	 */
	@Test
	void nonFetchJoinPlusFetchJoin(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			final List<Book> books = s.createSelectionQuery(
					"from Book b join b.publisher p left join fetch b.authors "
							+ "where p.name = 'Acme' order by b.isbn",
					Book.class
			).setMaxResults( 2 ).list();

			assertThat( books.size(), is( 2 ) );
			assertThat( books.get( 0 ).getIsbn(), is( "isbn-0" ) );
			assertThat( books.get( 1 ).getIsbn(), is( "isbn-2" ) );
			for ( Book b : books ) {
				assertThat( b.getAuthors().size(), is( 3 ) );
			}

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertThat( generated, containsString( "from (select" ) );
			// The non-fetch publisher join must remain inside the derived table —
			// the outer query refers to it only via the WHERE predicate which moved
			// down to the inner.
			final int derivedClose = generated.indexOf( ')' );
			final String inner = generated.substring( 0, derivedClose );
			final String outer = generated.substring( derivedClose );
			assertThat( inner, containsString( "publisher" ) );
			assertThat( outer, containsString( "author" ) );
		} );
	}

	/**
	 * Non-fetch association join {@code join b.publisher p} alongside a plural
	 * fetch. The publisher is only used as a filter (not loaded), so it stays
	 * inside the inner derived table while the authors fetch moves to the outer.
	 */
	@Test
	void nonFetchJoinPlusFetchJoin2(SessionFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();
		scope.inTransaction( s -> {
			sql.clear();

			record BookPublisher(Book book, Publisher publisher) {}

			final List<BookPublisher> books = s.createSelectionQuery(
					"select b, p from Book b join b.publisher p left join fetch b.authors "
					+ "where p.name = 'Acme' order by b.isbn",
					BookPublisher.class
			).setMaxResults( 2 ).list()
					.stream().distinct().toList();

			assertThat( books.size(), is( 2 ) );
			assertThat( books.get( 0 ).book().getIsbn(), is( "isbn-0" ) );
			assertThat( books.get( 1 ).book().getIsbn(), is( "isbn-2" ) );
			for ( var b : books ) {
				assertThat( b.book().getAuthors().size(), is( 3 ) );
			}
			assertThat( books.get( 0 ).publisher().getName(), is( "Acme" ) );
			assertThat( books.get( 1 ).publisher().getName(), is( "Acme" ) );

			final String generated = sql.getSqlQueries().get( 0 ).toLowerCase();
			assertThat( generated, containsString( "from (select" ) );
			// The non-fetch publisher join must remain inside the derived table —
			// the outer query refers to it only via the WHERE predicate which moved
			// down to the inner.
			final int derivedClose = generated.indexOf( ')' );
			final String inner = generated.substring( 0, derivedClose );
			final String outer = generated.substring( derivedClose );
			assertThat( inner, containsString( "publisher" ) );
			assertThat( outer, containsString( "author" ) );
		} );
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private String isbn;
		private String title;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "publisher_id")
		private Publisher publisher;
		@OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
		private List<Author> authors = new ArrayList<>();

		public Book() {
		}

		public Book(String isbn, String title) {
			this.isbn = isbn;
			this.title = title;
		}

		@Override
		public String toString() {
			return isbn;
		}

		public String getIsbn() {
			return isbn;
		}

		public String getTitle() {
			return title;
		}

		public Publisher getPublisher() {
			return publisher;
		}

		public void setPublisher(Publisher publisher) {
			this.publisher = publisher;
		}

		public List<Author> getAuthors() {
			return authors;
		}

		public void addAuthor(Author a) {
			authors.add( a );
			a.setBook( this );
		}
	}

	@Entity(name = "Publisher")
	public static class Publisher {
		@Id
		private Long id;
		private String name;

		@Override
		public String toString() {
			return name;
		}

		public Publisher() {
		}

		public Publisher(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		private Long id;
		private String name;
		@ManyToOne
		@JoinColumn(name = "book_isbn")
		private Book book;

		@Override
		public String toString() {
			return name;
		}

		public Author() {
		}

		public Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Book getBook() {
			return book;
		}

		public void setBook(Book b) {
			this.book = b;
		}
	}
}
