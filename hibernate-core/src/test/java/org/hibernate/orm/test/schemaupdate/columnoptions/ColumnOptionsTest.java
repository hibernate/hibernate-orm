/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.columnoptions;

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
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
@JiraKey("HHH-18057")
public class ColumnOptionsTest {
	static final String COLUMN_NAME = "NAME";
	static final String COLUMN_OPTIONS = "option_1";

	static final String DISCRIMINATOR_COLUMN_NAME = "DISC_COLUMN";
	static final String DISCRIMINATOR_COLUMN_OPTIONS = "option_2";

	static final String PRIMARY_KEY_JOIN_COLUMN_NAME = "PRIMARY_KEY_COLUMN";
	static final String PRIMARY_KEY_JOIN_COLUMN_OPTIONS = "option_3";

	static final String JOIN_COLUMN_NAME = "JOIN_COLUMN";
	static final String JOIN_COLUMN_OPTIONS = "option_4";

	static final String ELEMENT_COLLECTION_MAP_KEY_COLUMN_NAME = "MAP_KEY_COLUMN";
	static final String ELEMENT_COLLECTION_MAP_KEY_COLUMN_OPTIONS = "option_5";

	static final String ELEMENT_COLLECTION_MAP_KEY_JOIN_COLUMN_NAME = "MAP_KEY_JOIN_COLUMN";
	static final String ELEMENT_COLLECTION_MAP_KEY_JOIN_COLUMN_OPTIONS = "option_6";

	static final String ONE_TO_MANY_MAP_KEY_COLUMN_NAME = "MAP_KEY_COLUMN_2";
	static final String ONE_TO_MANY_MAP_KEY_COLUMN_OPTIONS = "option_7";

	static final String ONE_TO_MANY_MAP_KEY_JOIN_COLUMN_NAME = "MAP_KEY_JOIN_COLUMN_2";
	static final String ONE_TO_MANY_MAP_KEY_JOIN_COLUMN_OPTIONS = "option_8";

	static final String MANY_TO_MANY_MAP_KEY_COLUMN_NAME = "MAP_KEY_COLUMN_3";
	static final String MANY_TO_MANY_MAP_KEY_COLUMN_OPTIONS = "option_9";

	static final String MANY_TO_MANY_MAP_KEY_JOIN_COLUMN_NAME = "MAP_KEY_JOIN_COLUMN_3";
	static final String MANY_TO_MANY_MAP_KEY_JOIN_COLUMN_OPTIONS = "option__10";

