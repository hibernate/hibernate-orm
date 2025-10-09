/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schematools;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.internal.ExtractionContextImpl;
import org.hibernate.tool.schema.extract.internal.InformationExtractorJdbcDatabaseMetaDataImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.SchemaToolingSettings.EXTRA_PHYSICAL_TABLE_TYPES;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10298")
public class TestExtraPhysicalTableTypes {
	@Test
	@ServiceRegistry(settings = @Setting(name = EXTRA_PHYSICAL_TABLE_TYPES, value = "BASE TABLE"))
	public void testAddOneExtraPhysicalTableType(ServiceRegistryScope registryScope) throws Exception{
		var model = buildMetadata( registryScope );
		try (var ddlTransactionIsolator = buildDdlTransactionIsolator( registryScope )) {
			var informationExtractor = buildInformationExtractor(
					ddlTransactionIsolator,
					registryScope,
					model
			);
			assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( true ) );
			assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
		}
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = EXTRA_PHYSICAL_TABLE_TYPES, value = "BASE, BASE TABLE"))
	public void testAddingMultipleExtraPhysicalTableTypes(ServiceRegistryScope registryScope) throws Exception {
		var model = buildMetadata( registryScope );
		try (var ddlTransactionIsolator = buildDdlTransactionIsolator( registryScope )) {
			var informationExtractor = buildInformationExtractor(
					ddlTransactionIsolator,
					registryScope,
					model
			);
			assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( true ) );
			assertThat( informationExtractor.isPhysicalTableType( "BASE" ), is( true ) );
			assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
			assertThat( informationExtractor.isPhysicalTableType( "TABLE 1" ), is( false ) );
		}
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = EXTRA_PHYSICAL_TABLE_TYPES, value = " "))
	public void testExtraPhysicalTableTypesPropertyEmptyStringValue(ServiceRegistryScope registryScope) throws Exception {
		var model = buildMetadata( registryScope );

		var dialect = model.getDatabase().getDialect();
		// As of 2.0.202 H2 reports tables as BASE TABLE so we add the type through the dialect
		assumeFalse( dialect instanceof H2Dialect && dialect.getVersion().isSameOrAfter( 2 ) );

		try (var ddlTransactionIsolator = buildDdlTransactionIsolator( registryScope )) {
			var informationExtractor = buildInformationExtractor(
					ddlTransactionIsolator,
					registryScope,
					model
			);
			assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( false ) );
			assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
		}
	}

	@Test
	@ServiceRegistry
	public void testNoExtraPhysicalTableTypesProperty(ServiceRegistryScope registryScope) throws Exception {
		var model = buildMetadata( registryScope );

		var dialect = model.getDatabase().getDialect();
		// As of 2.0.202 H2 reports tables as BASE TABLE so we add the type through the dialect
		assumeFalse( dialect instanceof H2Dialect && dialect.getVersion().isSameOrAfter( 2 ) );

		try (var ddlTransactionIsolator = buildDdlTransactionIsolator( registryScope )) {
			var informationExtractor = buildInformationExtractor(
					ddlTransactionIsolator,
					registryScope,
					model
			);
			assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( false ) );
			assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
		}
	}

	private InformationExtractorJdbcDatabaseMetaDataImplTest buildInformationExtractor(
			DdlTransactionIsolator ddlTransactionIsolator,
			ServiceRegistryScope registryScope,
			MetadataImplementor metadata) throws SQLException {
		Database database = metadata.getDatabase();

		SqlStringGenerationContext sqlStringGenerationContext =
				SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );

		DatabaseInformation dbInfo = new DatabaseInformationImpl(
				registryScope.getRegistry(),
				database.getJdbcEnvironment(),
				sqlStringGenerationContext,
				ddlTransactionIsolator,
				registryScope.getRegistry().requireService( SchemaManagementTool.class )
		);

		ExtractionContextImpl extractionContext = new ExtractionContextImpl(
				registryScope.getRegistry(),
				database.getJdbcEnvironment(),
				sqlStringGenerationContext,
				registryScope.getRegistry().requireService( JdbcServices.class ).getBootstrapJdbcConnectionAccess(),
				(ExtractionContext.DatabaseObjectAccess) dbInfo

		);
		return new InformationExtractorJdbcDatabaseMetaDataImplTest( extractionContext );
	}

	private DdlTransactionIsolator buildDdlTransactionIsolator(ServiceRegistryScope registryScope) {
		final ConnectionProvider connectionProvider = registryScope.getRegistry().requireService( ConnectionProvider.class );
		return new DdlTransactionIsolatorTestingImpl(
				registryScope.getRegistry(),
				new JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess( connectionProvider )
		);
	}

	private MetadataImplementor buildMetadata(ServiceRegistryScope registryScope) {
		var metadata = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		return metadata;
	}

	public static class InformationExtractorJdbcDatabaseMetaDataImplTest extends InformationExtractorJdbcDatabaseMetaDataImpl {

		private final ExtractionContext extractionContext;

		public InformationExtractorJdbcDatabaseMetaDataImplTest(ExtractionContext extractionContext) {
			super( extractionContext );
			this.extractionContext = extractionContext;
		}

		public ExtractionContext getExtractionContext() {
			return extractionContext;
		}

		public boolean isPhysicalTableType(String tableType) {
			return super.isPhysicalTableType( tableType );
		}
	}

	static class DdlTransactionIsolatorImpl implements  DdlTransactionIsolator{

		@Override
		public JdbcContext getJdbcContext() {
			return null;
		}

		@Override
		public Connection getIsolatedConnection() {
			return null;
		}

		@Override
		public Connection getIsolatedConnection(boolean autocommit) {
			return null;
		}

		@Override
		public void release() {

		}
	}
}
