/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */

public class IndexTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Author.class,
		};
	}

	@Test
	public void test() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Author author = new Author();
			author.setFirstName("Vlad");
			author.setLastName("Mihalcea");
			entityManager.persist(author);
		});
	}

	//tag::schema-generation-columns-index-mapping-example[]
	@Entity
	@Table(
		name = "author",
		indexes =  @Index(
			name = "idx_author_first_last_name",
			columnList = "first_name, last_name",
			unique = false
	)
)
	public static class Author {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "first_name")
		private String firstName;

		@Column(name = "last_name")
		private String lastName;

		//Getter and setters omitted for brevity
	//end::schema-generation-columns-index-mapping-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

	//tag::schema-generation-columns-index-mapping-example[]
	}
	//end::schema-generation-columns-index-mapping-example[]
}
