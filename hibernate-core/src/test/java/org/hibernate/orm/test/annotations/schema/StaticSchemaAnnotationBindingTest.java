/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.schema;

import java.lang.annotation.Retention;

import org.hibernate.annotations.schema.ColumnMapping;
import org.hibernate.annotations.schema.JoinColumnMapping;
import org.hibernate.annotations.schema.TableMapping;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
	@TableMapping(@Table(name = "AUTHOR"))
	public @interface AUTHOR {
		@Retention(RUNTIME)
		@ColumnMapping(@Column(name = "ID"))
		public @interface ID {
		}
	}

	@Retention(RUNTIME)
	@TableMapping(@Table(name = "BOOK"))
	public @interface BOOK {
		@Retention(RUNTIME)
		@ColumnMapping(@Column(name = "ISBN"))
		@interface ISBN {
		}

		@Retention(RUNTIME)
		@ColumnMapping(@Column(name = "TITLE", nullable = false, length = 100))
		@interface TITLE {
		}

		@Retention(RUNTIME)
		@JoinColumnMapping(@JoinColumn(name = "AUTHOR_ID", referencedColumnName = "ID"))
		@interface AUTHOR_ID {
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
