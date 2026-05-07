/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.schema;

import java.lang.annotation.Retention;
import java.sql.JDBCType;

import org.hibernate.annotations.schema.StaticColumn;
import org.hibernate.annotations.schema.StaticJoinColumn;
import org.hibernate.annotations.schema.StaticTable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.util.SchemaUtil.getColumnNames;

@DomainModel(annotatedClasses = {
		StaticSchemaAnnotationBindingTest.AuthorEntity.class,
		StaticSchemaAnnotationBindingTest.BookEntity.class
})
public class StaticSchemaAnnotationBindingTest {

	@Test
	void bindsStaticSchemaAnnotationsAsTableColumnAndJoinColumn(DomainModelScope scope) {
		final var binding = scope.getEntityBinding( BookEntity.class );

		assertThat( binding.getTable().getName() ).isEqualTo( "BOOK" );
		assertThat( getColumnNames( "BOOK", scope.getDomainModel() ) )
				.contains( "ISBN", "TITLE", "AUTHOR_ID" );
		assertThat( binding.getProperty( "author" ).getColumns().get( 0 ).getName() )
				.isEqualTo( "AUTHOR_ID" );
	}

	@Retention(RUNTIME)
	@StaticTable(name = "AUTHOR")
	public @interface AUTHOR {
		@Retention(RUNTIME)
		@StaticColumn(name = "ID", type = JDBCType.INTEGER)
		public @interface ID {
		}
	}

	@Retention(RUNTIME)
	@StaticTable(name = "BOOK")
	public @interface BOOK {
		@Retention(RUNTIME)
		@StaticColumn(name = "ISBN", type = JDBCType.VARCHAR)
		public @interface ISBN {
		}

		@Retention(RUNTIME)
		@StaticColumn(name = "TITLE", type = JDBCType.VARCHAR,
					nullable = false, length = 100)
		public @interface TITLE {
		}

		@Retention(RUNTIME)
		@StaticJoinColumn(name = "AUTHOR_ID",
				referencedTableName = "AUTHOR",
				referencedColumnName = "ID",
				type = JDBCType.INTEGER)
		public @interface AUTHOR_ID {
		}
	}

	@Entity(name = "AuthorEntity")
	@AUTHOR
	static class AuthorEntity {
		@Id
		@AUTHOR.ID
		Integer id;
	}

	@Entity(name = "BookEntity")
	@BOOK
	static class BookEntity {
		@Id
		@BOOK.ISBN
		String isbn;

		@BOOK.TITLE
		String title;

		@ManyToOne
		@BOOK.AUTHOR_ID
		AuthorEntity author;
	}
}
