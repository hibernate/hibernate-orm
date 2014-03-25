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

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Torben Riis
 */
public class MigrationForSchemaWithStrangeCharactersTest extends BaseUnitTestCase {
    private static final String STRANGE_SCHEMA_NAME = "\"MY SCHEMA (TEST)!\"";
    private static final String DB_URL = 
            "jdbc:h2:mem:db1;INIT=CREATE SCHEMA IF NOT EXISTS " 
                    + STRANGE_SCHEMA_NAME + ";DB_CLOSE_DELAY=-1;MVCC=TRUE";
    
	private ServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
	    Properties props = Environment.getProperties();
	    props.setProperty(Environment.URL, DB_URL);
        serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry(props);
	}

	@After
	public void tearDown() {
		ServiceRegistryBuilder.destroy( serviceRegistry );
		serviceRegistry = null;
	}

	@Test
	public void testSchemaUpdateForSchemaWithStrangeCharacters() {
		String resource = "org/hibernate/test/schemaupdate/2_Version.hbm.xml";

		Configuration cfg = new Configuration();
		cfg.setProperty(Environment.SHOW_SQL, "true");
        cfg.setProperty(Environment.URL, DB_URL);
        cfg.setProperty(Environment.DEFAULT_SCHEMA, STRANGE_SCHEMA_NAME);
        cfg.addResource(resource);
        
        // Do export - create tables
        SchemaExport schemaExport = new SchemaExport( cfg );
        schemaExport.execute( false, true, false, true);
        assertEquals( 0, schemaExport.getExceptions().size() );

        // Do update - Ensure that no tables needs an update, since no changes has happened
		SchemaUpdate v2schemaUpdate = new SchemaUpdate( serviceRegistry, cfg );
		v2schemaUpdate.execute( true, true );
		assertEquals("Schema update should not register any errors since nothing has changed",  
		    0, v2schemaUpdate.getExceptions().size() );
	}
}
