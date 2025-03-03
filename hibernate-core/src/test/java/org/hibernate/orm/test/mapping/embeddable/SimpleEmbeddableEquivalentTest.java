/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class SimpleEmbeddableEquivalentTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class
		};
	}

	@Test
	public void testLifecycle() {

		doInJPA(this::entityManagerFactory, entityManager -> {

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
