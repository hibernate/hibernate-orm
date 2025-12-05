/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {MutableNaturalIdTest.Author.class})
public class MutableNaturalIdTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Author author = new Author();
			author.setId(1L);
			author.setName("John Doe");
			author.setEmail("john@acme.com");

			entityManager.persist(author);
		});
		scope.inTransaction( entityManager -> {
			//tag::naturalid-mutable-synchronized-example[]
			//tag::naturalid-mutable-example[]
			Author author = entityManager
				.unwrap(Session.class)
				.bySimpleNaturalId(Author.class)
				.load("john@acme.com");
			//end::naturalid-mutable-example[]

			author.setEmail("john.doe@acme.com");

			assertNull(
				entityManager
					.unwrap(Session.class)
					.bySimpleNaturalId(Author.class)
					.setSynchronizationEnabled(false)
					.load("john.doe@acme.com")
			);

			assertSame(author,
				entityManager
					.unwrap(Session.class)
					.bySimpleNaturalId(Author.class)
					.setSynchronizationEnabled(true)
					.load("john.doe@acme.com")
			);
			//end::naturalid-mutable-example[]

			//end::naturalid-mutable-synchronized-example[]
		});
	}

	//tag::naturalid-mutable-mapping-example[]
	@Entity(name = "Author")
	public static class Author {

		@Id
		private Long id;

		private String name;

		@NaturalId(mutable = true)
		private String email;

		//Getters and setters are omitted for brevity
	//end::naturalid-mutable-mapping-example[]

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

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

	//tag::naturalid-mutable-mapping-example[]
	}
	//end::naturalid-mutable-mapping-example[]
}
