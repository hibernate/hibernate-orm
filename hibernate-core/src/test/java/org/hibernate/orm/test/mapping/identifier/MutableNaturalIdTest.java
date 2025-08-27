/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class MutableNaturalIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Author.class
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Author author = new Author();
			author.setId(1L);
			author.setName("John Doe");
			author.setEmail("john@acme.com");

			entityManager.persist(author);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
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
