/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {SimpleEmbeddableEquivalentTest.Book.class})
public class SimpleEmbeddableEquivalentTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {

			Book book = new Book();
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setPublisherName("Amazon");
			book.setPublisherCountry("USA");

			entityManager.persist(book);
		});
	}

	//tag::embeddable-type-mapping-example[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		@Column(name = "publisher_name")
		private String publisherName;

		@Column(name = "publisher_country")
		private String publisherCountry;

		//Getters and setters are omitted for brevity
	//end::embeddable-type-mapping-example[]

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

		public String getPublisherName() {
			return publisherName;
		}

		public void setPublisherName(String publisherName) {
			this.publisherName = publisherName;
		}

		public String getPublisherCountry() {
			return publisherCountry;
		}

		public void setPublisherCountry(String publisherCountry) {
			this.publisherCountry = publisherCountry;
		}


		//tag::embeddable-type-mapping-example[]
	}
	//end::embeddable-type-mapping-example[]
}
