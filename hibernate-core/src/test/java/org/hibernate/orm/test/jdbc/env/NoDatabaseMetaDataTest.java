/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.env;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@BaseUnitTest
public class NoDatabaseMetaDataTest {

	@Test
	@JiraKey( value = "HHH-10515" )
	@ServiceRegistry(settings = {
			@Setting(name= JdbcSettings.ALLOW_METADATA_ON_BOOT, value = "false")
	})
	public void testNoJdbcMetadataDefaultDialect(ServiceRegistryScope registryScope) {
		registryScope.withService( JdbcEnvironment.class, (jdbcEnvironment) -> {
			var extractedDatabaseMetaData = jdbcEnvironment.getExtractedDatabaseMetaData();

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
		} );
	}

	@Test
	@JiraKey( value = "HHH-10515" )
	@ServiceRegistry(
			settings = @Setting(name= JdbcSettings.ALLOW_METADATA_ON_BOOT, value = "false"),
			settingConfigurations = @SettingConfiguration(configurer = TestDialect.class)
	)
	public void testNoJdbcMetadataDialectOverride(ServiceRegistryScope registryScope) {
		registryScope.withService( JdbcEnvironment.class, (jdbcEnvironment) -> {
			var extractedDatabaseMetaData = jdbcEnvironment.getExtractedDatabaseMetaData();

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
		} );
	}

	public static class TestDialect extends Dialect implements SettingConfiguration.Configurer {
		@Override
		public DatabaseVersion getVersion() {
			return ZERO_VERSION;
		}

		@Override
		public void applySettings(StandardServiceRegistryBuilder registryBuilder) {
			registryBuilder.applySetting( AvailableSettings.DIALECT, TestDialect.class.getName() );
		}
	}

}
