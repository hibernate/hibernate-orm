/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {SimpleEmbeddableTest.Book.class})
public class SimpleEmbeddableTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {

			Book book = new Book();
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setPublisher(
				new Publisher(
					"Amazon",
					"USA"
				)
			);

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

		private Publisher publisher;

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

		public Publisher getPublisher() {
			return publisher;
		}

		public void setPublisher(Publisher publisher) {
			this.publisher = publisher;
		}

	//tag::embeddable-type-mapping-example[]
	}

	@Embeddable
	public static class Publisher {

		@Column(name = "publisher_name")
		private String name;

		@Column(name = "publisher_country")
		private String country;

		//Getters and setters, equals and hashCode methods omitted for brevity

	//end::embeddable-type-mapping-example[]


		public Publisher(String name, String country) {
			this.name = name;
			this.country = country;
		}

		private Publisher() {}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

	//tag::embeddable-type-mapping-example[]
	}
	//end::embeddable-type-mapping-example[]
}
