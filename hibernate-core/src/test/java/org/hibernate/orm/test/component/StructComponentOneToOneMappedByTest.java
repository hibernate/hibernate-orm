package org.hibernate.orm.test.component;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Struct;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(
		annotatedClasses = {
				StructComponentOneToOneMappedByTest.Book.class,
				StructComponentOneToOneMappedByTest.BookDetails.class
		}
)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructAggregate.class)
public class StructComponentOneToOneMappedByTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Book book = new Book();
					BookDetails bookDetails = new BookDetails(book, "A nice book");
					book.id = 1L;
					book.title = "Hibernate 6";
					book.author = new Author( "Steve", bookDetails );

					session.save( book );
					session.save( bookDetails );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from BookDetails" ).executeUpdate();
					session.createQuery( "delete from Book" ).executeUpdate();
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Book book = session.createQuery( "from Book b", Book.class ).getSingleResult();
					// One-to-one mappedBy is eager by default
					assertTrue( Hibernate.isInitialized( book.author.getDetails() ) );
					assertEquals( "A nice book", book.author.getDetails().getSummary() );
				}
		);
	}

	@Test
	public void testJoin(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Book book = session.createQuery(
							"from Book b join fetch b.author.details",
							Book.class
					).getSingleResult();
					assertTrue( Hibernate.isInitialized( book.author.getDetails() ) );
					assertEquals( "A nice book", book.author.getDetails().getSummary() );
				}
		);
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Long id;
		private String title;
		private Author author;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
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
	}

	@Embeddable
	@Struct( name = "author_type")
	public static class Author {

		private String name;
		@OneToOne(mappedBy = "book", fetch = FetchType.LAZY)
		private BookDetails details;

		public Author() {
		}

		public Author(String name, BookDetails details) {
			this.name = name;
			this.details = details;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public BookDetails getDetails() {
			return details;
		}

		public void setDetails(BookDetails details) {
			this.details = details;
		}
	}

	@Entity(name = "BookDetails")
	public static class BookDetails {
		@Id
		@OneToOne
		private Book book;
		private String summary;

		public BookDetails() {
		}

		public BookDetails(Book book, String summary) {
			this.book = book;
			this.summary = summary;
		}

		public Book getBook() {
			return book;
		}

		public void setBook(Book book) {
			this.book = book;
		}

		public String getSummary() {
			return summary;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}
	}

}
