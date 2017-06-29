/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.embeddable;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class NestedEmbeddableTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class
		};
	}

	@Test
	public void testLifecycle() {

		doInJPA( this::entityManagerFactory, entityManager -> {

			Book book = new Book();
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vlad Mihalcea" );
			book.setPublisher(
				new Publisher(
					"Amazon",
					new Location(
						"USA",
						"Seattle"
					)
				)
			);

			entityManager.persist( book );
		} );
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
