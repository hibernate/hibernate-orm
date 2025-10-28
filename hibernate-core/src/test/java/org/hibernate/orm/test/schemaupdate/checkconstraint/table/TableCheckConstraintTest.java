/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.checkconstraint.table;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Locale;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.testing.orm.junit.DialectContext;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
@JiraKey("HHH-18054")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTableCheck.class)
public class TableCheckConstraintTest {
	static final String CONSTRAINTS = "NAME_COLUMN is not null";
	static final String SECONDARY_TABLE_CONSTRAINTS = "SECOND_NAME is not null";
	static final String ONE_TO_ONE_JOIN_TABLE_CONSTRAINTS = "ID is not null";
	static final String ONE_TO_MANY_JOIN_TABLE_CONSTRAINTS = "ID = 2";
	static final String MANY_TO_ONE_JOIN_TABLE_CONSTRAINTS = "ID = 3";
	static final String MANY_TO_MANY_JOIN_TABLE_CONSTRAINTS = "ID = 4";
	static final String ANY_JOIN_TABLE_CONSTRAINTS = "ID = 5";

	static final String SECONDARY_TABLE_NAME = "SECOND_TABLE_NAME";
	static final String ONE_TO_ONE_JOIN_TABLE_NAME = "ONE_TO_ONE_JOIN_TABLE_NAME";
	static final String ONE_TO_MANY_JOIN_TABLE_NAME = "ONE_TO_MAIN_JOIN_TABLE_NAME";
	static final String MANY_TO_ONE_JOIN_TABLE_NAME = "MANY_TO_ONE_JOIN_TABLE_NAME";
	static final String MANY_TO_MANY_JOIN_TABLE_NAME = "MANY_TO_MANY_JOIN_TABLE_NAME";
	static final String ANY_JOIN_TABLE_NAME = "ANY_JOIN_TABLE_NAME";

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
	public void testTableConstraintsAreApplied() throws Exception {
		createSchema( TestEntity.class );
		String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.replace( System.lineSeparator(), "" );
		assertThat( fileContent.toUpperCase( Locale.ROOT ) ).contains( CONSTRAINTS.toUpperCase( Locale.ROOT ) );
	}

	@Test
	public void testXmlMappingTableConstraintsAreApplied() throws Exception {
		createSchema( "org/hibernate/orm/test/schemaupdate/checkconstraint/table/mapping.xml" );
		String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.replace( System.lineSeparator(), "" );
		assertThat( fileContent.toUpperCase( Locale.ROOT ) ).contains( CONSTRAINTS.toUpperCase( Locale.ROOT ) );
	}

	@Test
	public void testSecondaryTableConstraintsAreApplied() throws Exception {
		createSchema( EntityWithSecondaryTables.class, AnotherTestEntity.class );
		String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				SECONDARY_TABLE_NAME,
				SECONDARY_TABLE_CONSTRAINTS
		), "Check Constraints on secondary table have not been created" );
	}

	@Test
	public void testXmlMappingSecondaryTableConstraintsAreApplied() throws Exception {
		createSchema( "org/hibernate/orm/test/schemaupdate/checkconstraint/table/mapping.xml" );
		String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				SECONDARY_TABLE_NAME,
				SECONDARY_TABLE_CONSTRAINTS
		), "Check Constraints on secondary table have not been created" );
	}

	@Test
	public void testJoinTableConstraintsAreApplied() throws Exception {
		createSchema( EntityWithSecondaryTables.class, AnotherTestEntity.class );
		String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				MANY_TO_ONE_JOIN_TABLE_NAME,
				MANY_TO_ONE_JOIN_TABLE_CONSTRAINTS
		), "Check Constraints on ManyToOne join table have not been created" );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				MANY_TO_MANY_JOIN_TABLE_NAME,
				MANY_TO_MANY_JOIN_TABLE_CONSTRAINTS
		), "Check Constraints on ManyToMany join table have not been created" );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				ONE_TO_ONE_JOIN_TABLE_NAME,
				ONE_TO_ONE_JOIN_TABLE_CONSTRAINTS
		), "Check Constraints on OneToOne join table have not been created" );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				ONE_TO_MANY_JOIN_TABLE_NAME,
				ONE_TO_MANY_JOIN_TABLE_CONSTRAINTS
		), "Check Constraints on OneToOne join table have not been created" );
	}

	@Test
	public void testXmlMappingJoinTableConstraintsAreApplied() throws Exception {
		createSchema( "org/hibernate/orm/test/schemaupdate/checkconstraint/table/mapping.xml" );
		String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				MANY_TO_ONE_JOIN_TABLE_NAME,
				MANY_TO_ONE_JOIN_TABLE_CONSTRAINTS
		), "Check Constraints on ManyToOne join table have not been created" );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				MANY_TO_MANY_JOIN_TABLE_NAME,
				MANY_TO_MANY_JOIN_TABLE_CONSTRAINTS
		), "Check Constraints on ManyToMany join table have not been created" );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				ONE_TO_ONE_JOIN_TABLE_NAME,
				ONE_TO_ONE_JOIN_TABLE_CONSTRAINTS
		), "Check Constraints on OneToOne join table have not been created" );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				ONE_TO_MANY_JOIN_TABLE_NAME,
				ONE_TO_MANY_JOIN_TABLE_CONSTRAINTS
		), "Check Constraints on OneToOne join table have not been created" );
	}

	@Test
	public void testAnyJoinTableConstraintsAreApplied() throws Exception {
		createSchema( EntityWithSecondaryTables.class, AnotherTestEntity.class );
		String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		assertTrue( tableCreationStatementContainsConstraints(
				fileContent,
				ANY_JOIN_TABLE_NAME,
				ANY_JOIN_TABLE_CONSTRAINTS
		), "Check Constraints on Any join table have not been created" );
	}

	private static boolean tableCreationStatementContainsConstraints(
			String[] fileContent,
			String tableName,
			String secondaryTableConstraints) {
		final String createTableString = DialectContext.getDialect().getCreateTableString().toUpperCase( Locale.ROOT );
		for ( String string : fileContent ) {
			String statement = string.toUpperCase( Locale.ROOT );
			if ( statement.contains( createTableString + " " + tableName.toUpperCase( Locale.ROOT ) ) ) {
				if ( statement.contains( secondaryTableConstraints.toUpperCase( Locale.ROOT ) ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private void createSchema(Class<?>... annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class<?> c : annotatedClasses ) {
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
