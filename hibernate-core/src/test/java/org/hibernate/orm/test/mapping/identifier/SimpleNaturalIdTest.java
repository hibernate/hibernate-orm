/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {SimpleNaturalIdTest.Book.class})
public class SimpleNaturalIdTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Book book = new Book();
			book.setId(1L);
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setIsbn("978-9730228236");

			entityManager.persist(book);
		});
		scope.inTransaction( entityManager -> {
			//tag::naturalid-simple-load-access-example[]
			Book book = entityManager
				.unwrap(Session.class)
				.bySimpleNaturalId(Book.class)
				.load("978-9730228236");
			//end::naturalid-simple-load-access-example[]

			assertEquals("High-Performance Java Persistence", book.getTitle());
		});
		scope.inTransaction( entityManager -> {
			//tag::naturalid-load-access-example[]
			Book book = entityManager
				.unwrap(Session.class)
				.byNaturalId(Book.class)
				.using("isbn", "978-9730228236")
				.load();
			//end::naturalid-load-access-example[]

			assertEquals("High-Performance Java Persistence", book.getTitle());
		});
	}

	//tag::naturalid-simple-basic-attribute-mapping-example[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		private String author;

		@NaturalId
		private String isbn;

		//Getters and setters are omitted for brevity
	//end::naturalid-simple-basic-attribute-mapping-example[]

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

		public String getIsbn() {
			return isbn;
		}

		public void setIsbn(String isbn) {
			this.isbn = isbn;
		}
	//tag::naturalid-simple-basic-attribute-mapping-example[]
	}
	//end::naturalid-simple-basic-attribute-mapping-example[]
}
