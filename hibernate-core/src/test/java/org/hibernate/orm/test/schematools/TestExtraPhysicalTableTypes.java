/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schematools;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.internal.ExtractionContextImpl;
import org.hibernate.tool.schema.extract.internal.InformationExtractorJdbcDatabaseMetaDataImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10298")
public class TestExtraPhysicalTableTypes {

	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;
	@After
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testAddOneExtraPhysicalTableType() throws Exception {
		buildMetadata( "BASE TABLE" );
		DdlTransactionIsolator ddlTransactionIsolator = buildDdlTransactionIsolator();
		try {
			InformationExtractorJdbcDatabaseMetaDataImplTest informationExtractor = buildInformationExtractorJdbcDatabaseMetaDataImplTest(
					ddlTransactionIsolator
			);
			assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( true ) );
			assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
		}
		finally {
			ddlTransactionIsolator.release();
		}
	}

	@Test
	public void testAddingMultipleExtraPhysicalTableTypes() throws Exception {
		buildMetadata( "BASE, BASE TABLE" );
		DdlTransactionIsolator ddlTransactionIsolator = buildDdlTransactionIsolator();
		try {
			InformationExtractorJdbcDatabaseMetaDataImplTest informationExtractor = buildInformationExtractorJdbcDatabaseMetaDataImplTest(
					ddlTransactionIsolator
			);
			assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( true ) );
			assertThat( informationExtractor.isPhysicalTableType( "BASE" ), is( true ) );
			assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
			assertThat( informationExtractor.isPhysicalTableType( "TABLE 1" ), is( false ) );
		}
		finally {
			ddlTransactionIsolator.release();
		}
	}

	@Test
	public void testExtraPhysicalTableTypesPropertyEmptyStringValue() throws Exception {
		buildMetadata( "  " );
		Dialect dialect = metadata.getDatabase().getDialect();
		// As of 2.0.202 H2 reports tables as BASE TABLE so we add the type through the dialect
		Assume.assumeFalse( dialect instanceof H2Dialect && dialect.getVersion().isSameOrAfter( 2 ) );
		DdlTransactionIsolator ddlTransactionIsolator = buildDdlTransactionIsolator();
		try {
			InformationExtractorJdbcDatabaseMetaDataImplTest informationExtractor = buildInformationExtractorJdbcDatabaseMetaDataImplTest(
					ddlTransactionIsolator
			);
			assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( false ) );
			assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
		}
		finally {
			ddlTransactionIsolator.release();
		}
	}

	@Test
	public void testNoExtraPhysicalTableTypesProperty() throws Exception {
		buildMetadata( null );
		Dialect dialect = metadata.getDatabase().getDialect();
		// As of 2.0.202 H2 reports tables as BASE TABLE so we add the type through the dialect
		Assume.assumeFalse( dialect instanceof H2Dialect && dialect.getVersion().isSameOrAfter( 2 ) );
		DdlTransactionIsolator ddlTransactionIsolator = buildDdlTransactionIsolator();
		try {
			InformationExtractorJdbcDatabaseMetaDataImplTest informationExtractor = buildInformationExtractorJdbcDatabaseMetaDataImplTest(
					ddlTransactionIsolator
			);
			assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( false ) );
			assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
		}
		finally {
			ddlTransactionIsolator.release();
		}
	}

	private InformationExtractorJdbcDatabaseMetaDataImplTest buildInformationExtractorJdbcDatabaseMetaDataImplTest(DdlTransactionIsolator ddlTransactionIsolator)
			throws SQLException {
		Database database = metadata.getDatabase();

		SqlStringGenerationContext sqlStringGenerationContext =
				SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );

		DatabaseInformation dbInfo = new DatabaseInformationImpl(
				ssr,
				database.getJdbcEnvironment(),
				sqlStringGenerationContext,
				ddlTransactionIsolator,
				database.getServiceRegistry().getService( SchemaManagementTool.class )
		);

		ExtractionContextImpl extractionContext = new ExtractionContextImpl(
				ssr,
				database.getJdbcEnvironment(),
				sqlStringGenerationContext,
				ssr.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess(),
				(ExtractionContext.DatabaseObjectAccess) dbInfo

		);
		return new InformationExtractorJdbcDatabaseMetaDataImplTest(
				extractionContext );
	}

	private DdlTransactionIsolator buildDdlTransactionIsolator() {
		final ConnectionProvider connectionProvider = ssr.getService( ConnectionProvider.class );
		return new DdlTransactionIsolatorTestingImpl(
				ssr,
				new JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess( connectionProvider )
		);
	}

	private void buildMetadata(String extraPhysicalTableTypes) {
		if ( extraPhysicalTableTypes == null ) {
			ssr = ServiceRegistryUtil.serviceRegistry();
		}
		else {
			ssr = ServiceRegistryUtil.serviceRegistryBuilder()
					.applySetting( Environment.EXTRA_PHYSICAL_TABLE_TYPES, extraPhysicalTableTypes )
					.build();
		}
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
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
