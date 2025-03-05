/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.tablecomment;

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
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-18055")
@BaseUnitTest
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsCommentOn.class)
public class JpaTableCommentTest {
	static final String TABLE_NAME = "PRIMARY_TABLE";
	static final String TABLE_COMMENT = "This is the primary table";

	static final String SECONDARY_TABLE_NAME = "SECOND_TABLE";
	static final String SECONDARY_TABLE_COMMENT = "This is the secondary table";

	static final String JOIN_TABLE_NAME = "JOIN_TABLE";
	static final String JOIN_TABLE_COMMENT = "This is the join table";

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
				tableCreationStatementContainsComment( output, TABLE_NAME, TABLE_COMMENT ),
				"Table " + TABLE_NAME + " comment has not been created "
		);

		assertTrue(
				tableCreationStatementContainsComment( output, SECONDARY_TABLE_NAME, SECONDARY_TABLE_COMMENT ),
				"SecondaryTable " + SECONDARY_TABLE_NAME + " comment has not been created "
		);

		assertTrue(
				tableCreationStatementContainsComment( output, JOIN_TABLE_NAME, JOIN_TABLE_COMMENT ),
				"Join Table " + JOIN_TABLE_NAME + " comment has not been created "
		);
	}

	@Test
	public void testXmlMAppingTableCommentAreCreated() throws Exception {
		createSchema( "org/hibernate/orm/test/schemaupdate/tablecomment/TestEntity.xml" );
		assertTrue(
				tableCreationStatementContainsComment( output, TABLE_NAME, TABLE_COMMENT ),
				"Table " + TABLE_NAME + " comment has not been created "
		);

		assertTrue(
				tableCreationStatementContainsComment( output, JOIN_TABLE_NAME, JOIN_TABLE_COMMENT ),
				"Join Table " + JOIN_TABLE_NAME + " comment has not been created "
		);

		assertTrue(
				tableCreationStatementContainsComment( output, SECONDARY_TABLE_NAME, SECONDARY_TABLE_COMMENT ),
				"SecondaryTable " + SECONDARY_TABLE_NAME + " comment has not been created "
		);
	}

	private boolean tableCreationStatementContainsComment(
			File output,
			String tableName,
			String comment) throws Exception {
		final String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		final Dialect dialect = metadata.getDatabase().getDialect();
		final String createTable = dialect.getCreateTableString().toUpperCase( Locale.ROOT ) + " ";
		for ( final String s : fileContent ) {
			final String statement = s.toUpperCase( Locale.ROOT );
			if ( !dialect.getTableComment( "" ).isEmpty() ) {
				if ( statement.contains( createTable + tableName.toUpperCase( Locale.ROOT ) ) ) {
					if ( statement.contains( comment.toUpperCase( Locale.ROOT ) ) ) {
						return true;
					}
				}
			}
			else if ( statement.contains( "COMMENT ON TABLE " + tableName.toUpperCase( Locale.ROOT ) ) ) {
				if ( statement.contains( comment.toUpperCase( Locale.ROOT ) ) ) {
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
