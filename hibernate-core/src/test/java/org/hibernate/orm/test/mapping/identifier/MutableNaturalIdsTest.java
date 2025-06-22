/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Gary Gregory
 */
public class MutableNaturalIdsTest extends BaseEntityManagerFunctionalTestCase {

	private static final String FIELD_1 = "email1";
	private static final String FIELD_2 = "email2";
	private static final String FIELD_3 = "email3";

	private static final String OLD_VALUE_1 = "john1@acme.com";
	private static final String OLD_VALUE_2 = "john2@acme.com";
	private static final String OLD_VALUE_3 = "john3@acme.com";

	private static final String NEW_VALUE_1 = "john.doe1@acme.com";
	private static final String NEW_VALUE_2 = "john.doe2@acme.com";

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
			author.setEmail1(OLD_VALUE_1);
			author.setEmail2(OLD_VALUE_2);
			author.setEmail3(OLD_VALUE_3);

			entityManager.persist(author);
		});
		doInJPA(this::entityManagerFactory, entityManager -> {
			Author author = entityManager
				.unwrap(Session.class)
				.byNaturalId(Author.class)
				.using(FIELD_1, OLD_VALUE_1)
				.using(FIELD_2, OLD_VALUE_2)
				.using(FIELD_3, OLD_VALUE_3)
				.load();

			author.setEmail1(NEW_VALUE_1);
			author.setEmail2(NEW_VALUE_2);

			assertNull(
				entityManager
					.unwrap(Session.class)
					.byNaturalId(Author.class)
					.setSynchronizationEnabled(false)
					.using(FIELD_1, NEW_VALUE_1)
					.using(FIELD_2, NEW_VALUE_2)
					.using(FIELD_3, OLD_VALUE_3)
					.load()
			);

			assertSame(author,
				entityManager
					.unwrap(Session.class)
					.byNaturalId(Author.class)
					.setSynchronizationEnabled(true)
					.using(FIELD_1, NEW_VALUE_1)
					.using(FIELD_2, NEW_VALUE_2)
					.using(FIELD_3, OLD_VALUE_3)
					.load()
			);
		});
	}

	@Entity(name = "Author")
	public static class Author {

		@Id
		private Long id;

		private String name;

		@NaturalId(mutable = true)
		private String email1;

		@NaturalId(mutable = true)
		private String email2;

		@NaturalId
		private String email3;

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

		public String getEmail1() {
			return email1;
		}

		public String getEmail2() {
			return email2;
		}

		public String getEmail3() {
			return email3;
		}

		public void setEmail1(String email) {
			this.email1 = email;
		}

		public void setEmail2(String email2) {
			this.email2 = email2;
		}

		public void setEmail3(String email3) {
			this.email3 = email3;
		}

	}
}
