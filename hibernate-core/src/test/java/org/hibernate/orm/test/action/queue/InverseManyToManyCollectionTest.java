package org.hibernate.orm.test.action.queue;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.testing.jdbc.CollectingStatementObserver;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests that changes to the inverse (`mappedBy`) side of a bidirectional
/// many-to-many association are not written to the database.
///
/// @author Thorben Janssen
/// @author Steve Ebersole
@JiraKey("HHH-20996")
@DomainModel(annotatedClasses = {
		InverseManyToManyCollectionTest.Book.class,
		InverseManyToManyCollectionTest.Author.class
})
@SessionFactory(useCollectingStatementObserver = true)
public class InverseManyToManyCollectionTest {

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author author = new Author( 1L, "Author" );
			final Book book1 = new Book( 1L, "Book 1" );
			final Book book2 = new Book( 2L, "Book 2" );
			book1.authors.add( author );
			book2.authors.add( author );
			author.books.add( book1 );
			author.books.add( book2 );
			session.persist( author );
			session.persist( book1 );
			session.persist( book2 );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testChangeOfInverseSideIsIgnored(SessionFactoryScope scope) {
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();

		scope.inTransaction( session -> {
			final Author author = session.find( Author.class, 1L );
			final Book book1 = session.find( Book.class, 1L );
			observer.clear();

			// only the inverse side gets changed, Book.authors still contains the Author
			author.books.remove( book1 );
		} );

		// the owning side is the source of truth, so the association must still exist
		scope.inTransaction( session ->
				assertThat( session.find( Book.class, 1L ).authors ).extracting( a -> a.id ).containsExactly( 1L )
		);
		assertThat( observer.getSqlQueries() ).filteredOn( sql -> sql.startsWith( "delete" ) ).isEmpty();
	}

	@Test
	public void testChangeOfBothSidesDeletesRowOnce(SessionFactoryScope scope) {
		final CollectingStatementObserver observer = scope.getCollectingStatementObserver();

		scope.inTransaction( session -> {
			final Author author = session.find( Author.class, 1L );
			final Book book1 = session.find( Book.class, 1L );
			// initialize both collections before recording the statements
			assertThat( author.books ).hasSize( 2 );
			assertThat( book1.authors ).hasSize( 1 );
			observer.clear();

			author.books.remove( book1 );
			book1.authors.remove( author );
		} );

		// one row of the join table gets removed by the owning side Book.authors
		assertThat( observer.getSqlQueries() ).filteredOn( sql -> sql.startsWith( "delete" ) ).hasSize( 1 );
		scope.inTransaction( session -> {
			assertThat( session.find( Book.class, 1L ).authors ).isEmpty();
			assertThat( session.find( Author.class, 1L ).books ).extracting( b -> b.id ).containsExactly( 2L );
		} );
	}

	@Test
	public void testInverseClearIsIgnored(SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final var author = session.find( Author.class, 1L );
			assertThat( author.books ).hasSize( 2 );
			observer.clear();
			author.books.clear();
		} );
		assertThat( observer.getSqlQueries() ).filteredOn( sql -> sql.startsWith( "delete" ) ).isEmpty();
		scope.inTransaction( session ->
				assertThat( session.find( Author.class, 1L ).books ).hasSize( 2 ) );
	}

	@Test
	public void testOwningSideRemovalDeletesOnce(SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final var book = session.find( Book.class, 1L );
			assertThat( book.authors ).hasSize( 1 );
			observer.clear();
			book.authors.clear();
		} );
		assertThat( observer.getSqlQueries() ).filteredOn( sql -> sql.startsWith( "delete" ) ).hasSize( 1 );
		scope.inTransaction( session -> {
			assertThat( session.find( Book.class, 1L ).authors ).isEmpty();
			assertThat( session.find( Author.class, 1L ).books ).extracting( b -> b.id ).containsExactly( 2L );
		} );
	}

	@Entity(name = "Book")
	@Table(name = "Book")
	public static class Book {
		@Id
		private Long id;

		private String title;

		@ManyToMany
		@JoinTable(name = "BookAuthor",
				joinColumns = @JoinColumn(name = "bookId"),
				inverseJoinColumns = @JoinColumn(name = "authorId"))
		private Set<Author> authors = new HashSet<>();

		public Book() {
		}

		public Book(Long id, String title) {
			this.id = id;
			this.title = title;
		}
	}

	@Entity(name = "Author")
	@Table(name = "Author")
	public static class Author {
		@Id
		private Long id;

		private String name;

		@ManyToMany(mappedBy = "authors")
		private Set<Book> books = new HashSet<>();

		public Author() {
		}

		public Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
