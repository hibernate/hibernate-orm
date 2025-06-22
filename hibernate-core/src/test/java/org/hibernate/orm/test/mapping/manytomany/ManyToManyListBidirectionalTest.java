/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.util.collections.ArrayHelper;

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

import static org.assertj.core.api.Assertions.assertThat;

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
			session.persist( author1 );
			session.persist( author2 );

			final Book bookByAuthor1 = new Book( 1, author1 );
			final Book bookByAuthor2 = new Book( 2, author2 );
			final Book bookByAuthors1And2 = new Book( 3, author1, author2 );
			session.persist( bookByAuthor1 );
			session.persist( bookByAuthor2 );
			session.persist( bookByAuthors1And2 );
		} );

		scope.inTransaction( session -> {
			final List<Book> books = session.createQuery( "from Book b", Book.class ).list();

			assertThat( books ).hasSize( 3 );
			books.forEach( (book) -> {
				book.authors.forEach( (author) -> {
					assertThat( author.books ).contains( book );
				} );
			} );

//			assertThat( books )
//					.hasSize( 3 )
//					.allSatisfy( book -> assertThat( book.authors )
//							.allSatisfy( author -> assertThat( author.books ).contains( book ) ) );
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

		public Book(int id, Author author) {
			this.id = id;
			link( author );
		}

		private void link(Author author) {
			authors.add( author );
			author.books.add( this );
		}

		public Book(int id, Author... authors) {
			this.id = id;
			ArrayHelper.forEach( authors, this::link );
		}

		@ManyToMany
		@JoinTable(name = "book_author",
				joinColumns = { @JoinColumn(name = "fk_book") },
				inverseJoinColumns = { @JoinColumn(name = "fk_author") })
		private List<Author> authors = new ArrayList<>();

		public void addAuthor(Author author) {
			link( author );
		}

		@Override
		public String toString() {
			return "Book(" + id + ")@" + Integer.toHexString( hashCode() );
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

		@Override
		public String toString() {
			return "Author(" + id + ")@" + Integer.toHexString( hashCode() );
		}
	}

}
