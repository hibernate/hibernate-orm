/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.Helper;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Andrea Boriero
 */
@RunWith(Parameterized.class)
public abstract class BaseSchemaUnitTestCase {
	protected static final Class<?>[] NO_CLASSES = new Class[0];
	protected static final String[] NO_MAPPINGS = new String[0];

	@Parameterized.Parameters
	public static Collection<String> parameters() {
		return Arrays.asList(
				new String[] {
						JdbcMetadaAccessStrategy.GROUPED.toString()
						, JdbcMetadaAccessStrategy.INDIVIDUALLY.toString()
				}
		);
	}

	@Parameterized.Parameter
	public String jdbcMetadataExtractorStrategy;

	private StandardServiceRegistry standardServiceRegistry;
	private DatabaseModel databaseModel;

	private File output;

	@Before
	public void setUp() throws IOException {
		createTempOutputFile();

		standardServiceRegistry = buildServiceRegistry();

		final MetadataImplementor metadata = buildMetadata();

		databaseModel = Helper.buildDatabaseModel( metadata );
	}

	@After
	public void tearDown() {
		try {
			if ( dropSchemaAfterTest() ) {
				createSchemaExport().drop( EnumSet.of( TargetType.DATABASE ) );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( standardServiceRegistry );
		}
	}

	public StandardServiceRegistry getStandardServiceRegistry() {
		return standardServiceRegistry;
	}

	public DatabaseModel getDatabaseModel() {
		return databaseModel;
	}

	public SchemaUpdate createSchemaUpdate() {
		SchemaUpdate schemaUpdate = new SchemaUpdate( databaseModel, standardServiceRegistry );
		if ( createSqlScriptTempOutputFile() ) {
			schemaUpdate.setOutputFile( output.getAbsolutePath() );
		}
		return schemaUpdate;
	}

	public SchemaExport createSchemaExport() {
		SchemaExport schemaExport = new SchemaExport( databaseModel, standardServiceRegistry );
		if ( createSqlScriptTempOutputFile() ) {
			schemaExport.setOutputFile( output.getAbsolutePath() );
		}
		return schemaExport;
	}

	public SchemaValidator createSchemaValidator() {
		return new SchemaValidator( databaseModel, standardServiceRegistry );
	}

	public String getSqlScriptOutputFileContent() throws IOException {
		if ( createSqlScriptTempOutputFile() ) {
			return new String( Files.readAllBytes( output.toPath() ) );
		}
		else {
			throw new RuntimeException(
					"Temporary Output file was not created, the BaseSchemaTest createSqlScriptTempOutputFile() method must be overridden to return true" );
		}
	}

	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	protected String getOutputTempScriptFileName() {
		return "update_script";
	}

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected String[] getHmbMappingFiles() {
		return NO_MAPPINGS;
	}

	protected boolean createSqlScriptTempOutputFile() {
		return false;
	}

	protected boolean dropSchemaAfterTest() {
		return true;
	}

	private void createTempOutputFile() throws IOException {
		if ( createSqlScriptTempOutputFile() ) {
			output = File.createTempFile( getOutputTempScriptFileName(), ".sql" );
			output.deleteOnExit();
		}
	}

	private StandardServiceRegistry buildServiceRegistry() {
		StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder();
		applySettings( standardServiceRegistryBuilder );
		return standardServiceRegistryBuilder
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						jdbcMetadataExtractorStrategy
				)
				.build();
	}

	private MetadataImplementor buildMetadata() {
		final MetadataSources metadataSources = new MetadataSources( standardServiceRegistry );
		addAnnotatedClass( metadataSources );
		addResources( metadataSources );

		return (MetadataImplementor) metadataSources.buildMetadata();
	}

	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
	}

	private void addResources(MetadataSources metadataSources) {
		String[] mappings = getHmbMappingFiles();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				metadataSources.addResource(
						getBaseForMappings() + mapping
				);
			}
		}
	}

	private void addAnnotatedClass(MetadataSources metadataSources) {
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		for ( int i = 0; i < annotatedClasses.length; i++ ) {
			metadataSources.addAnnotatedClass( annotatedClasses[i] );
		}
	}

}
