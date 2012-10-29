/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.schemaupdate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public abstract class SchemaExportTest extends BaseUnitTestCase {
    private final String MAPPING = "org/hibernate/test/schemaupdate/mapping.hbm.xml";

    protected abstract SchemaExport createSchemaExport(Configuration cfg);

    private boolean doesDialectSupportDropTableIfExist() {
        return Dialect.getDialect().supportsIfExistsAfterTableName() || Dialect.getDialect()
                .supportsIfExistsBeforeTableName();
    }
	protected ServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
		Configuration cfg = new Configuration();
		cfg.addResource( MAPPING );
		SchemaExport schemaExport = createSchemaExport( cfg );
		schemaExport.drop( true, true );
	}

	@After
	public void tearDown() {
		ServiceRegistryBuilder.destroy( serviceRegistry );
		serviceRegistry = null;
	}

    @Test
    public void testCreateAndDropOnlyType() {
        Configuration cfg = new Configuration();
        cfg.addResource( MAPPING );
        SchemaExport schemaExport = createSchemaExport( cfg );
        // create w/o dropping first; (OK because tables don't exist yet
        schemaExport.execute( false, true, false, true );
//        if ( doesDialectSupportDropTableIfExist() ) {
            assertEquals( 0, schemaExport.getExceptions().size() );
//        }
//        else {
//            assertEquals( 2, schemaExport.getExceptions().size() );
//        }
        // create w/o dropping again; should be an exception for each table
        // (2 total) because the tables exist already
//        assertEquals( 0, schemaExport.getExceptions().size() );
        schemaExport.execute( false, true, false, true );
        assertEquals( 2, schemaExport.getExceptions().size() );
        // drop tables only
        schemaExport.execute( false, true, true, false );
        assertEquals( 0, schemaExport.getExceptions().size() );
    }

    @Test
    public void testBothType() {
        Configuration cfg = new Configuration();
        cfg.addResource( MAPPING );
        SchemaExport schemaExport = createSchemaExport( cfg );
        // drop before create (nothing to drop yeT)
        schemaExport.execute( false, true, false, false );
        if ( doesDialectSupportDropTableIfExist() ) {
            assertEquals( 0, schemaExport.getExceptions().size() );
        }
        else {
            assertEquals( 2, schemaExport.getExceptions().size() );
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
        Configuration cfg = new Configuration();
        cfg.addResource( MAPPING );
        SchemaExport schemaExport = createSchemaExport( cfg );
        java.io.File outFile = new java.io.File("schema.ddl");
        schemaExport.setOutputFile(outFile.getPath());
        // do not script to console or export to database
        schemaExport.execute( false, false, false, true );
        if ( doesDialectSupportDropTableIfExist()
        		&& schemaExport.getExceptions().size() > 0 ) {
            assertEquals( 2, schemaExport.getExceptions().size() );
        }
        assertTrue( outFile.exists() );
        //check file is not empty
        assertTrue( outFile.length() > 0 );
        outFile.delete();
    }

    @Test
    public void testCreateAndDrop() {
        Configuration cfg = new Configuration();
        cfg.addResource( MAPPING );
        SchemaExport schemaExport = createSchemaExport( cfg );
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
