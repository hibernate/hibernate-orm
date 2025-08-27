/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MultipleNaturalIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class,
			Publisher.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Publisher publisher = new Publisher();
			publisher.setId(1L);
			publisher.setName("Amazon");
			entityManager.persist(publisher);

			Book book = new Book();
			book.setId(1L);
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setProductNumber("973022823X");
			book.setPublisher(publisher);

			entityManager.persist(book);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Publisher publisher = entityManager.getReference(Publisher.class, 1L);
			//tag::naturalid-load-access-example[]

			Book book = entityManager
				.unwrap(Session.class)
				.byNaturalId(Book.class)
				.using("productNumber", "973022823X")
				.using("publisher", publisher)
				.load();
			//end::naturalid-load-access-example[]

			assertEquals("High-Performance Java Persistence", book.getTitle());
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Publisher publisher = entityManager.getReference(Publisher.class, 1L);
			Book book = entityManager
					.unwrap(Session.class)
					.byNaturalId(Book.class)
					.using(Map.of("productNumber", "973022823X", "publisher", publisher))
					.load();

			assertEquals("High-Performance Java Persistence", book.getTitle());
		});
	}

	//tag::naturalid-multiple-attribute-mapping-example[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		private String author;

		@NaturalId
		private String productNumber;

		@NaturalId
		@ManyToOne(fetch = FetchType.LAZY)
		private Publisher publisher;

		//Getters and setters are omitted for brevity
	//end::naturalid-multiple-attribute-mapping-example[]

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

		public String getProductNumber() {
			return productNumber;
		}

		public void setProductNumber(String productNumber) {
			this.productNumber = productNumber;
		}

		public Publisher getPublisher() {
			return publisher;
		}

		public void setPublisher(Publisher publisher) {
			this.publisher = publisher;
		}
	//tag::naturalid-multiple-attribute-mapping-example[]
	}

	@Entity(name = "Publisher")
	public static class Publisher implements Serializable {

		@Id
		private Long id;

		private String name;

		//Getters and setters are omitted for brevity
		//end::naturalid-multiple-attribute-mapping-example[]

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

	//tag::naturalid-multiple-attribute-mapping-example[]

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Publisher publisher = (Publisher) o;
			return Objects.equals(id, publisher.id) &&
					Objects.equals(name, publisher.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, name);
		}
	}
	//end::naturalid-multiple-attribute-mapping-example[]
}
