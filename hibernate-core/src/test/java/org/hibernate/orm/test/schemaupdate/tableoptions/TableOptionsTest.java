package org.hibernate.orm.test.schemaupdate.tableoptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Locale;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsTableOptions.class)
public class TableOptionsTest {
	static final String TABLE_NAME = "PRIMARY_TABLE";
	static final String TABLE_OPTIONS = "option_1";

	static final String SECONDARY_TABLE_NAME = "SECOND_TABLE";
	static final String SECONDARY_TABLE_OPTIONS = "option_2";

	static final String JOIN_TABLE_NAME = "JOIN_TABLE";
	static final String JOIN_TABLE_OPTIONS = "option_3";

	static final String COLLECTION_TABLE_NAME = "COLLECTION_TABLE";
	static final String COLLECTION_TABLE_OPTIONS = "option_4";

	private File output;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeEach
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterEach
	public void tearsDown() {
		output.delete();
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testTableCommentAreCreated() throws Exception {
		createSchema( TestEntity.class );
		assertTrue(
				tableCreationStatementContainsOptions( output, TABLE_NAME, TABLE_OPTIONS ),
				"Table " + TABLE_NAME + " options has not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions( output, SECONDARY_TABLE_NAME, SECONDARY_TABLE_OPTIONS ),
				"SecondaryTable " + SECONDARY_TABLE_NAME + " options has not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions( output, JOIN_TABLE_NAME, JOIN_TABLE_OPTIONS ),
				"Join Table " + JOIN_TABLE_NAME + " options has not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions( output, COLLECTION_TABLE_NAME, COLLECTION_TABLE_OPTIONS ),
				"Join Table " + COLLECTION_TABLE_NAME + " options has not been created "
		);
	}

	@Test
	public void testXmlMappingTableCommentAreCreated() throws Exception {
		createSchema( "org/hibernate/orm/test/schemaupdate/tableoptions/TestEntity.xml" );
		assertTrue(
				tableCreationStatementContainsOptions( output, TABLE_NAME, TABLE_OPTIONS ),
				"Table " + TABLE_NAME + " options has not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions( output, SECONDARY_TABLE_NAME, SECONDARY_TABLE_OPTIONS ),
				"SecondaryTable " + SECONDARY_TABLE_NAME + " options has not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions( output, JOIN_TABLE_NAME, JOIN_TABLE_OPTIONS ),
				"Join Table " + JOIN_TABLE_NAME + " options has not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions( output, COLLECTION_TABLE_NAME, COLLECTION_TABLE_OPTIONS ),
				"Join Table " + COLLECTION_TABLE_NAME + " options has not been created "
		);
	}

	private static boolean tableCreationStatementContainsOptions(
			File output,
			String tableName,
			String options) throws Exception {
		String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		for ( int i = 0; i < fileContent.length; i++ ) {
			String statement = fileContent[i].toUpperCase( Locale.ROOT );
			if ( statement.contains( "CREATE TABLE " + tableName.toUpperCase( Locale.ROOT ) ) ) {
				if ( statement.contains( options.toUpperCase( Locale.ROOT ) ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private void createSchema(String... xmlMapping) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( String xml : xmlMapping ) {
			metadataSources.addResource( xml );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );
	}

	private void createSchema(Class... annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class c : annotatedClasses ) {
			metadataSources.addAnnotatedClass( c );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );
	}

}
