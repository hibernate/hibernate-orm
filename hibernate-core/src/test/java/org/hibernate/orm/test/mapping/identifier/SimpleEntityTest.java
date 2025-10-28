/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {
		SimpleEntityTest.Book.class, SimpleEntityTest.Library.class
})
public class SimpleEntityTest {

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Library library = new Library();
			library.setId(1L);
			library.setName("Amazon");

			entityManager.persist(library);

			Book book = new Book();
			book.setId(1L);
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");

			entityManager.persist(book);
		});
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(  entityManager -> {
			scope.getEntityManagerFactory().getSchemaManager().truncate();
		} );
	}

	@Test
	public void testIdentityScope(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {
			//tag::entity-pojo-identity-scope-example[]
			Book book1 = entityManager.find(Book.class, 1L);
			Book book2 = entityManager.find(Book.class, 1L);

			assertTrue(book1 == book2);
			//end::entity-pojo-identity-scope-example[]
		});

	}

	@Test
	public void testSetIdentityScope(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {
			//tag::entity-pojo-set-identity-scope-example[]
			Library library = entityManager.find(Library.class, 1L);

			Book book1 = entityManager.find(Book.class, 1L);
			Book book2 = entityManager.find(Book.class, 1L);

			library.getBooks().add(book1);
			library.getBooks().add(book2);

			assertEquals(1, library.getBooks().size());
			//end::entity-pojo-set-identity-scope-example[]
		});
	}

	@Test
	public void testMultiSessionIdentityScope(EntityManagerFactoryScope scope) {

		//tag::entity-pojo-multi-session-identity-scope-example[]
		Book book1 = scope.fromTransaction( entityManager -> entityManager.find(Book.class, 1L) );

		Book book2 = scope.fromTransaction( entityManager -> entityManager.find(Book.class, 1L) );

		assertFalse(book1 == book2);
		//end::entity-pojo-multi-session-identity-scope-example[]
	}

	@Test
	public void testMultiSessionSetIdentityScope(EntityManagerFactoryScope scope) {

		Book book1 = scope.fromTransaction( entityManager -> entityManager.find(Book.class, 1L) );

		Book book2 = scope.fromTransaction( entityManager -> entityManager.find(Book.class, 1L) );
		//tag::entity-pojo-multi-session-set-identity-scope-example[]

		scope.inTransaction( entityManager -> {
			Set<Book> books = new HashSet<>();

			books.add(book1);
			books.add(book2);

			assertEquals(2, books.size());
		});
		//end::entity-pojo-multi-session-set-identity-scope-example[]
	}

	@Test
	public void testTransientSetIdentityScope(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {
			//tag::entity-pojo-transient-set-identity-scope-example[]
			Library library = entityManager.find(Library.class, 1L);

			Book book1 = new Book();
			book1.setId(100L);
			book1.setTitle("High-Performance Java Persistence");

			Book book2 = new Book();
			book2.setId(101L);
			book2.setTitle("Java Persistence with Hibernate");

			library.getBooks().add(book1);
			library.getBooks().add(book2);

			assertEquals(2, library.getBooks().size());
			//end::entity-pojo-transient-set-identity-scope-example[]
		});
	}

	//tag::entity-pojo-set-mapping-example[]
	@Entity(name = "MyLibrary")
	public static class Library {

		@Id
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn(name = "book_id")
		private Set<Book> books = new HashSet<>();

		//Getters and setters are omitted for brevity
	//end::entity-pojo-set-mapping-example[]

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
	//tag::entity-pojo-set-mapping-example[]
	}
	//end::entity-pojo-set-mapping-example[]

	//tag::entity-pojo-mapping-example[]
	@Entity(name = "Book")
	public static class Book {

		//tag::entity-pojo-identifier-mapping-example[]
		@Id
		private Long id;
		//end::entity-pojo-identifier-mapping-example[]

		private String title;

		private String author;

		//Getters and setters are omitted for brevity
	//end::entity-pojo-mapping-example[]

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
	//tag::entity-pojo-mapping-example[]
	}
	//end::entity-pojo-mapping-example[]
}
