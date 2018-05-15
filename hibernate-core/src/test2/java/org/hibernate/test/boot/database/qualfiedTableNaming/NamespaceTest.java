/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.boot.database.qualfiedTableNaming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11625")
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

		assertThat( physicalName.getSchema().getText(), is( EXPECTED_SCHEMA_PHYSICAL_NAME ) );
		assertThat( physicalName.getCatalog().getText(), is( EXPECTED_CATALOG_PHYSICAL_NAME ) );
	}

	public static class TestNamingStrategy implements PhysicalNamingStrategy {
		@Override
		public Identifier toPhysicalCatalogName(
				Identifier name, JdbcEnvironment jdbcEnvironment) {
			return new Identifier( EXPECTED_CATALOG_PHYSICAL_NAME, false );
		}

		@Override
		public Identifier toPhysicalSchemaName(
				Identifier name, JdbcEnvironment jdbcEnvironment) {
			return new Identifier( EXPECTED_SCHEMA_PHYSICAL_NAME, false );
		}

		@Override
		public Identifier toPhysicalTableName(
				Identifier name, JdbcEnvironment jdbcEnvironment) {
			return name;
		}

		@Override
		public Identifier toPhysicalSequenceName(
				Identifier name, JdbcEnvironment jdbcEnvironment) {
			return null;
		}

		@Override
		public Identifier toPhysicalColumnName(
				Identifier name, JdbcEnvironment jdbcEnvironment) {
			return name;
		}
	}
}
