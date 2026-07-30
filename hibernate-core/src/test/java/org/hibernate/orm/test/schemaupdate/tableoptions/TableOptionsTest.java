/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.tableoptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Locale;

import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
public class TableOptionsTest {

	static final String TABLE_GENERATOR_NAME = "TEST_TABLE_GENERATOR";
	static final String TABLE_GENERATOR_OPTIONS = "option_0";

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
	public void testTableOptionsAreCreated() throws Exception {
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
				"JoinTable " + JOIN_TABLE_NAME + " options has not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions( output, COLLECTION_TABLE_NAME, COLLECTION_TABLE_OPTIONS ),
				"JoinTable " + COLLECTION_TABLE_NAME + " options has not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions( output, TABLE_GENERATOR_NAME, TABLE_GENERATOR_OPTIONS ),
				"TableGenerator " + COLLECTION_TABLE_NAME + " options has not been created "
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

		assertTrue(
				tableCreationStatementContainsOptions( output, TABLE_GENERATOR_NAME, TABLE_GENERATOR_OPTIONS ),
				"TableGenerator " + TABLE_GENERATOR_NAME + " options has not been created "
		);
	}

	private boolean tableCreationStatementContainsOptions(
			File output,
			String tableName,
			String options) throws Exception {
		final String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		final String createTable = metadata.getDatabase().getDialect().getCreateTableString().toUpperCase( Locale.ROOT ) + " ";
		for ( final String s : fileContent ) {
			final String statement = s.toUpperCase( Locale.ROOT );
			if ( statement.contains( createTable + tableName.toUpperCase( Locale.ROOT ) ) ) {
				if ( statement.contains( options.toUpperCase( Locale.ROOT ) ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private void createSchema(String... xmlMapping) {
		final MappingSources mappingSources = new MappingSources();
		for ( String xmlMappingResource : xmlMapping ) {
			mappingSources.addMappingResource( xmlMappingResource );
		}
		metadata = MetadataBuildingTestHelper.buildValidatedMetadata( ssr, mappingSources );
		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.createOnly( EnumSet.of( TargetType.SCRIPT ), metadata );
	}

	private void createSchema(Class... annotatedClasses) {
		metadata = MetadataBuildingTestHelper.buildValidatedMetadata( ssr, annotatedClasses );
		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.createOnly( EnumSet.of( TargetType.SCRIPT ), metadata );
	}

}
