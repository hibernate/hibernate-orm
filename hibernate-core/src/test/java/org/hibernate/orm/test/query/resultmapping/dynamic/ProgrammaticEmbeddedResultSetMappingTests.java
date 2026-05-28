/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping.dynamic;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.sql.EmbeddedMapping;
import jakarta.persistence.sql.EntityMapping;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.sql.ResultSetMapping.embedded;
import static jakarta.persistence.sql.ResultSetMapping.entity;
import static jakarta.persistence.sql.ResultSetMapping.field;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = ProgrammaticEmbeddedResultSetMappingTests.SqlMappingEmbeddedBook.class )
@SessionFactory
public class ProgrammaticEmbeddedResultSetMappingTests {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			session.persist( new SqlMappingEmbeddedBook(
					1,
					"The Left Hand of Darkness",
					new SqlMappingDetails( "isbn-1", "fiction" )
			) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void programmaticEmbeddedSqlResultSetMappingTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			EmbeddedMapping<SqlMappingEmbeddedBook, SqlMappingDetails> details =
					embedded( SqlMappingEmbeddedBook.class, SqlMappingDetails.class, "details",
							field( SqlMappingDetails.class, String.class, "isbn", "BOOK_ISBN" ),
							field( SqlMappingDetails.class, String.class, "category", "BOOK_CATEGORY" ) );
			EntityMapping<SqlMappingEmbeddedBook> mapping = entity( SqlMappingEmbeddedBook.class,
					field( SqlMappingEmbeddedBook.class, Integer.class, "id", "BOOK_ID" ),
					field( SqlMappingEmbeddedBook.class, String.class, "title", "BOOK_TITLE" ),
					details );

			SqlMappingEmbeddedBook book = session
					.createNativeQuery(
							"SELECT ID AS BOOK_ID, TITLE AS BOOK_TITLE, ISBN AS BOOK_ISBN, CATEGORY AS BOOK_CATEGORY "
									+ "FROM JPA40_SQL_EMBEDDED_BOOK WHERE ID = 1",
							mapping )
					.getSingleResult();

			assertThat( book.getDetails() ).isNotNull();
			assertThat( book.getDetails().getIsbn() ).isEqualTo( "isbn-1" );
			assertThat( book.getDetails().getCategory() ).isEqualTo( "fiction" );
		} );
	}

	@Entity( name = "SqlMappingEmbeddedBook" )
	@Table( name = "JPA40_SQL_EMBEDDED_BOOK" )
	public static class SqlMappingEmbeddedBook {
		@Id
		@Column( name = "ID" )
		private Integer id;

		@Column( name = "TITLE" )
		private String title;

		@Embedded
		private SqlMappingDetails details;

		protected SqlMappingEmbeddedBook() {
		}

		public SqlMappingEmbeddedBook(Integer id, String title, SqlMappingDetails details) {
			this.id = id;
			this.title = title;
			this.details = details;
		}

		public SqlMappingDetails getDetails() {
			return details;
		}
	}

	@Embeddable
	public static class SqlMappingDetails {
		@Column( name = "ISBN" )
		private String isbn;

		@Column( name = "CATEGORY" )
		private String category;

		protected SqlMappingDetails() {
		}

		public SqlMappingDetails(String isbn, String category) {
			this.isbn = isbn;
			this.category = category;
		}

		public String getIsbn() {
			return isbn;
		}

		public String getCategory() {
			return category;
		}
	}
}
