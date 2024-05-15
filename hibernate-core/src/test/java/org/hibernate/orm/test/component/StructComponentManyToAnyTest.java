package org.hibernate.orm.test.component;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(
		annotatedClasses = {
				StructComponentManyToAnyTest.Book.class
		}
)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructAggregate.class)
public class StructComponentManyToAnyTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					Book book1 = new Book();
					book1.id = 1L;
					book1.title = "Hibernate 3";
					book1.author = new Author( "Gavin", null );

					session.save( book1 );

					Book book2 = new Book();
					book2.id = 2L;
					book2.title = "Hibernate 6";
					book2.author = new Author( "Steve", book1 );

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
					Book book = session.createQuery( "from Book b where b.id = 2", Book.class ).getSingleResult();
					assertFalse( Hibernate.isInitialized( book.author.getFavoriteBook() ) );
					assertEquals( "Gavin", book.author.getFavoriteBook().getAuthor().getName() );
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
		@Any(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@AnyDiscriminatorValue(entity = Book.class, discriminator = "B")
		@JoinColumn(name = "favorite_book_id")
		@Column(name = "favorite_book_type")
		private Object favoriteBook;

		public Author() {
		}

		public Author(String name, Object favoriteBook) {
			this.name = name;
			this.favoriteBook = favoriteBook;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Book getFavoriteBook() {
			return (Book) favoriteBook;
		}

		public void setFavoriteBook(Book favoriteBook) {
			this.favoriteBook = favoriteBook;
		}
	}

}
