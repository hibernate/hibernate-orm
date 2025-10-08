/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component;


import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Struct;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@DomainModel(
		annotatedClasses = {
				StructComponentEnumTest.Book.class,
				StructComponentEnumTest.Publisher.class
		}
)
@SessionFactory
@RequiresDialect(PostgreSQLDialect.class)
public class StructComponentEnumTest {

	@Test
	public void save(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {

					Book book = new Book();
					book.title = "Hibernate";
					book.author = "Steve";
					book.publisher = new Publisher( "penguin", PublisherType.TRADITIONAL );

					session.persist( book );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		private Publisher publisher;
	}

	@Embeddable
	@Struct( name = "publisher_type")
	public static class Publisher {

		private String name;

		@Enumerated(EnumType.STRING)
		@JdbcTypeCode(SqlTypes.NAMED_ENUM)
		private PublisherType type;

		public Publisher(String name, PublisherType type) {
			this.name = name;
			this.type = type;
		}

		public Publisher() {
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public PublisherType getType() {
			return type;
		}

		public void setType(PublisherType type) {
			this.type = type;
		}
	}

	public enum PublisherType {
		TRADITIONAL, SELF_PUBLISHED, PRINT_ON_DEMAND
	}


}
