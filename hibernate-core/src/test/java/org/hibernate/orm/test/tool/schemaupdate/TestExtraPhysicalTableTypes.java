/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.orm.test.util.DdlTransactionIsolatorTestingImpl;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.internal.ExtractionContextImpl;
import org.hibernate.tool.schema.extract.internal.InformationExtractorJdbcDatabaseMetaDataImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.internal.Helper;
import org.hibernate.tool.schema.internal.exec.JdbcContext;

import org.hibernate.testing.TestForIssue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10298")
public class TestExtraPhysicalTableTypes {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@AfterEach
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testAddOneExtraPhysicalTableType() throws Exception {
		buildMetadata( "BASE TABLE" );
		InformationExtractorJdbcDatabaseMetaDataImplTest informationExtractor = buildInformationExtractorJdbcDatabaseMetaDataImplTest();

		assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( true ) );
		assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
	}

	@Test
	public void testAddingMultipleExtraPhysicalTableTypes() throws Exception {
		buildMetadata( "BASE, BASE TABLE" );
		InformationExtractorJdbcDatabaseMetaDataImplTest informationExtractor = buildInformationExtractorJdbcDatabaseMetaDataImplTest();

		assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( true ) );
		assertThat( informationExtractor.isPhysicalTableType( "BASE" ), is( true ) );
		assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
		assertThat( informationExtractor.isPhysicalTableType( "TABLE 1" ), is( false ) );
	}

	@Test
	public void testExtraPhysicalTableTypesPropertyEmptyStringValue() throws Exception {
		buildMetadata( "  " );
		InformationExtractorJdbcDatabaseMetaDataImplTest informationExtractor = buildInformationExtractorJdbcDatabaseMetaDataImplTest();

		assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( false ) );
		assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
	}

	@Test
	public void testNoExtraPhysicalTabeTypesProperty() throws Exception {
		buildMetadata( null );
		InformationExtractorJdbcDatabaseMetaDataImplTest informationExtractor = buildInformationExtractorJdbcDatabaseMetaDataImplTest();

		assertThat( informationExtractor.isPhysicalTableType( "BASE TABLE" ), is( false ) );
		assertThat( informationExtractor.isPhysicalTableType( "TABLE" ), is( true ) );
	}

	private InformationExtractorJdbcDatabaseMetaDataImplTest buildInformationExtractorJdbcDatabaseMetaDataImplTest()
			throws SQLException {
		final DatabaseModel databaseModel = Helper.buildDatabaseModel( metadata );
		final ConnectionProvider connectionProvider = ssr.getService( ConnectionProvider.class );
		final DatabaseInformation dbInfo = new DatabaseInformationImpl(
				ssr,
				databaseModel.getJdbcEnvironment(),
				new DdlTransactionIsolatorTestingImpl( ssr,
													   new JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess(
															   connectionProvider )
				),
				databaseModel.getDefaultNamespace()
		);
		final ExtractionContextImpl extractionContext = new ExtractionContextImpl(
				ssr,
				databaseModel.getJdbcEnvironment(),
				ssr.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess(),
				(ExtractionContext.DatabaseObjectAccess) dbInfo,
				databaseModel.getDefaultNamespace().getName().getCatalog(),
				databaseModel.getDefaultNamespace().getName().getSchema()

		);
		return new InformationExtractorJdbcDatabaseMetaDataImplTest(
				extractionContext );
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
		public InformationExtractorJdbcDatabaseMetaDataImplTest(ExtractionContext extractionContext) {
			super( extractionContext );
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
