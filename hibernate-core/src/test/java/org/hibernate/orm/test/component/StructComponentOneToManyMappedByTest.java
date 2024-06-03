package org.hibernate.orm.test.component;

import java.util.Set;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(
		annotatedClasses = {
				StructComponentOneToManyMappedByTest.Book.class
		}
)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructAggregate.class)
public class StructComponentOneToManyMappedByTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Book book1 = new Book();
					book1.id = 1L;
					book1.title = "Main book";
					book1.author = new Author( "Abc" );

					session.save( book1 );

					Book book2 = new Book();
					book2.id = 2L;
					book2.title = "Second book";
					book2.mainBook = book1;
					book2.author = new Author( "Abc" );

					session.save( book2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session ->
						session.createQuery( "delete from Book" ).executeUpdate()
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Book book = session.createQuery( "from Book b where b.id = 1", Book.class ).getSingleResult();
					assertFalse( Hibernate.isInitialized( book.author.getBooks() ) );
					assertEquals( "Second book", book.author.getBooks().iterator().next().getTitle() );
				}
		);
	}

	@Test
	public void testJoin(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Book book = session.createQuery(
							"from Book b join fetch b.author.books where b.id = 1",
							Book.class
					).getSingleResult();
					assertTrue( Hibernate.isInitialized( book.author.getBooks() ) );
					assertEquals( "Second book", book.author.getBooks().iterator().next().getTitle() );
				}
		);
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Long id;
		private String title;
		private Author author;
		@ManyToOne(fetch = FetchType.LAZY)
		private Book mainBook;

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

		public Book getMainBook() {
			return mainBook;
		}

		public void setMainBook(Book mainBook) {
			this.mainBook = mainBook;
		}
	}

	@Embeddable
	@Struct( name = "author_type")
	public static class Author {

		private String name;
		@OneToMany(mappedBy = "mainBook")
		private Set<Book> books;

		public Author() {
		}

		public Author(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Book> getBooks() {
			return books;
		}

		public void setBooks(Set<Book> books) {
			this.books = books;
		}
	}

}
