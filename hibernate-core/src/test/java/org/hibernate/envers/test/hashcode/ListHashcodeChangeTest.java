/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.hashcode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11063")
public class ListHashcodeChangeTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer authorId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Author.class, Book.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Revision 1
		inTransaction(
				entityManager -> {
					final Author author = new Author();
					author.setFirstName( "TestFirstName" );
					author.setLastName( "lastName" );
					author.addBook( createBook1() );
					author.addBook( createBook2() );
					entityManager.persist( author );
					authorId = author.getId();
				}
		);

		// Revision 2
		// Removes all books and re-adds original 2 plus one new book
		inTransaction(
				entityManager -> {
					final Author author = entityManager.find( Author.class, authorId );
					author.removeAllBooks();
					author.addBook( createBook1() );
					author.addBook( createBook2() );
					author.addBook( createBook3() );
					entityManager.merge( author );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( Author.class, authorId ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testAuthorState() {
		// tests that Author has 3 books.
		inJPA(
				entityManager -> {
					final Author author = entityManager.find( Author.class, authorId );
					assertThat( author, notNullValue() );
					assertThat( author.getBooks(), CollectionMatchers.hasSize( 3 ) );
				}
		);
	}

	@DynamicTest
	public void testAuthorLastRevision() {
		// tests that Author has 3 books, Book1, Book2, and Book3.
		// where Book1 and Book2 were removed and re-added with the addition of Book3.
		final Number lastRevision = getAuditReader().getRevisions( Author.class, authorId )
				.stream()
				.reduce( (first, second) -> second )
				.orElse( null );

		final Author author = (Author) getAuditReader().createQuery()
				.forEntitiesAtRevision( Author.class, lastRevision )
				.getSingleResult();

		assertThat( author, notNullValue() );
		assertThat( author.getBooks(), CollectionMatchers.hasSize( 3 ) );
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

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		@JoinColumn(name = "author_id", referencedColumnName = "id", nullable = false)
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
			for( Iterator<Book> it = books.iterator(); it.hasNext(); ) {
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
		@JoinColumn(name = "author_id", nullable = false, insertable = false, updatable = false)
		@NotAudited
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
