/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.index;

import java.util.List;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@RequiresDialect(H2Dialect.class)
@ServiceRegistry
@DomainModel(annotatedClasses = {
		Jpa4TableAndIndexDdlTest.TypedTableEntity.class,
		Jpa4TableAndIndexDdlTest.TypedIndexEntity.class
})
public class Jpa4TableAndIndexDdlTest {
	@Test
	public void testJpa4TableTypeAndIndexTypeUsingAreGenerated(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope) {
		final List<String> commands = new SchemaCreatorImpl( registryScope.getRegistry() )
				.generateCreationCommands( modelScope.getDomainModel(), false );

		assertThat( commands ).anySatisfy( command ->
				assertThat( command ).containsIgnoringCase( "create global temporary table typed_table" )
		);
		assertThat( commands ).anySatisfy( command ->
				assertThat( command ).containsIgnoringCase(
						"create hash index typed_index on typed_index_table using btree (name) include (id)"
				)
		);
		assertThat( commands ).anySatisfy( command ->
				assertThat( command ).containsIgnoringCase(
						"create unique index unique_type_index on typed_index_table (code)"
				)
		);
	}

	@Entity(name = "TypedTableEntity")
	@Table(name = "typed_table", type = "global temporary")
	public static class TypedTableEntity {
		@Id
		private Long id;
	}

	@Entity(name = "TypedIndexEntity")
	@Table(
			name = "typed_index_table",
			indexes = {
					@Index(
							name = "typed_index",
							columnList = "name",
							type = "hash",
							using = "btree",
							options = "include (id)"
					),
					@Index(
							name = "unique_type_index",
							columnList = "code",
							type = "unique"
					)
			}
	)
	public static class TypedIndexEntity {
		@Id
		private Long id;

		@Column(name = "name")
		private String name;

		@Column(name = "code")
		private String code;
	}
}