	static final String ORDER_COLUMN_NAME = "ORDER_COLUMN";
	static final String ORDER_COLUMN_OPTIONS = "option__11";

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
		createSchema( TestEntity.class, AnotherTestEntity.class );
		assertTrue(
				tableCreationStatementContainsOptions( output, COLUMN_NAME, COLUMN_OPTIONS ),
				"Column " + COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						DISCRIMINATOR_COLUMN_NAME,
						DISCRIMINATOR_COLUMN_OPTIONS
				),
				"DiscriminatorColumn " + DISCRIMINATOR_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions( output, JOIN_COLUMN_NAME, JOIN_COLUMN_OPTIONS ),
				"JoinColumn " + JOIN_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ELEMENT_COLLECTION_MAP_KEY_COLUMN_NAME,
						ELEMENT_COLLECTION_MAP_KEY_COLUMN_OPTIONS
				),
				"ElementCollection MapKeyColumn " + ELEMENT_COLLECTION_MAP_KEY_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ONE_TO_MANY_MAP_KEY_COLUMN_NAME,
						ONE_TO_MANY_MAP_KEY_COLUMN_OPTIONS
				),
				"OneToMany MapKeyColumn " + ONE_TO_MANY_MAP_KEY_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						MANY_TO_MANY_MAP_KEY_COLUMN_NAME,
						MANY_TO_MANY_MAP_KEY_COLUMN_OPTIONS
				),
				"ManyToMany MapKeyColumn " + MANY_TO_MANY_MAP_KEY_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ONE_TO_MANY_MAP_KEY_COLUMN_NAME,
						ONE_TO_MANY_MAP_KEY_COLUMN_OPTIONS
				),
				"OneToMany MapKeyColumn " + ONE_TO_MANY_MAP_KEY_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						PRIMARY_KEY_JOIN_COLUMN_NAME,
						PRIMARY_KEY_JOIN_COLUMN_OPTIONS
				),
				"PrimaryKeyJoinColumn " + PRIMARY_KEY_JOIN_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ELEMENT_COLLECTION_MAP_KEY_JOIN_COLUMN_NAME,
						ELEMENT_COLLECTION_MAP_KEY_JOIN_COLUMN_OPTIONS
				),
				"ElementCollection MapKeyJoinColumn " + ELEMENT_COLLECTION_MAP_KEY_JOIN_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ONE_TO_MANY_MAP_KEY_JOIN_COLUMN_NAME,
						ONE_TO_MANY_MAP_KEY_JOIN_COLUMN_OPTIONS
				),
				"OneToMany MapKeyJoinColumn " + ONE_TO_MANY_MAP_KEY_JOIN_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						MANY_TO_MANY_MAP_KEY_JOIN_COLUMN_NAME,
						MANY_TO_MANY_MAP_KEY_JOIN_COLUMN_OPTIONS
				),
				"ManyToMany MapKeyJoinColumn " + MANY_TO_MANY_MAP_KEY_JOIN_COLUMN_OPTIONS + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ORDER_COLUMN_NAME,
						ORDER_COLUMN_OPTIONS
				),
				"OrderColumn " + ORDER_COLUMN_NAME + " options have not been created "
		);
	}

	@Test
	public void testXmlMappingTableCommentAreCreated() throws Exception {
		createSchema( "org/hibernate/orm/test/schemaupdate/columnoptions/TestEntity.xml" );
		assertTrue(
				tableCreationStatementContainsOptions( output, COLUMN_NAME, COLUMN_OPTIONS ),
				"Column " + COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						DISCRIMINATOR_COLUMN_NAME,
						DISCRIMINATOR_COLUMN_OPTIONS
				),
				"DiscriminatorColumn " + DISCRIMINATOR_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions( output, JOIN_COLUMN_NAME, JOIN_COLUMN_OPTIONS ),
				"JoinColumn " + JOIN_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ELEMENT_COLLECTION_MAP_KEY_COLUMN_NAME,
						ELEMENT_COLLECTION_MAP_KEY_COLUMN_OPTIONS
				),
				"ElementCollection MapKeyColumn " + ELEMENT_COLLECTION_MAP_KEY_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ONE_TO_MANY_MAP_KEY_COLUMN_NAME,
						ONE_TO_MANY_MAP_KEY_COLUMN_OPTIONS
				),
				"OneToMany MapKeyColumn " + ONE_TO_MANY_MAP_KEY_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						MANY_TO_MANY_MAP_KEY_COLUMN_NAME,
						MANY_TO_MANY_MAP_KEY_COLUMN_OPTIONS
				),
				"ManyToMany MapKeyColumn " + MANY_TO_MANY_MAP_KEY_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ONE_TO_MANY_MAP_KEY_COLUMN_NAME,
						ONE_TO_MANY_MAP_KEY_COLUMN_OPTIONS
				),
				"OneToMany MapKeyColumn " + ONE_TO_MANY_MAP_KEY_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						PRIMARY_KEY_JOIN_COLUMN_NAME,
						PRIMARY_KEY_JOIN_COLUMN_OPTIONS
				),
				"PrimaryKeyJoinColumn " + PRIMARY_KEY_JOIN_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ELEMENT_COLLECTION_MAP_KEY_JOIN_COLUMN_NAME,
						ELEMENT_COLLECTION_MAP_KEY_JOIN_COLUMN_OPTIONS
				),
				"ElementCollection MapKeyJoinColumn " + ELEMENT_COLLECTION_MAP_KEY_JOIN_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ONE_TO_MANY_MAP_KEY_JOIN_COLUMN_NAME,
						ONE_TO_MANY_MAP_KEY_JOIN_COLUMN_OPTIONS
				),
				"OneToMany MapKeyJoinColumn " + ONE_TO_MANY_MAP_KEY_JOIN_COLUMN_NAME + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						MANY_TO_MANY_MAP_KEY_JOIN_COLUMN_NAME,
						MANY_TO_MANY_MAP_KEY_JOIN_COLUMN_OPTIONS
				),
				"ManyToMany MapKeyJoinColumn " + MANY_TO_MANY_MAP_KEY_JOIN_COLUMN_OPTIONS + " options have not been created "
		);

		assertTrue(
				tableCreationStatementContainsOptions(
						output,
						ORDER_COLUMN_NAME,
						ORDER_COLUMN_OPTIONS
				),
				"OrderColumn " + ORDER_COLUMN_NAME + " options have not been created "
		);
	}

	private static boolean tableCreationStatementContainsOptions(
			File output,
			String columnName,
			String options) throws Exception {
		String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		for ( int i = 0; i < fileContent.length; i++ ) {
			String statement = fileContent[i].toUpperCase( Locale.ROOT );
			if ( statement.contains( options.toUpperCase( Locale.ROOT ) ) ) {
				return true;
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
				.createOnly( EnumSet.of( TargetType.SCRIPT ), metadata );
	}

}
