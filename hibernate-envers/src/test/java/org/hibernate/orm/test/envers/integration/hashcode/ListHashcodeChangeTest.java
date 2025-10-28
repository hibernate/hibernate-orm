/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.hashcode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11063")
@EnversTest
@Jpa(annotatedClasses = {ListHashcodeChangeTest.Author.class, ListHashcodeChangeTest.Book.class})
public class ListHashcodeChangeTest {
	private Integer authorId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			final Author author = new Author();
			author.setFirstName( "TestFirstName" );
			author.setLastName( "lastName" );
			author.addBook( createBook1() );
			author.addBook( createBook2() );
			em.persist( author );
			authorId = author.getId();
		} );

		// Revision 2
		// Removes all books and re-adds original 2 plus one new book
		scope.inTransaction( em -> {
			final Author author = em.find( Author.class, authorId );
			author.removeAllBooks();
			author.addBook( createBook1() );
			author.addBook( createBook2() );
			author.addBook( createBook3() );
			em.merge( author );
		} );
	}

	@Test
	// tests that Author has 3 books.
	public void testAuthorState(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final Author author = em.find( Author.class, authorId );
			assertNotNull( author );
			assertEquals( 3, author.getBooks().size() );
		} );
	}

	@Test
	public void testAuthorLastRevision(EntityManagerFactoryScope scope) {
		// tests that Author has 3 books, Book1, Book2, and Book3.
		// where Book1 and Book2 were removed and re-added with the addition of Book3.
		scope.inEntityManager( entityManager -> {
			final var reader = AuditReaderFactory.get( entityManager );
			final List<Number> revisions = reader.getRevisions( Author.class, authorId );
			final Number lastRevision = revisions.get( revisions.size() - 1 );

			final Author author = (Author) reader.createQuery()
					.forEntitiesAtRevision( Author.class, lastRevision )
					.getSingleResult();

			assertNotNull( author );
			assertEquals( 3, author.getBooks().size() );
		} );
	}

	private Book createBook1() {
		Book book = new Book();
		book.setTitle( "Book1" );
		return book;
	}

	private Book createBook2() {
		Book book = new Book();
		book.setTitle( "Book2" );
		return book;
	}

	private Book createBook3() {
		Book book = new Book();
		book.setTitle( "Book3" );
		return book;
	}

	@Entity(name = "Author")
	@Audited
	public static class Author {
		@Id
		@GeneratedValue
		private Integer id;

		private String firstName;

		private String lastName;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "author")
		private List<Book> books;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public List<Book> getBooks() {
			return books;
		}

		public void setBooks(List<Book> books) {
			this.books = books;
		}

		public void addBook(Book book) {
			if ( this.books == null ) {
				this.books = new ArrayList<Book>();
			}
			book.setAuthor( this );
			this.books.add( book );
		}

		public void removeAllBooks() {
			if ( this.books != null ) {
				this.books.clear();
			}
		}

		public Book getBook(String title) {
			return books.stream().filter( b -> title.equals( b.getTitle() ) ).findFirst().orElse( null );
		}

		public void removeBook(String title) {
			for ( Iterator<Book> it = books.iterator(); it.hasNext(); ) {
				Book book = it.next();
				if ( title.equals( title ) ) {
					it.remove();
				}
			}
		}

		@Override
		public String toString() {
			return "Author{" +
				"id=" + id +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				'}';
		}
	}

	@Entity(name = "Book")
	@Audited
	public static class Book {
		@Id
		@GeneratedValue
		private Integer id;

		private String title;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinTable(name = "author_book",
				joinColumns = @JoinColumn(name = "book_id"),
				inverseJoinColumns = @JoinColumn(name = "author_id", nullable = false))
		private Author author;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public Author getAuthor() {
			return author;
		}

		public void setAuthor(Author author) {
			this.author = author;
		}

		@Override
		public int hashCode() {
			return Objects.hash( title );
		}

		@Override
		public boolean equals(Object object) {
			if ( this == object ) {
				return true;
			}
			if ( object == null || getClass() != object.getClass() ) {
				return false;
			}
			Book book = (Book) object;
			return Objects.equals( title, book.title );
		}

		@Override
		public String toString() {
			return "Book{" +
				"id=" + id +
				", title='" + title + '\'' +
				", author=" + author +
				'}';
		}
	}
}
