/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Gail Badner
 */
public class SchemaExportTest extends BaseUnitTestCase {
	private boolean doesDialectSupportDropTableIfExist() {
		final Dialect dialect = metadata.getDatabase().getDialect();
		return dialect.supportsIfExistsAfterTableName() || dialect.supportsIfExistsBeforeTableName();
	}

	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addResource( "org/hibernate/orm/test/schemaupdate/mapping.hbm.xml" )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();

		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), metadata );
	}

	@After
	public void tearDown() {
		ServiceRegistryBuilder.destroy( serviceRegistry );
		serviceRegistry = null;
	}

	@Test
	public void testCreateAndDropOnlyType() {
		final SchemaExport schemaExport = new SchemaExport();

		// create w/o dropping first; (OK because tables don't exist yet
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.CREATE, metadata );
		assertEquals( 0, schemaExport.getExceptions().size() );

		// create w/o dropping again; should cause an exception because the tables exist already
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.CREATE, metadata );
		assertEquals( 1, schemaExport.getExceptions().size() );

		// drop tables only
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP, metadata );
		assertEquals( 0, schemaExport.getExceptions().size() );
	}

	@Test
	public void testBothType() {
		final SchemaExport schemaExport = new SchemaExport();

		// drop before create (nothing to drop yeT)
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP, metadata );
		if ( doesDialectSupportDropTableIfExist() ) {
			assertEquals( 0, schemaExport.getExceptions().size() );
		}
		else {
			assertEquals( 1, schemaExport.getExceptions().size() );
		}

		// drop before create again (this time drops the tables before re-creating)
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH, metadata );
		int exceptionCount = schemaExport.getExceptions().size();
		if ( doesDialectSupportDropTableIfExist() ) {
			assertEquals( 0,  exceptionCount);
		}

		// drop tables
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP, metadata );
		assertEquals( 0, schemaExport.getExceptions().size() );
	}

	@Test
	public void testCreateAndDrop() {
		final SchemaExport schemaExport = new SchemaExport();

		// should drop before creating, but tables don't exist yet
		schemaExport.create( EnumSet.of( TargetType.DATABASE ), metadata );
		if ( doesDialectSupportDropTableIfExist() ) {
			assertEquals( 0, schemaExport.getExceptions().size() );
		}
		else {
			assertEquals( 1, schemaExport.getExceptions().size() );
		}
		// call create again; it should drop tables before re-creating
		schemaExport.create( EnumSet.of( TargetType.DATABASE ), metadata );
		assertEquals( 0, schemaExport.getExceptions().size() );
		// drop the tables
		schemaExport.drop( EnumSet.of( TargetType.DATABASE ), metadata );
		assertEquals( 0, schemaExport.getExceptions().size() );
	}

	@Test
	@JiraKey(value = "HHH-10678")
	@RequiresDialectFeature( value = DialectChecks.SupportSchemaCreation.class)
	public void testHibernateMappingSchemaPropertyIsNotIgnored() throws Exception {
		File output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();

		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
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
		assertThat( fileContent, fileContentMatcher.find(), is( true ) );
	}
}
