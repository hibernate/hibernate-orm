/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.database.qualfiedTableNaming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
	private Namespace.Name name;

	@Before
	public void setUp() {
		when( mockDatabase.getPhysicalNamingStrategy() ).thenReturn( new TestNamingStrategy() );
		name = new Namespace.Name(
				Identifier.toIdentifier( "DB1" ),
				Identifier.toIdentifier( "PUBLIC" )
		);
	}

	@Test
	public void testPhysicalNameSchemaAndCatalog() {
		Namespace namespace = new Namespace( mockDatabase.getPhysicalNamingStrategy(), mockDatabase.getJdbcEnvironment(), name );

		final Namespace.Name physicalName = namespace.getPhysicalName();

		assertThat( physicalName.schema().getText(), is( EXPECTED_SCHEMA_PHYSICAL_NAME ) );
		assertThat( physicalName.catalog().getText(), is( EXPECTED_CATALOG_PHYSICAL_NAME ) );
	}

	public static class TestNamingStrategy implements PhysicalNamingStrategy {
		@Override
		public Identifier toPhysicalCatalogName(
				Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return new Identifier( EXPECTED_CATALOG_PHYSICAL_NAME, false );
		}

		@Override
		public Identifier toPhysicalSchemaName(
				Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return new Identifier( EXPECTED_SCHEMA_PHYSICAL_NAME, false );
		}

		@Override
		public Identifier toPhysicalTableName(
				Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return logicalName;
		}

		@Override
		public Identifier toPhysicalSequenceName(
				Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return null;
		}

		@Override
		public Identifier toPhysicalColumnName(
				Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
			return logicalName;
		}
	}
}
