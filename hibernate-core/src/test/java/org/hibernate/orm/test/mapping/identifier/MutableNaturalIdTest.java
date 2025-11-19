/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.KeyType;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
			Author author = entityManager.find( Author.class,
					"john@acme.com",
					KeyType.NATURAL );

			// change the natural id value
			author.setEmail("john.doe@acme.com");

			// since there has been no flush,
			// the internal resolution cache
			// does not know about the change -
			// without synchronization, we will
			// get a miss.

			Author author2 = entityManager.find( Author.class,
					"john.doe@acme.com",
					KeyType.NATURAL,
					NaturalIdSynchronization.DISABLED );
			assertNull( author2 );

			// with synchronization (the default),
			// however, we will get correct results.

			Author author3 = entityManager.find( Author.class,
					"john.doe@acme.com",
					KeyType.NATURAL );
			assertEquals( author, author3 );
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
