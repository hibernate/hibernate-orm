/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema;

import org.hibernate.annotations.DefaultSchema;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-18977")
public class DefaultSchemaTest {

	@DomainModel(annotatedClasses = EntityWithDefaultSchema.class)
	@Test
	void verifyTypeLevelDefaultSchema(DomainModelScope scope) {
		scope.withHierarchy( EntityWithDefaultSchema.class, (descriptor) -> {
			final Table table = descriptor.getTable();
			assertThat( table.getSchema() )
					.as( "@DefaultSchema on entity class should set the schema" )
					.isEqualTo( "my_schema" );
		} );
	}

	@DomainModel(annotatedClasses = EntityWithDefaultCatalog.class)
	@Test
	void verifyTypeLevelDefaultCatalog(DomainModelScope scope) {
		scope.withHierarchy( EntityWithDefaultCatalog.class, (descriptor) -> {
			final Table table = descriptor.getTable();
			assertThat( table.getCatalog() )
					.as( "@DefaultSchema on entity class should set the catalog" )
					.isEqualTo( "my_catalog" );
		} );
	}

	@DomainModel(annotatedClasses = {EntityWithExplicitSchema.class})
	@Test
	void verifyExplicitSchemaOverridesDefault(DomainModelScope scope) {
		scope.withHierarchy( EntityWithExplicitSchema.class, (descriptor) -> {
			final Table table = descriptor.getTable();
			assertThat( table.getSchema() )
					.as( "Explicit @Table(schema=...) should override @DefaultSchema" )
					.isEqualTo( "explicit_schema" );
		} );
	}

	@DefaultSchema(schema = "my_schema")
	@Entity(name = "EntityWithDefaultSchema")
	public static class EntityWithDefaultSchema {
		@Id
		public Long id;
	}

	@DefaultSchema(catalog = "my_catalog")
	@Entity(name = "EntityWithDefaultCatalog")
	public static class EntityWithDefaultCatalog {
		@Id
		public Long id;
	}

	@DefaultSchema(schema = "default_schema")
	@Entity(name = "EntityWithExplicitSchema")
	@jakarta.persistence.Table(schema = "explicit_schema")
	public static class EntityWithExplicitSchema {
		@Id
		public Long id;
	}
}
