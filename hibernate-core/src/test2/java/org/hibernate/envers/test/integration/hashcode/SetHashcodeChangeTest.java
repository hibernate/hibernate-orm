/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.hashcode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11063")
public class SetHashcodeChangeTest extends BaseEnversJPAFunctionalTestCase {
	private Integer authorId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Author.class, Book.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			final Author author = new Author();
			author.setFirstName( "TestFirstName" );
			author.setLastName( "lastName" );
			author.addBook( createBook1() );
			author.addBook( createBook2() );
			entityManager.persist( author );
			authorId = author.getId();
			entityManager.getTransaction().commit();
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
		}
		finally {
			entityManager.close();
		}
		// Revision 2
		// Removes all books and re-adds original 2 plus one new book
		entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			final Author author = entityManager.find( Author.class, authorId );
			author.removeAllBooks();
			author.addBook( createBook1() );
			author.addBook( createBook2() );
			author.addBook( createBook3() );
			entityManager.merge( author );
			entityManager.getTransaction().commit();
		}
		catch ( Exception e ) {
			if( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testRevisionCounts() {

	}

	@Test
	// tests that Author has 3 books.
	public void testAuthorState() {
		EntityManager entityManager = getEntityManager();
		try {
			final Author author = entityManager.find( Author.class, authorId );
			assertNotNull( author );
			assertEquals( 3, author.getBooks().size() );
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testAuthorLastRevision() {
		// tests that Author has 3 books, Book1, Book2, and Book3.
		// where Book1 and Book2 were removed and re-added with the addition of Book3.
		EntityManager entityManager = getEntityManager();
		try {
			final AuditReader reader = getAuditReader();
			final List<Number> revisions = reader.getRevisions( Author.class, authorId );
			final Number lastRevision = revisions.get( revisions.size() - 1 );

			final Author author = (Author) reader.createQuery()
					.forEntitiesAtRevision( Author.class, lastRevision )
					.getSingleResult();

			assertNotNull( author );
			assertEquals( 3, author.getBooks().size() );
		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
		}
		finally {
			entityManager.close();
		}
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
		private Set<Book> books;

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

		public Set<Book> getBooks() {
			return books;
		}

		public void setBooks(Set<Book> books) {
			this.books = books;
		}

		public void addBook(Book book) {
			if ( this.books == null ) {
				this.books = new HashSet<Book>();
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
		@JoinTable(name = "author_book",
				joinColumns = @JoinColumn(name = "book_id"), inverseJoinColumns = @JoinColumn(name="author_id",nullable = false))
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
