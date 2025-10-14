/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {PropertyAccessOverrideTest.Book.class})
public class PropertyAccessOverrideTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			Book book = new Book();
			book.setId(1L);
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");

			entityManager.persist(book);
		});
	}

	//tag::access-property-override-mapping-example[]
	@Entity(name = "Book")
	public static class Book {

		private Long id;

		private String title;

		private String author;

		@Access(AccessType.FIELD)
		@Version
		private int version;

		@Id
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
	}
	//end::access-property-override-mapping-example[]
}
