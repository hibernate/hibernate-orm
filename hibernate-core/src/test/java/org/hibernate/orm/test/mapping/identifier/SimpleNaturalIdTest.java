/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SimpleNaturalIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.setId(1L);
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setIsbn("978-9730228236");

			entityManager.persist(book);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::naturalid-simple-load-access-example[]
			Book book = entityManager
				.unwrap(Session.class)
				.bySimpleNaturalId(Book.class)
				.load("978-9730228236");
			//end::naturalid-simple-load-access-example[]

			assertEquals("High-Performance Java Persistence", book.getTitle());
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
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
