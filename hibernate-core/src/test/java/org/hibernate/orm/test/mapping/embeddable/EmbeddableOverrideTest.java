/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {
		EmbeddableOverrideTest.Book.class,
		EmbeddableOverrideTest.Country.class
})
public class EmbeddableOverrideTest {

	@Test
	public void testLifecycle(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Country canada = new Country();
			canada.setName("Canada");
			entityManager.persist(canada);

			Country usa = new Country();
			usa.setName("USA");
			entityManager.persist(usa);
		});

		scope.inTransaction( entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			Country canada = session.byNaturalId(Country.class).using("name", "Canada").load();
			Country usa = session.byNaturalId(Country.class).using("name", "USA").load();

			Book book = new Book();
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setEbookPublisher(new Publisher("Leanpub", canada));
			book.setPaperBackPublisher(new Publisher("Amazon", usa));

			entityManager.persist(book);
		});
	}

	//tag::embeddable-type-override-mapping-example[]
	@Entity(name = "Book")
	@AttributeOverrides({
			@AttributeOverride(
					name = "ebookPublisher.name",
					column = @Column(name = "ebook_pub_name")
			),
			@AttributeOverride(
					name = "paperBackPublisher.name",
					column = @Column(name = "paper_back_pub_name")
			)
	})
	@AssociationOverrides({
			@AssociationOverride(
					name = "ebookPublisher.country",
					joinColumns = @JoinColumn(name = "ebook_pub_country_id")
			),
			@AssociationOverride(
					name = "paperBackPublisher.country",
					joinColumns = @JoinColumn(name = "paper_back_pub_country_id")
			)
	})
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		private Publisher ebookPublisher;

		private Publisher paperBackPublisher;

		//Getters and setters are omitted for brevity
		//end::embeddable-type-override-mapping-example[]

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

		public Publisher getEbookPublisher() {
			return ebookPublisher;
		}

		public void setEbookPublisher(Publisher ebookPublisher) {
			this.ebookPublisher = ebookPublisher;
		}

		public Publisher getPaperBackPublisher() {
			return paperBackPublisher;
		}

		public void setPaperBackPublisher(Publisher paperBackPublisher) {
			this.paperBackPublisher = paperBackPublisher;
		}
		//tag::embeddable-type-override-mapping-example[]
	}
		//end::embeddable-type-override-mapping-example[]

	//tag::embeddable-type-association-mapping-example[]
	@Embeddable
	public static class Publisher {

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Country country;

		//Getters and setters, equals and hashCode methods omitted for brevity

	//end::embeddable-type-association-mapping-example[]

		public Publisher(String name, Country country) {
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

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}
	//tag::embeddable-type-association-mapping-example[]
	}

	@Entity(name = "Country")
	public static class Country {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String name;

		//Getters and setters are omitted for brevity
		//end::embeddable-type-association-mapping-example[]

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
		//tag::embeddable-type-association-mapping-example[]
	}
		//end::embeddable-type-association-mapping-example[]
}
