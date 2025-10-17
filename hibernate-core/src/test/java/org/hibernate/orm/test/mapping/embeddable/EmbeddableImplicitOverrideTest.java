/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {EmbeddableImplicitOverrideTest.Book.class, EmbeddableImplicitOverrideTest.Country.class}
)
@SessionFactory
@ServiceRegistry(
		settingProviders = {
				@SettingProvider(
						settingName = MappingSettings.IMPLICIT_NAMING_STRATEGY,
						provider = EmbeddableImplicitOverrideTest.ImplicitNamingStrategySettingProvider.class)
		}
)
public class EmbeddableImplicitOverrideTest {

	public static class ImplicitNamingStrategySettingProvider implements SettingProvider.Provider<ImplicitNamingStrategyComponentPathImpl> {
		@Override
		public ImplicitNamingStrategyComponentPathImpl getSetting() {
			return ImplicitNamingStrategyComponentPathImpl.INSTANCE;
		}
	}

	// Preserved because of the doc inclusion
	private void doesNothing(MetadataBuilder metadataBuilder) {
		//tag::embeddable-multiple-ImplicitNamingStrategyComponentPathImpl[]
		metadataBuilder.applyImplicitNamingStrategy(
			ImplicitNamingStrategyComponentPathImpl.INSTANCE
		);
		//end::embeddable-multiple-ImplicitNamingStrategyComponentPathImpl[]
	}

	@Test
	public void testLifecycle(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Country canada = new Country();
			canada.setName("Canada");
			session.persist(canada);

			Country usa = new Country();
			usa.setName("USA");
			session.persist(usa);
		});

		scope.inTransaction( session -> {
			Country canada = session.byNaturalId(Country.class).using("name", "Canada").load();
			Country usa = session.byNaturalId(Country.class).using("name", "USA").load();

			Book book = new Book();
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setEbookPublisher(new Publisher("Leanpub", canada));
			book.setPaperBackPublisher(new Publisher("Amazon", usa));

			session.persist(book);
		});
	}

	//tag::embeddable-multiple-namingstrategy-entity-mapping[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		private Publisher ebookPublisher;

		private Publisher paperBackPublisher;

		//Getters and setters are omitted for brevity
	//end::embeddable-multiple-namingstrategy-entity-mapping[]

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
	//tag::embeddable-multiple-namingstrategy-entity-mapping[]
	}

	@Embeddable
	public static class Publisher {

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Country country;

		//Getters and setters, equals and hashCode methods omitted for brevity
	//end::embeddable-multiple-namingstrategy-entity-mapping[]

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
	//tag::embeddable-multiple-namingstrategy-entity-mapping[]
	}

	@Entity(name = "Country")
	public static class Country {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String name;

		//Getters and setters are omitted for brevity
	//end::embeddable-multiple-namingstrategy-entity-mapping[]

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
	//tag::embeddable-multiple-namingstrategy-entity-mapping[]
	}
	//end::embeddable-multiple-namingstrategy-entity-mapping[]
}
