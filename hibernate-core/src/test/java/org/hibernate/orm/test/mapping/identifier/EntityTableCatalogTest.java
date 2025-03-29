/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;


/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(MySQLDialect.class)
public class EntityTableCatalogTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class
		};
	}

	@Test
	public void test() {

	}

	//tag::mapping-entity-table-catalog-mysql-example[]
	@Entity(name = "Book")
	@Table(
		catalog = "public",
		name = "book"
	)
	public static class Book {

		@Id
		private Long id;

		private String title;

		private String author;

		//Getters and setters are omitted for brevity
	//end::mapping-entity-table-catalog-mysql-example[]

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
	//tag::mapping-entity-table-catalog-mysql-example[]
	}
	//end::mapping-entity-table-catalog-mysql-example[]
}
