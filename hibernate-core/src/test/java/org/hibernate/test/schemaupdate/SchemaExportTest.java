/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.Target;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public abstract class SchemaExportTest extends BaseUnitTestCase {

    protected abstract SchemaExport createSchemaExport(MetadataImplementor metadata, ServiceRegistry serviceRegistry);

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

		SchemaExport schemaExport = createSchemaExport( metadata, serviceRegistry );
		schemaExport.drop( true, true );
	}

	@After
	public void tearDown() {
		ServiceRegistryBuilder.destroy( serviceRegistry );
		serviceRegistry = null;
	}

    @Test
    public void testCreateAndDropOnlyType() {
        final SchemaExport schemaExport = createSchemaExport( metadata, serviceRegistry );

        // create w/o dropping first; (OK because tables don't exist yet
        schemaExport.execute( Target.EXPORT, SchemaExport.Type.CREATE );
        assertEquals( 0, schemaExport.getExceptions().size() );

        // create w/o dropping again; should cause an exception because the tables exist already
        schemaExport.execute( Target.EXPORT, SchemaExport.Type.CREATE );
        assertEquals( 1, schemaExport.getExceptions().size() );

        // drop tables only
        schemaExport.execute( Target.EXPORT, SchemaExport.Type.DROP );
        assertEquals( 0, schemaExport.getExceptions().size() );
    }

    @Test
    public void testBothType() {
        final SchemaExport schemaExport = createSchemaExport( metadata, serviceRegistry );

        // drop before create (nothing to drop yeT)
        schemaExport.execute( false, true, false, false );
        if ( doesDialectSupportDropTableIfExist() ) {
            assertEquals( 0, schemaExport.getExceptions().size() );
        }
        else {
            assertEquals( 1, schemaExport.getExceptions().size() );
        }

        // drop before crete again (this time drops the tables before re-creating)
        schemaExport.execute( false, true, false, false );
        assertEquals( 0, schemaExport.getExceptions().size() );

        // drop tables
        schemaExport.execute( false, true, true, false );
        assertEquals( 0, schemaExport.getExceptions().size() );
    }

    @Test
    public void testGenerateDdlToFile() {
        final SchemaExport schemaExport = createSchemaExport( metadata, serviceRegistry );

        java.io.File outFile = new java.io.File("schema.ddl");
        schemaExport.setOutputFile( outFile.getPath() );

        // do not script to console or export to database
        schemaExport.execute( false, false, false, true );
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
        final SchemaExport schemaExport = createSchemaExport( metadata, serviceRegistry );

        // should drop before creating, but tables don't exist yet
        schemaExport.create( true, true );
		if ( doesDialectSupportDropTableIfExist() ) {
			assertEquals( 0, schemaExport.getExceptions().size() );
		}
		else {
			assertEquals( 2, schemaExport.getExceptions().size() );
		}
        // call create again; it should drop tables before re-creating
        schemaExport.create( true, true );
        assertEquals( 0, schemaExport.getExceptions().size() );
        // drop the tables
        schemaExport.drop( true, true );
        assertEquals( 0, schemaExport.getExceptions().size() );
    }
}
