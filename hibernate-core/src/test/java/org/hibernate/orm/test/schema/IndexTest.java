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


import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = IndexTest.Author.class)
public class IndexTest {
	@AfterEach
	void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
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
