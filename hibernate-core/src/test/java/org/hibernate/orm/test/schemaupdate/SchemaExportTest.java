/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import org.hamcrest.MatcherAssert;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@org.hibernate.testing.orm.junit.ServiceRegistry
@DomainModel(xmlMappings = "org/hibernate/orm/test/schemaupdate/mapping.hbm.xml")
public class SchemaExportTest {
	private boolean doesDialectSupportDropTableIfExist(ServiceRegistryScope registryScope) {
		var dialect = registryScope.getRegistry().requireService( JdbcEnvironment.class ).getDialect();
		return dialect.supportsIfExistsAfterTableName() || dialect.supportsIfExistsBeforeTableName();
	}

	@BeforeEach
	public void setUp(DomainModelScope modelScope) throws Exception {
		var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), model );
	}

	@AfterEach
	public void tearDown(DomainModelScope modelScope) {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
	}

	@Test
	public void testCreateAndDropOnlyType(DomainModelScope modelScope) {
		final var schemaExport = new SchemaExport();
		final var model = modelScope.getDomainModel();

		// create w/o dropping first; (OK because tables don't exist yet
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.CREATE, model );
		assertEquals( 0, schemaExport.getExceptions().size() );

		// create w/o dropping again; should cause an exception because the tables exist already
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.CREATE, model );
		assertEquals( 1, schemaExport.getExceptions().size() );

		// drop tables only
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP, model );
		assertEquals( 0, schemaExport.getExceptions().size() );
	}

	@Test
	public void testBothType(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		final var schemaExport = new SchemaExport();
		final var model = modelScope.getDomainModel();

		// drop before create (nothing to drop yeT)
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP, model );
		if ( doesDialectSupportDropTableIfExist( registryScope ) ) {
			assertEquals( 0, schemaExport.getExceptions().size() );
		}
		else {
			assertEquals( 1, schemaExport.getExceptions().size() );
		}

		// drop before create again (this time drops the tables before re-creating)
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH, model );
		int exceptionCount = schemaExport.getExceptions().size();
		if ( doesDialectSupportDropTableIfExist( registryScope ) ) {
			assertEquals( 0, exceptionCount );
		}

		// drop tables
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP, model );
		assertEquals( 0, schemaExport.getExceptions().size() );
	}

	@Test
	public void testCreateAndDrop(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		final var schemaExport = new SchemaExport();
		final var model = modelScope.getDomainModel();

		// should drop before creating, but tables don't exist yet
		schemaExport.create( EnumSet.of( TargetType.DATABASE ), model );
		if ( doesDialectSupportDropTableIfExist( registryScope ) ) {
			assertEquals( 0, schemaExport.getExceptions().size() );
		}
		else {
			assertEquals( 1, schemaExport.getExceptions().size() );
		}
		// call create again; it should drop tables before re-creating
		schemaExport.create( EnumSet.of( TargetType.DATABASE ), model );
		assertEquals( 0, schemaExport.getExceptions().size() );

		// drop the tables
		schemaExport.drop( EnumSet.of( TargetType.DATABASE ), model );
		assertEquals( 0, schemaExport.getExceptions().size() );
	}

	@Test
	@JiraKey(value = "HHH-10678")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportSchemaCreation.class)
	public void testHibernateMappingSchemaPropertyIsNotIgnored(
			ServiceRegistryScope registryScope,
			@TempDir File tempDir) throws Exception {
		var output = new File( tempDir, "update_script.sql" );

		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addResource( "org/hibernate/orm/test/schemaupdate/mapping2.hbm.xml" )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();

		final SchemaExport schemaExport = new SchemaExport();
		schemaExport.setOutputFile( output.getAbsolutePath() );
		schemaExport.execute( EnumSet.of( TargetType.SCRIPT ), SchemaExport.Action.CREATE, metadata );

		String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table schema1.version" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		MatcherAssert.assertThat( fileContent, fileContentMatcher.find(), is( true ) );
	}
}
