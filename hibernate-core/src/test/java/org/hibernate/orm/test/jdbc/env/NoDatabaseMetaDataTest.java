/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.env;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.Test;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class NoDatabaseMetaDataTest extends BaseUnitTestCase {

	@Test
	@JiraKey( value = "HHH-10515" )
	public void testNoJdbcMetadataDefaultDialect() {
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.temp.use_jdbc_metadata_defaults", "false" )
				.build();
		JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		ExtractedDatabaseMetaData extractedDatabaseMetaData = jdbcEnvironment.getExtractedDatabaseMetaData();

		assertNull( extractedDatabaseMetaData.getConnectionCatalogName() );
		assertNull( extractedDatabaseMetaData.getConnectionSchemaName() );
		assertFalse( extractedDatabaseMetaData.supportsNamedParameters() );
		assertFalse( extractedDatabaseMetaData.supportsRefCursors() );
		assertFalse( extractedDatabaseMetaData.supportsScrollableResults() );
		assertFalse( extractedDatabaseMetaData.supportsGetGeneratedKeys() );
		assertTrue( extractedDatabaseMetaData.supportsBatchUpdates() );
		assertFalse( extractedDatabaseMetaData.supportsDataDefinitionInTransaction() );
		assertFalse( extractedDatabaseMetaData.doesDataDefinitionCauseTransactionCommit() );
		assertNull( extractedDatabaseMetaData.getSqlStateType() );

		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Test
	@JiraKey( value = "HHH-10515" )
	public void testNoJdbcMetadataDialectOverride() {
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.temp.use_jdbc_metadata_defaults", "false" )
				.applySetting( AvailableSettings.DIALECT, TestDialect.class.getName() )
				.build();
		JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );
		ExtractedDatabaseMetaData extractedDatabaseMetaData = jdbcEnvironment.getExtractedDatabaseMetaData();

		assertNull( extractedDatabaseMetaData.getConnectionCatalogName() );
		assertNull( extractedDatabaseMetaData.getConnectionSchemaName() );
		assertTrue( extractedDatabaseMetaData.supportsNamedParameters() );
		assertFalse( extractedDatabaseMetaData.supportsRefCursors() );
		assertFalse( extractedDatabaseMetaData.supportsScrollableResults() );
		assertFalse( extractedDatabaseMetaData.supportsGetGeneratedKeys() );
		assertTrue( extractedDatabaseMetaData.supportsBatchUpdates() );
		assertFalse( extractedDatabaseMetaData.supportsDataDefinitionInTransaction() );
		assertFalse( extractedDatabaseMetaData.doesDataDefinitionCauseTransactionCommit() );
		assertNull( extractedDatabaseMetaData.getSqlStateType() );

		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	public static class TestDialect extends Dialect {
		@Override
		public boolean supportsNamedParameters(java.sql.DatabaseMetaData databaseMetaData) {
			return true;
		}

		@Override
		public DatabaseVersion getVersion() {
			return ZERO_VERSION;
		}
	}

}
