package org.hibernate.orm.test.collection.bag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				BagWithMappedByTest.Author.class,
				BagWithMappedByTest.Movie.class,
				BagWithMappedByTest.Book.class
		}
)
@SessionFactory
public class BagWithMappedByTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Author author = new Author( 1l, "Fab" );
					Movie firstMovie = new Movie( 1l, "First", author );
					Movie secondMovie = new Movie( 2l, "Second", author );
					Movie thirdMovie = new Movie( 3l, "Third", author );

					Book firstBook = new Book( 4l, "First" );
					Book secondBook = new Book( 5l, "Second" );
					Book thirdBook = new Book( 6l, "Third" );

					author.setBooks( Set.of( firstBook, secondBook, thirdBook ) );

					session.persist( author );

					session.persist( firstMovie );
					session.persist( secondMovie );
					session.persist( thirdMovie );

					session.persist( firstBook );
					session.persist( secondBook );
					session.persist( thirdBook );
				}
		);
	}

	@Test
	public void testFetchingMoviesAndAnotherCollectionDoesNotProduceDuplicates(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					QueryImplementor<Author> query = session.createQuery(
							"select a from Author a left join fetch a.books left join fetch a.movies",
							Author.class
					);
					List<Author> authors = query.list();
					assertThat( authors.size() ).isEqualTo( 1 );

					Author author = authors.get( 0 );
					assertThat( author.getMovies().size() ).isEqualTo( 3 );
					assertThat( author.getBooks().size() ).isEqualTo( 3 );
				}
		);
	}


	@Entity(name = "Author")
	public static class Author {
		@Id
		private Long id;

		private String name;

		@OneToMany
		private Set<Book> books = new HashSet<>();

		@OneToMany(mappedBy = "author")
		private List<Movie> movies = new ArrayList();

		public Author() {
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public void setBooks(Set<Book> books) {
			this.books = books;
		}

		public Set<Book> getBooks() {
			return books;
		}

		public List<Movie> getMovies() {
			return movies;
		}
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Long id;

		private String title;

		public Book() {
		}

		public Book(Long id, String title) {
			this.id = id;
			this.title = title;
		}
	}

	@Entity(name = "Movie")
	public static class Movie {
		@Id
		private Long id;

		private String title;

		@ManyToOne
		private Author author;

		public Movie() {
		}

		public Movie(Long id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
			author.movies.add( this );
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}
	}


}
