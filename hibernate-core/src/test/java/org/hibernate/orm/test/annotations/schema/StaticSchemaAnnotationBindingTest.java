/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.schema;

import java.lang.annotation.Retention;
import java.sql.JDBCType;

import org.hibernate.annotations.schema.StaticColumn;
import org.hibernate.annotations.schema.StaticTable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.util.SchemaUtil.getColumnNames;

@DomainModel(annotatedClasses = StaticSchemaAnnotationBindingTest.BookEntity.class)
public class StaticSchemaAnnotationBindingTest {

	@Test
	void bindsStaticSchemaAnnotationsAsTableAndColumn(DomainModelScope scope) {
		final var binding = scope.getEntityBinding( BookEntity.class );

		assertThat( binding.getTable().getName() ).isEqualTo( "BOOK" );
		assertThat( getColumnNames( "BOOK", scope.getDomainModel() ) )
				.contains( "ISBN", "TITLE" );
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
	}

	@Entity(name = "BookEntity")
	@BOOK
	static class BookEntity {
		@Id
		@BOOK.ISBN
		String isbn;

		@BOOK.TITLE
		String title;
	}
}
