/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

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
@Jpa(annotatedClasses = {NestedEmbeddableTest.Book.class})
public class NestedEmbeddableTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {
			Book book = new Book();
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setPublisher(
				new Publisher(
					"Amazon",
					new Location(
						"USA",
						"Seattle"
					)
				)
			);

			entityManager.persist(book);
		});
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		private Publisher publisher;

		//Getters and setters are omitted for brevity

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

	}

	//tag::embeddable-type-mapping-example[]
	@Embeddable
	public static class Publisher {

		private String name;

		private Location location;

		public Publisher(String name, Location location) {
			this.name = name;
			this.location = location;
		}

		private Publisher() {}

		//Getters and setters are omitted for brevity
	//end::embeddable-type-mapping-example[]

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Location getLocation() {
			return location;
		}

		public void setLocation(Location location) {
			this.location = location;
		}

	//tag::embeddable-type-mapping-example[]
	}

	@Embeddable
	public static class Location {

		private String country;

		private String city;

		public Location(String country, String city) {
			this.country = country;
			this.city = city;
		}

		private Location() {}

		//Getters and setters are omitted for brevity
	//end::embeddable-type-mapping-example[]

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}
	//tag::embeddable-type-mapping-example[]
	}
	//end::embeddable-type-mapping-example[]
}
