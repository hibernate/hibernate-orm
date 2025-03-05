/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.secondarytable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Locale;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTypeName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
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

@JiraKey("HHH-18081")
@BaseUnitTest
public class SecondaryTableTest {

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
	public void testSecondaryTablesAreCreated() throws Exception {
		createSchema( "org/hibernate/orm/test/schemaupdate/secondarytable/TestEntity.xml" );

		assertTrue(
				isTableCreated( output, getExpectedTableName( "SECONDARY_TABLE_1", "SCHEMA1", "CATALOG1" ) ),
				"Table SECONDARY_TABLE_1 has not been created "
		);
		assertTrue(
				isTableCreated( output, getExpectedTableName( "SECONDARY_TABLE_2", null, null ) ),
				"Table SECONDARY_TABLE_2 has not been created "
		);
	}

	private String getExpectedTableName(String tableName, String schema, String catalog) {
		SqlStringGenerationContext context = SqlStringGenerationContextImpl.forTests(
				metadata.getDatabase().getJdbcEnvironment(), catalog, schema );
		return context.format(
				new QualifiedTypeName( Identifier.toIdentifier( catalog ), Identifier.toIdentifier( schema ),
						Identifier.toIdentifier( tableName ) ) );
	}

	private boolean isTableCreated(
			File output,
			String tableName) throws Exception {
		final String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		final String createTable = metadata.getDatabase().getDialect().getCreateTableString().toUpperCase( Locale.ROOT ) + " ";
		for ( final String s : fileContent ) {
			final String statement = s.toUpperCase( Locale.ROOT );
			if ( statement.contains( createTable + tableName.toUpperCase( Locale.ROOT ) ) ) {
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

}
