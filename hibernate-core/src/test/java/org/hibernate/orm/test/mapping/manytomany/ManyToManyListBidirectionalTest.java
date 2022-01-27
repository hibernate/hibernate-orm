/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.manytomany;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

@DomainModel(
		annotatedClasses = {
				ManyToManyListBidirectionalTest.Book.class,
				ManyToManyListBidirectionalTest.Author.class
		}
)
@SessionFactory
@ServiceRegistry
@SuppressWarnings( "unused" )
public class ManyToManyListBidirectionalTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Author author1 = new Author( 1 );
			final Author author2 = new Author( 2 );

			final Book bookByAuthor1 = new Book( 1 );
			bookByAuthor1.addAuthor( author1 );

			final Book bookByAuthor2 = new Book( 2 );
			bookByAuthor2.addAuthor( author2 );

			final Book bookByAuthor1AndAuthor2 = new Book( 3 );
			bookByAuthor1AndAuthor2.addAuthor( author1 );
			bookByAuthor1AndAuthor2.addAuthor( author2 );

			session.persist( author1 );
			session.persist( author2 );
			session.persist( bookByAuthor1 );
			session.persist( bookByAuthor2 );
			session.persist( bookByAuthor1AndAuthor2 );
		} );

		scope.inTransaction( session -> {
			assertThat( session.createQuery( "from Book b", Book.class ).list() )
					.hasSize( 3 )
					.allSatisfy( book -> assertThat( book.authors )
							.allSatisfy( author -> assertThat( author.books ).contains( book ) ) );
		} );

		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Book" ).executeUpdate();
			session.createMutationQuery( "delete from Author" ).executeUpdate();
		} );

		scope.inTransaction( session -> {
			assertThat( session.createQuery( "from Book", Book.class ).list() ).isEmpty();
			assertThat( session.createQuery( "from Author", Author.class ).list() ).isEmpty();
		} );
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		private int id;

		public Book() {
		}

		public Book(int id) {
			this.id = id;
		}

		@ManyToMany
		@JoinTable(name = "book_author",
				joinColumns = { @JoinColumn(name = "fk_book") },
				inverseJoinColumns = { @JoinColumn(name = "fk_author") })
		private List<Author> authors = new ArrayList<>();

		public void addAuthor(Author author) {
			authors.add( author );
			author.books.add( this );
		}
	}

	@Entity(name = "Author")
	public static class Author {

		@Id
		private int id;

		public Author() {
		}

		public Author(int id) {
			this.id = id;
		}

		@ManyToMany(mappedBy = "authors")
		private List<Book> books = new ArrayList<>();

		public void addBook(Book book) {
			books.add( book );
			book.authors.add( this );
		}
	}

}
