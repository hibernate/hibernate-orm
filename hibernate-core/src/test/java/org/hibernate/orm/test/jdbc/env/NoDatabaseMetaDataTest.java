/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.env;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
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
import static org.junit.jupiter.api.Assertions.assertSame;
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
			var jdbcMetadata = jdbcEnvironment.getJdbcMetadata();
			var extractedDatabaseMetaData = jdbcMetadata.getExtractedDatabaseMetaData();
			assertSame( jdbcMetadata, jdbcEnvironment.getJdbcMetadata() );
			assertSame( extractedDatabaseMetaData, jdbcMetadata.getExtractedDatabaseMetaData() );
			assertFalse( jdbcMetadata.isJdbcMetadataAccessible() );
			assertFalse( extractedDatabaseMetaData.isJdbcMetadataAccessible() );
			assertSame( IdentifierCaseStrategy.UPPER, jdbcMetadata.getUnquotedIdentifierCaseStrategy() );
			assertSame( IdentifierCaseStrategy.MIXED, jdbcMetadata.getQuotedIdentifierCaseStrategy() );
			assertTrue( jdbcMetadata.getSqlKeywords().isEmpty() );

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
			assertFalse( jdbcMetadata.supportsNamedParameters() );
			assertFalse( jdbcMetadata.supportsRefCursors() );
			assertTrue( jdbcMetadata.supportsBatchUpdates() );
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
			var jdbcMetadata = jdbcEnvironment.getJdbcMetadata();
			var extractedDatabaseMetaData = jdbcMetadata.getExtractedDatabaseMetaData();

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
			assertTrue( jdbcMetadata.supportsNamedParameters() );
			assertTrue( jdbcMetadata.supportsRefCursors() );
			assertFalse( jdbcMetadata.supportsBatchUpdates() );
		} );
	}

	public static class TestDialect extends Dialect implements SettingConfiguration.Configurer {
		private static final JdbcMetadataOverrides JDBC_METADATA_OVERRIDES = JdbcMetadataOverrides.builder()
				.namedParameterSupport( JdbcMetadataOverrides.SupportOverride.SUPPORTED )
				.batchUpdateSupport( JdbcMetadataOverrides.SupportOverride.UNSUPPORTED )
				.standardRefCursorSupport( JdbcMetadataOverrides.SupportOverride.SUPPORTED )
				.build();

		public TestDialect() {
			super( ZERO_VERSION );
		}

		@Override
		public void applySettings(StandardServiceRegistryBuilder registryBuilder) {
			registryBuilder.applySetting( AvailableSettings.DIALECT, TestDialect.class.getName() );
		}

		@Override
		public JdbcMetadataOverrides getJdbcMetadataOverrides() {
			return JDBC_METADATA_OVERRIDES;
		}
	}

}
