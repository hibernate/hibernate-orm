/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.bag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.loader.BagFetchException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(
		annotatedClasses = {
				BagFetchExceptionTest.Library.class,
				BagFetchExceptionTest.City.class,
				BagFetchExceptionTest.Movie.class,
				BagFetchExceptionTest.Author.class
		}
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-15547")
public class BagFetchExceptionTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Author author1 = new Author( 1l, "Andrea" );
					Author author2 = new Author( 2l, "Rob" );
					Author author3 = new Author( 3l, "Bob" );
					Author author4 = new Author( 4l, "Ronnie" );

					session.persist( author1 );
					session.persist( author2 );
					session.persist( author3 );
					session.persist( author4 );

					Movie movie1 = new Movie( 1l, "little Book", Set.of( author1, author2 ) );
					Movie movie2 = new Movie( 2l, "Big Book", Set.of( author3, author4 ) );

					session.persist( movie1 );
					session.persist( movie2 );

					Library lib = new Library( 1l );
					lib.setBooks( Set.of( "book1", "book2" ) );
					lib.setMovies( List.of( movie1, movie2 ) );
					lib.setMusics( List.of( "movie1", "movie2", "movie3" ) );

					City city = new City( 2l, "London", List.of( "Hackney", "Angel", "Lambeth" ) );
					lib.setCity( city );

					session.persist( city );

					session.persist( lib );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Library" ).executeUpdate();
					session.createMutationQuery( "delete from City" ).executeUpdate();
					session.createMutationQuery( "delete from Movie" ).executeUpdate();
					session.createMutationQuery( "delete from Author" ).executeUpdate();
				}
		);
	}

	@Test
	public void testFetchingSimultaneouslyABagWithAnotherCollectionShouldThrowAnException(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					IllegalArgumentException thrown = assertThrows(
							IllegalArgumentException.class,
							() -> {
								TypedQuery<Library> query = session.createQuery(
										"SELECT l FROM Library AS l "
												+ "LEFT JOIN FETCH l.books "
												+ "LEFT JOIN FETCH l.movies",
										Library.class
								);
								query.getSingleResult();
								// this query would return a Library entities with 4 movies instead of 2
							}
					);

					assertThat( thrown.getCause() ).isInstanceOf( BagFetchException.class );
				}
		);
	}

	@Test
	public void testFetchingABagAndJoiningAnotherCollectionShouldThrowAnException(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					IllegalArgumentException thrown = assertThrows(
							IllegalArgumentException.class,
							() -> {
								TypedQuery<Library> query = session.createQuery(
										"SELECT l FROM Library AS l "
												+ "LEFT JOIN FETCH l.movies "
												+ "LEFT JOIN l.books ",
										Library.class
								);
								query.getSingleResult();
								// this query would return a Library entities with 4 movies instead of 2
							}
					);
					assertThat( thrown.getCause() ).isInstanceOf( BagFetchException.class );
				}
		);
	}

	@Test
	public void testFetchingABag(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Library> query = session.createQuery(
							"SELECT l FROM Library AS l "
									+ "LEFT JOIN FETCH l.movies ",
							Library.class
					);
					Library library = query.getSingleResult();
					assertThat( library ).isNotNull();
					assertThat( library.movies.size() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testFetchingABagAndJoiningABagCollectionShouldThrowAnException(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					IllegalArgumentException thrown = assertThrows(
							IllegalArgumentException.class,
							() -> {
								TypedQuery<Library> query = session.createQuery(
										"SELECT l FROM Library AS l "
												+ "LEFT JOIN FETCH l.movies m "
												+ "LEFT JOIN m.authors",
										Library.class
								);
								query.getResultList();
								// this query would return a Library entities with 4 movies instead of 2
							}
					);
					assertThat( thrown.getCause() ).isInstanceOf( BagFetchException.class );
				}
		);
	}

	@Test
	public void testFetchingABagAndFetchingABagCollectionShouldThrowAnException(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					IllegalArgumentException thrown = assertThrows(
							IllegalArgumentException.class,
							() -> {
								TypedQuery<Library> query = session.createQuery(
										"SELECT l FROM Library AS l "
												+ "LEFT JOIN FETCH l.movies m "
												+ "LEFT JOIN FETCH m.authors",
										Library.class
								);
								query.getSingleResult();

								// this query would return a Library entities with 4 movies instead of 2
							}
					);
					assertThat( thrown.getCause() ).isInstanceOf( BagFetchException.class );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Library library = session.get( Library.class, 1L );
					assertThat( library ).isNotNull();
					assertThat( library.movies.size() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testFetchingABagAndAnAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Library> query = session.createQuery(
							"SELECT l FROM Library AS l "
									+ "LEFT JOIN FETCH l.movies "
									+ "LEFT JOIN FETCH l.city",
							Library.class
					);
					Library library = query.getSingleResult();
					assertThat( library ).isNotNull();
					assertThat( library.movies.size() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testFetchingABagAndAnAssociation2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Library> query = session.createQuery(
							"SELECT l FROM Library AS l "
									+ "LEFT JOIN FETCH l.movies "
									+ "LEFT JOIN FETCH l.city c",
							Library.class
					);
					Library library = query.getSingleResult();
					assertThat( library ).isNotNull();
					assertThat( library.movies.size() ).isEqualTo( 2 );
				}
		);
	}

	@Test
	public void testJoiningABagAndAnAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Library> query = session.createQuery(
							"SELECT l FROM Library AS l "
									+ "LEFT JOIN l.movies "
									+ "LEFT JOIN FETCH l.city",
							Library.class
					);
					Library library = query.getSingleResult();
					assertThat( library ).isNotNull();
					assertThat( library.movies.size() ).isEqualTo( 2 );
				}
		);
	}

	@Entity(name = "Library")
	@Table(name = "LIBRARY_TABLE")
	public static class Library {
		@Id
		private Long id;

		private String description;

		@OneToOne
		private City city;

		@ElementCollection
		private Set<String> books = new HashSet<>();

		@ElementCollection
		private List<String> musics = new ArrayList<>();

		@OneToMany
		private List<Movie> movies = new ArrayList<>();

		public Library() {
		}

		public Library(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public City getCity() {
			return city;
		}

		public void setCity(City city) {
			this.city = city;
		}

		public Set<String> getBooks() {
			return books;
		}

		public void setBooks(Set<String> books) {
			this.books = books;
		}

		public List<String> getMusics() {
			return musics;
		}

		public void setMusics(List<String> musics) {
			this.musics = musics;
		}

		public List<Movie> getMovies() {
			return movies;
		}

		public void setMovies(List<Movie> movies) {
			this.movies = movies;
		}
	}

	@Entity(name = "Movie")
	public static class Movie {
		@Id
		private Long id;

		private String title;

		@OneToMany
		private Set<Author> authors;

		public Movie() {
		}

		public Movie(Long id, String title, Set<Author> authors) {
			this.id = id;
			this.title = title;
			this.authors = authors;
		}

		public Long getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}
	}

	@Entity(name = "Author")
	public static class Author {
		@Id
		private Long id;

		private String name;

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
	}

	@Entity(name = "City")
	public static class City {
		@Id
		private Long id;

		private String name;

		@ElementCollection
		private List<String> councils;

		public City() {
		}

		public City(Long id, String name, List<String> councils) {
			this.id = id;
			this.name = name;
			this.councils = councils;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<String> getCouncils() {
			return councils;
		}
	}
}
