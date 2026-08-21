/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.sequencegenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Locale;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
public class SequenceGeneratorOptionsTest {
	static final String TABLE_NAME = "TEST_ENTITY_TABLE";
	static final String SEQUENCE_GENERATOR_NAME = "TEST_SEQUENCE_GENERATOR";
	static final String SEQUENCE_GENERATOR_OPTIONS = "option_0";

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
	public void testSequenceOptionsAreCreated() throws Exception {
		createSchema( TestEntity.class );
		assertTrue(
				tableSequenceStatementContainsOptions( output, SEQUENCE_GENERATOR_NAME, SEQUENCE_GENERATOR_OPTIONS, metadata.getDatabase().getDialect() ),
				"Sequence " + SEQUENCE_GENERATOR_NAME + " options has not been created "
		);
	}

	@Test
	public void testXmlMappingSequenceOptionsAreCreated() throws Exception {
		createSchema( "org/hibernate/orm/test/schemaupdate/sequencegenerator/TestEntity.xml" );
		assertTrue(
				tableSequenceStatementContainsOptions( output, SEQUENCE_GENERATOR_NAME, SEQUENCE_GENERATOR_OPTIONS, metadata.getDatabase().getDialect() ),
				"Sequence " + SEQUENCE_GENERATOR_NAME + " options has not been created "
		);
	}

	private static boolean tableSequenceStatementContainsOptions(
			File output,
			String sequenceName,
			String options,
			Dialect dialect) throws Exception {
		String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		for ( int i = 0; i < fileContent.length; i++ ) {
			String statement = fileContent[i].toUpperCase( Locale.ROOT );
			if ( dialect.getSequenceSupport().supportsSequences() ) {
				if ( statement.contains( "CREATE SEQUENCE " + sequenceName.toUpperCase( Locale.ROOT ) ) ) {
					if ( statement.contains( options.toUpperCase( Locale.ROOT ) ) ) {
						return true;
					}
				}
			}
			else if ( statement.contains( "CREATE TABLE " + sequenceName.toUpperCase( Locale.ROOT ) ) ) {
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
				.createOnly( EnumSet.of( TargetType.SCRIPT ), metadata );
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
				.createOnly( EnumSet.of( TargetType.SCRIPT ), metadata );
	}

}
