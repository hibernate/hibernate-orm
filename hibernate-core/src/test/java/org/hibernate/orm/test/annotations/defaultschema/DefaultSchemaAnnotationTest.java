/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.defaultschema;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.TableGenerator;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSchemaAnnotationTest {
	private static final String PACKAGE_SCHEMA = "package_schema";
	private static final String EXPLICIT_SCHEMA = "explicit_schema";

	@Test
	@DomainModel(annotatedClasses = {
			Book.class,
			Author.class,
			GeneratedBook.class
	})
	void appliesPackageDefaultSchema(DomainModelScope scope) {
		final Metadata domainModel = scope.getDomainModel();

		assertThat( scope.getEntityBinding( Book.class ).getTable().getSchema() ).isEqualTo( PACKAGE_SCHEMA );
		assertThat( scope.getEntityBinding( Book.class ).getJoins().get( 0 ).getTable().getSchema() )
				.isEqualTo( PACKAGE_SCHEMA );
		assertThat( domainModel.getCollectionBinding( Book.class.getName() + ".tags" ).getCollectionTable().getSchema() )
				.isEqualTo( PACKAGE_SCHEMA );
		assertThat( domainModel.getCollectionBinding( Book.class.getName() + ".authors" ).getCollectionTable().getSchema() )
				.isEqualTo( PACKAGE_SCHEMA );
		assertThat( findTable( domainModel, "book_ids" ).getSchema() ).isEqualTo( PACKAGE_SCHEMA );
	}

	@Test
	@DomainModel(annotatedClasses = ExplicitBook.class)
	void explicitSchemaTakesPrecedence(DomainModelScope scope) {
		assertThat( scope.getEntityBinding( ExplicitBook.class ).getTable().getSchema() ).isEqualTo( EXPLICIT_SCHEMA );
	}

	private static Table findTable(Metadata domainModel, String name) {
		for ( var namespace : domainModel.getDatabase().getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				if ( name.equals( table.getName() ) ) {
					return table;
				}
			}
		}
		throw new AssertionError( "Could not find table " + name );
	}

	@Entity(name = "DefaultSchemaBook")
	@jakarta.persistence.Table(name = "books")
	@SecondaryTable(name = "book_details")
	public static class Book {
		@Id
		private Integer id;

		private String title;

		@ElementCollection
		@CollectionTable(name = "book_tags")
		private Set<String> tags = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "book_authors")
		private Set<Author> authors = new HashSet<>();
	}

	@Entity(name = "DefaultSchemaAuthor")
	@jakarta.persistence.Table(name = "authors")
	public static class Author {
		@Id
		private Integer id;
	}

	@Entity(name = "DefaultSchemaGeneratedBook")
	@jakarta.persistence.Table(name = "generated_books")
	@TableGenerator(name = "book_id_generator", table = "book_ids")
	public static class GeneratedBook {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "book_id_generator")
		private Long id;
	}

	@Entity(name = "DefaultSchemaExplicitBook")
	@jakarta.persistence.Table(name = "explicit_books", schema = EXPLICIT_SCHEMA)
	public static class ExplicitBook {
		@Id
		private Integer id;
	}
}
