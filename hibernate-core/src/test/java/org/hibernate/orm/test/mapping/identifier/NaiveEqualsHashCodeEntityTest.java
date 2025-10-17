/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {
		NaiveEqualsHashCodeEntityTest.Book.class, NaiveEqualsHashCodeEntityTest.Library.class
})
public class NaiveEqualsHashCodeEntityTest {

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Library library = new Library();
			library.setId(1L);
			library.setName("Amazon");

			entityManager.persist(library);
		});
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(  entityManager -> {
			scope.getEntityManagerFactory().getSchemaManager().truncate();
		} );
	}

	@Test
	public void testPersist(EntityManagerFactoryScope scope) {

		//tag::entity-pojo-naive-equals-hashcode-persist-example[]
		Book book1 = new Book();
		book1.setTitle("High-Performance Java Persistence");

		Book book2 = new Book();
		book2.setTitle("Java Persistence with Hibernate");

		Library library = scope.fromTransaction( entityManager -> {
			Library _library = entityManager.find(Library.class, 1L);

			_library.getBooks().add(book1);
			_library.getBooks().add(book2);

			return _library;
		});

		assertFalse(library.getBooks().contains(book1));
		assertFalse(library.getBooks().contains(book2));
		//end::entity-pojo-naive-equals-hashcode-persist-example[]
	}

	@Test
	public void testPersistForceFlush(EntityManagerFactoryScope scope) {

		//tag::entity-pojo-naive-equals-hashcode-persist-force-flush-example[]
		Book book1 = new Book();
		book1.setTitle("High-Performance Java Persistence");

		Book book2 = new Book();
		book2.setTitle("Java Persistence with Hibernate");

		Library library = scope.fromTransaction( entityManager -> {
			Library _library = entityManager.find(Library.class, 1L);

			entityManager.persist(book1);
			entityManager.persist(book2);
			entityManager.flush();

			_library.getBooks().add(book1);
			_library.getBooks().add(book2);

			return _library;
		});

		assertTrue(library.getBooks().contains(book1));
		assertTrue(library.getBooks().contains(book2));
		//end::entity-pojo-naive-equals-hashcode-persist-force-flush-example[]
	}

	//tag::entity-pojo-naive-equals-hashcode-example[]
	@Entity(name = "MyLibrary")
	public static class Library {

		@Id
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn(name = "book_id")
		private Set<Book> books = new HashSet<>();

		//Getters and setters are omitted for brevity
	//end::entity-pojo-naive-equals-hashcode-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
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
	//tag::entity-pojo-naive-equals-hashcode-example[]
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		//Getters and setters are omitted for brevity
	//end::entity-pojo-naive-equals-hashcode-example[]

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

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

	//tag::entity-pojo-naive-equals-hashcode-example[]

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Book)) {
				return false;
			}
			Book book = (Book) o;
			return Objects.equals(id, book.getId());
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
		}
	}
	//end::entity-pojo-naive-equals-hashcode-example[]
}
