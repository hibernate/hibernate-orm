/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schematools;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.internal.ExtractionContextImpl;
import org.hibernate.tool.schema.extract.internal.InformationExtractorJdbcDatabaseMetaDataImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.internal.exec.ImprovedExtractionContextImpl;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import org.junit.After;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.util.DdlTransactionIsolatorTestingImpl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10298")
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
	public void testNoExtraPhysicalTabeTypesProperty() throws Exception {
		buildMetadata( null );
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

		DatabaseInformation dbInfo = new DatabaseInformationImpl(
				ssr,
				database.getJdbcEnvironment(),
				ddlTransactionIsolator,
				database.getDefaultNamespace().getName(),
				database.getServiceRegistry().getService( SchemaManagementTool.class )
		);

		ExtractionContextImpl extractionContext = new ExtractionContextImpl(
				ssr,
				database.getJdbcEnvironment(),
				ssr.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess(),
				(ExtractionContext.DatabaseObjectAccess) dbInfo,
				database.getDefaultNamespace().getPhysicalName().getCatalog(),
				database.getDefaultNamespace().getPhysicalName().getSchema()

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
			ssr = new StandardServiceRegistryBuilder().build();
		}
		else {
			ssr = new StandardServiceRegistryBuilder()
					.applySetting( Environment.EXTRA_PHYSICAL_TABLE_TYPES, extraPhysicalTableTypes )
					.build();
		}
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.buildMetadata();
		metadata.validate();
	}

	public class InformationExtractorJdbcDatabaseMetaDataImplTest extends InformationExtractorJdbcDatabaseMetaDataImpl {

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

	class DdlTransactionIsolatorImpl implements  DdlTransactionIsolator{

		@Override
		public JdbcContext getJdbcContext() {
			return null;
		}

		@Override
		public void prepare() {

		}

		@Override
		public Connection getIsolatedConnection() {
			return null;
		}

		@Override
		public void release() {

		}
	}
}
