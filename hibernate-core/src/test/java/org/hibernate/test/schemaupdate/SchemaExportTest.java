/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

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
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class SchemaExportTest extends BaseUnitTestCase {
    private boolean doesDialectSupportDropTableIfExist() {
        return Dialect.getDialect().supportsIfExistsAfterTableName() || Dialect.getDialect()
                .supportsIfExistsBeforeTableName();
    }

	protected ServiceRegistry serviceRegistry;
    protected MetadataImplementor metadata;

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
        metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
                .addResource( "org/hibernate/test/schemaupdate/mapping.hbm.xml" )
                .buildMetadata();
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

        // drop beforeQuery create (nothing to drop yeT)
        schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP, metadata );
        if ( doesDialectSupportDropTableIfExist() ) {
            assertEquals( 0, schemaExport.getExceptions().size() );
        }
        else {
            assertEquals( 1, schemaExport.getExceptions().size() );
        }

        // drop beforeQuery create again (this time drops the tables beforeQuery re-creating)
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
    public void testGenerateDdlToFile() {
		final SchemaExport schemaExport = new SchemaExport();

        java.io.File outFile = new java.io.File("schema.ddl");
        schemaExport.setOutputFile( outFile.getPath() );

        // do not script to console or export to database
        schemaExport.execute( EnumSet.of( TargetType.SCRIPT ), SchemaExport.Action.DROP, metadata );
        if ( doesDialectSupportDropTableIfExist() && schemaExport.getExceptions().size() > 0 ) {
            assertEquals( 2, schemaExport.getExceptions().size() );
        }
        assertTrue( outFile.exists() );

        //check file is not empty
        assertTrue( outFile.length() > 0 );
        outFile.delete();
    }

    @Test
    public void testCreateAndDrop() {
		final SchemaExport schemaExport = new SchemaExport();

        // should drop beforeQuery creating, but tables don't exist yet
        schemaExport.create( EnumSet.of( TargetType.DATABASE ), metadata );
		if ( doesDialectSupportDropTableIfExist() ) {
			assertEquals( 0, schemaExport.getExceptions().size() );
		}
		else {
			assertEquals( 1, schemaExport.getExceptions().size() );
		}
        // call create again; it should drop tables beforeQuery re-creating
		schemaExport.create( EnumSet.of( TargetType.DATABASE ), metadata );
        assertEquals( 0, schemaExport.getExceptions().size() );
        // drop the tables
		schemaExport.drop( EnumSet.of( TargetType.DATABASE ), metadata );
        assertEquals( 0, schemaExport.getExceptions().size() );
    }

	@Test
	@TestForIssue(jiraKey = "HHH-10678")
	@RequiresDialectFeature( value = DialectChecks.SupportSchemaCreation.class)
	public void testHibernateMappingSchemaPropertyIsNotIgnored() throws Exception {
		File output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();

		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addResource( "org/hibernate/test/schemaupdate/mapping2.hbm.xml" )
				.buildMetadata();
		metadata.validate();

		final SchemaExport schemaExport = new SchemaExport();
		schemaExport.setOutputFile( output.getAbsolutePath() );
		schemaExport.execute( EnumSet.of( TargetType.SCRIPT ), SchemaExport.Action.CREATE, metadata );

		String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( fileContent, fileContent.toLowerCase().contains( "create table schema1.version" ), is( true ) );
	}
}
