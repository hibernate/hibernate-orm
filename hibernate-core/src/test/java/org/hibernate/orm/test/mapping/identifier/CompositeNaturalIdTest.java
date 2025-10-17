/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
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
@Jpa(annotatedClasses = {CompositeNaturalIdTest.Book.class})
public class CompositeNaturalIdTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Book book = new Book();
			book.setId(1L);
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setIsbn(new Isbn(
				"973022823X",
				"978-9730228236"
			));

			entityManager.persist(book);
		});
		scope.inTransaction( entityManager -> {
			//tag::naturalid-simple-load-access-example[]

			Book book = entityManager
				.unwrap(Session.class)
				.bySimpleNaturalId(Book.class)
				.load(
					new Isbn(
						"973022823X",
						"978-9730228236"
					)
				);
			//end::naturalid-simple-load-access-example[]

			assertEquals("High-Performance Java Persistence", book.getTitle());
		});
		scope.inTransaction( entityManager -> {
			//tag::naturalid-load-access-example[]

			Book book = entityManager
				.unwrap(Session.class)
				.byNaturalId(Book.class)
				.using(
					"isbn",
					new Isbn(
						"973022823X",
						"978-9730228236"
					))
				.load();
			//end::naturalid-load-access-example[]

			assertEquals("High-Performance Java Persistence", book.getTitle());
		});
	}

	//tag::naturalid-single-embedded-attribute-mapping-example[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		private String author;

		@NaturalId
		@Embedded
		private Isbn isbn;

		//Getters and setters are omitted for brevity
	//end::naturalid-single-embedded-attribute-mapping-example[]

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

		public Isbn getIsbn() {
			return isbn;
		}

		public void setIsbn(Isbn isbn) {
			this.isbn = isbn;
		}
	//tag::naturalid-single-embedded-attribute-mapping-example[]
	}

	@Embeddable
	public static class Isbn implements Serializable {

		private String isbn10;

		private String isbn13;

		//Getters and setters are omitted for brevity
	//end::naturalid-single-embedded-attribute-mapping-example[]

		public Isbn() {
		}

		public Isbn(String isbn10, String isbn13) {
			this.isbn10 = isbn10;
			this.isbn13 = isbn13;
		}

		public String getIsbn10() {
			return isbn10;
		}

		public void setIsbn10(String isbn10) {
			this.isbn10 = isbn10;
		}

		public String getIsbn13() {
			return isbn13;
		}

		public void setIsbn13(String isbn13) {
			this.isbn13 = isbn13;
		}

	//tag::naturalid-single-embedded-attribute-mapping-example[]

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Isbn isbn = (Isbn) o;
			return Objects.equals(isbn10, isbn.isbn10) &&
					Objects.equals(isbn13, isbn.isbn13);
		}

		@Override
		public int hashCode() {
			return Objects.hash(isbn10, isbn13);
		}
	}
	//end::naturalid-single-embedded-attribute-mapping-example[]
}
