/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.manytoone;

import java.util.Set;

import org.hibernate.Session;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.cfg.JdbcSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Allows testing a @ManyToOne mappedBy relationship with a @Any as the return variable.
 *
 * @author Vincent Bouthinon
 */
@Jpa(
		annotatedClasses = {
				ManyToOneWithAnyTest.Library.class,
				ManyToOneWithAnyTest.Book.class,
				ManyToOneWithAnyTest.Shop.class
		},
		integrationSettings = @Setting(name = JdbcSettings.SHOW_SQL, value = "true")
)
@JiraKey("HHH-15722")
class ManyToOneWithAnyTest {

	@Test
	void testMappingManyToOneMappedByAny(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					Library library = new Library();
					Book firstBook = new Book();
					final Set<Book> books = Set.of( firstBook, new Book() );
					library.setBooks( books );

					entityManager.persist( library );
					entityManager.flush();
					entityManager.clear();

					firstBook = entityManager.unwrap( Session.class )
							.byId( firstBook.getClass() )
							.load( firstBook.getId() );

					assertNotNull( firstBook );

					entityManager.clear();

					library = entityManager.unwrap( Session.class )
							.byId( library.getClass() )
							.load( library.getId() );

					assertNotNull( library );
					assertEquals( 2, library.getBooks().size() );
				}
		);
	}

	@Test
	void testWithSameIdentifiantButSubTypeDifferent(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );
					Library library = new Library();
					library.setId( 1L );
					library.setBooks( Set.of( new Book(), new Book() ) );

					Shop shop = new Shop();
					shop.setId( 1L );
					shop.setBooks( Set.of( new Book(), new Book(), new Book() ) );

					session.save( library );
					session.save( shop );
					session.flush();
					session.clear();

					library = session.byId( library.getClass() ).load( library.getId() );
					assertNotNull( library );
					assertEquals( 2, library.getBooks().size() );

					shop = session.byId( shop.getClass() ).load( shop.getId() );
					assertNotNull( shop );
					assertEquals( 3, shop.getBooks().size() );
				}
		);
	}

	@Entity(name = "book")
	@Table(name = "TBOOK")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		@Any
		@AnyKeyJavaClass(Long.class)
		@AnyDiscriminatorValue(entity = Shop.class, discriminator = "S")
		@AnyDiscriminatorValue(entity = Library.class, discriminator = "L")
		@Column(name = "STORE_ROLE")
		@JoinColumn(name = "STORE_ID")
		private Store store;

		public void setId(final Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public Store getStore() {
			return store;
		}

		public void setStore(final Store store) {
			this.store = store;
		}
	}

	@Entity(name = "library")
	@Table(name = "TLIBRARY")
	public static class Library implements Store {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		private Set<Book> books;

		public void setId(final Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public Set<Book> getBooks() {
			return books;
		}

		public void setBooks(final Set<Book> books) {
			books.forEach( book -> book.setStore( this ) );
			this.books = books;
		}
	}

	@Entity(name = "shop")
	@Table(name = "TSHOP")
	public static class Shop implements Store {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		private Set<Book> books;

		public void setId(final Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public Set<Book> getBooks() {
			return books;
		}

		public void setBooks(final Set<Book> books) {
			books.forEach( book -> book.setStore( this ) );
			this.books = books;
		}
	}

	public interface Store {
	}
}
