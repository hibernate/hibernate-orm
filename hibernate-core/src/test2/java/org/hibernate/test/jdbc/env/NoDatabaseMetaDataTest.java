/**
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jdbc.env;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class NoDatabaseMetaDataTest extends BaseUnitTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-10515" )
	public void testNoJdbcMetadataDefaultDialect() {
		final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.temp.use_jdbc_metadata_defaults", "false" )
				.build();
		JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		ExtractedDatabaseMetaData extractedDatabaseMetaData = jdbcEnvironment.getExtractedDatabaseMetaData();

		assertNull( extractedDatabaseMetaData.getConnectionCatalogName() );
		assertNull( extractedDatabaseMetaData.getConnectionSchemaName() );
		assertTrue( extractedDatabaseMetaData.getTypeInfoSet().isEmpty() );
		assertTrue( extractedDatabaseMetaData.getExtraKeywords().isEmpty() );
		assertFalse( extractedDatabaseMetaData.supportsNamedParameters() );
		assertFalse( extractedDatabaseMetaData.supportsRefCursors() );
		assertFalse( extractedDatabaseMetaData.supportsScrollableResults() );
		assertFalse( extractedDatabaseMetaData.supportsGetGeneratedKeys() );
		assertFalse( extractedDatabaseMetaData.supportsBatchUpdates() );
		assertFalse( extractedDatabaseMetaData.supportsDataDefinitionInTransaction() );
		assertFalse( extractedDatabaseMetaData.doesDataDefinitionCauseTransactionCommit() );
		assertNull( extractedDatabaseMetaData.getSqlStateType() );
		assertFalse( extractedDatabaseMetaData.doesLobLocatorUpdateCopy() );

		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10515" )
	public void testNoJdbcMetadataDialectOverride() {
		final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.temp.use_jdbc_metadata_defaults", "false" )
				.applySetting( AvailableSettings.DIALECT, TestDialect.class.getName() )
				.build();
		JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		ExtractedDatabaseMetaData extractedDatabaseMetaData = jdbcEnvironment.getExtractedDatabaseMetaData();

		assertNull( extractedDatabaseMetaData.getConnectionCatalogName() );
		assertNull( extractedDatabaseMetaData.getConnectionSchemaName() );
		assertTrue( extractedDatabaseMetaData.getTypeInfoSet().isEmpty() );
		assertTrue( extractedDatabaseMetaData.getExtraKeywords().isEmpty() );
		assertTrue( extractedDatabaseMetaData.supportsNamedParameters() );
		assertFalse( extractedDatabaseMetaData.supportsRefCursors() );
		assertFalse( extractedDatabaseMetaData.supportsScrollableResults() );
		assertFalse( extractedDatabaseMetaData.supportsGetGeneratedKeys() );
		assertFalse( extractedDatabaseMetaData.supportsBatchUpdates() );
		assertFalse( extractedDatabaseMetaData.supportsDataDefinitionInTransaction() );
		assertFalse( extractedDatabaseMetaData.doesDataDefinitionCauseTransactionCommit() );
		assertNull( extractedDatabaseMetaData.getSqlStateType() );
		assertFalse( extractedDatabaseMetaData.doesLobLocatorUpdateCopy() );

		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	public static class TestDialect extends Dialect {
		@Override
		public boolean supportsNamedParameters(java.sql.DatabaseMetaData databaseMetaData) {
			return true;
		}
	}

}

