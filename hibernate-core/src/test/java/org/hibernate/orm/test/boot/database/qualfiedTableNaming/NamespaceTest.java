/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.database.qualfiedTableNaming;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11625")
public class NamespaceTest {

	private static final String EXPECTED_CATALOG_PHYSICAL_NAME = "catalog";
	private static final String EXPECTED_SCHEMA_PHYSICAL_NAME = "schema";

	private final Database mockDatabase = mock( Database.class );
	private Namespace.LogicalNamespaceName name;

	@BeforeEach
	public void setUp() {
		when( mockDatabase.getPhysicalNamingStrategy() ).thenReturn( new TestNamingStrategy() );
		final var environment = mock( JdbcEnvironment.class );
		when( environment.getIdentifierHelper() ).thenReturn( IdentifierHelperBuilder.from( environment ).build() );
		when( mockDatabase.getJdbcEnvironment() ).thenReturn( environment );
		name = new Namespace.LogicalNamespaceName(
				new LogicalName( "DB1", false, true ),
				new LogicalName( "PUBLIC", false, true )
		);
	}

	@Test
	public void testPhysicalNameSchemaAndCatalog() {
		Namespace namespace = new Namespace( mockDatabase.getPhysicalNamingStrategy(), mockDatabase.getJdbcEnvironment(), name );

		final var physicalName = namespace.getPhysicalName();

		assertThat( physicalName.schema().getText(), is( EXPECTED_SCHEMA_PHYSICAL_NAME ) );
		assertThat( physicalName.catalog().getText(), is( EXPECTED_CATALOG_PHYSICAL_NAME ) );
	}

	public static class TestNamingStrategy implements PhysicalNamingStrategy {
		@Override
		@Nullable
		public PhysicalName toPhysicalCatalogName(
				@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
			return jdbcEnvironment.getPhysicalNameFactory().create( EXPECTED_CATALOG_PHYSICAL_NAME, false );
		}

		@Override
		@Nullable
		public PhysicalName toPhysicalSchemaName(
				@Nullable LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
			return jdbcEnvironment.getPhysicalNameFactory().create( EXPECTED_SCHEMA_PHYSICAL_NAME, false );
		}

		@Override
		@Nonnull
		public PhysicalName toPhysicalTableName(
				@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
			return logicalName == null ? null : jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
		}

		@Override
		@Nonnull
		public PhysicalName toPhysicalSequenceName(
				@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
			return null;
		}

		@Override
		@Nonnull
		public PhysicalName toPhysicalColumnName(
				@Nonnull LogicalName logicalName, @Nonnull PhysicalNamingContext jdbcEnvironment) {
			return logicalName == null ? null : jdbcEnvironment.getPhysicalNameFactory().create( logicalName.getText(), logicalName.isQuoted() );
		}

	@Override
	@Nonnull
	public PhysicalName toPhysicalTypeName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalPrimaryKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalForeignKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalUniqueKeyName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	@Override
	@Nonnull
	public PhysicalName toPhysicalIndexName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
		return context.getPhysicalNameFactory().create( name.getText(), name.isQuoted() );
	}

	}
}
