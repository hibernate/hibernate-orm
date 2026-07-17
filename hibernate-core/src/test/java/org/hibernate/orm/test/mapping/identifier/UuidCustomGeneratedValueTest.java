/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.dialect.SybaseDialect;

import org.hibernate.id.uuid.CustomVersionOneStrategy;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(
		dialectClass = SybaseDialect.class, matchSubTypes = true,
		reason = "Skipped for Sybase to avoid problems with UUIDs potentially ending with a trailing 0 byte"
)
@Jpa(annotatedClasses = {UuidCustomGeneratedValueTest.Book.class})
public class UuidCustomGeneratedValueTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		Book book = new Book();

		scope.inTransaction( entityManager -> {
			book.setTitle("High-Performance Java Persistence");
			book.setAuthor("Vlad Mihalcea");

			entityManager.persist(book);
		});

		assertNotNull(book.getId());
	}

	//tag::identifiers-generators-custom-uuid-mapping-example[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		@UuidGenerator(algorithm = CustomVersionOneStrategy.class)
		private UUID id;

		private String title;

		private String author;

		//Getters and setters are omitted for brevity
	//end::identifiers-generators-custom-uuid-mapping-example[]

		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
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
	//tag::identifiers-generators-custom-uuid-mapping-example[]
	}
	//end::identifiers-generators-custom-uuid-mapping-example[]
}
