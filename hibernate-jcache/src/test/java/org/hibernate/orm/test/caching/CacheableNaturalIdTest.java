/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching;

import javax.cache.configuration.MutableConfiguration;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cache.jcache.JCacheHelper;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = CacheableNaturalIdTest.Book.class,
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_FACTORY, value = "jcache"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true"),
				@Setting(name = AvailableSettings.CACHE_REGION_PREFIX, value = "")
		}
)
public class CacheableNaturalIdTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		JCacheHelper.locateStandardCacheManager().createCache("default-update-timestamps-region", new MutableConfiguration<>());
		JCacheHelper.locateStandardCacheManager().createCache("default-query-results-region", new MutableConfiguration<>());
		JCacheHelper.locateStandardCacheManager().createCache("org.hibernate.userguide.mapping.identifier.CacheableNaturalIdTest$Book##NaturalId", new MutableConfiguration<>());
	}

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Book book = new Book();
			book.setId(1L);
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");
			book.setIsbn("978-9730228236");

			entityManager.persist(book);
		});
		scope.inTransaction( entityManager -> {
			//tag::naturalid-cacheable-load-access-example[]
			Book book = entityManager
				.unwrap(Session.class)
				.bySimpleNaturalId(Book.class)
				.load("978-9730228236");
			//end::naturalid-cacheable-load-access-example[]

			assertEquals("High-Performance Java Persistence", book.getTitle());
		});
	}

	//tag::naturalid-cacheable-mapping-example[]
	@Entity(name = "Book")
	@NaturalIdCache
	public static class Book {

		@Id
		private Long id;

		private String title;

		private String author;

		@NaturalId
		private String isbn;

		//Getters and setters are omitted for brevity
	//end::naturalid-cacheable-mapping-example[]

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
	//tag::naturalid-cacheable-mapping-example[]
	}
	//end::naturalid-cacheable-mapping-example[]
}
