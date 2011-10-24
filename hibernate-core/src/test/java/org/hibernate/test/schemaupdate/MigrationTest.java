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
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import static org.junit.Assert.assertEquals;

/**
 * @author Max Rydahl Andersen
 */
public class MigrationTest extends BaseUnitTestCase {
	private ServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@After
	public void tearDown() {
		ServiceRegistryBuilder.destroy( serviceRegistry );
		serviceRegistry = null;
	}

	protected JdbcServices getJdbcServices() {
		return serviceRegistry.getService( JdbcServices.class );
	}

	@Test
	public void testSimpleColumnAddition() {
		String resource1 = "org/hibernate/test/schemaupdate/1_Version.hbm.xml";
		String resource2 = "org/hibernate/test/schemaupdate/2_Version.hbm.xml";

		Configuration v1cfg = new Configuration();
		v1cfg.addResource( resource1 );
		new SchemaExport( v1cfg ).execute( false, true, true, false );

		SchemaUpdate v1schemaUpdate = new SchemaUpdate( serviceRegistry, v1cfg );
		v1schemaUpdate.execute( true, true );

		assertEquals( 0, v1schemaUpdate.getExceptions().size() );

		Configuration v2cfg = new Configuration();
		v2cfg.addResource( resource2 );

		SchemaUpdate v2schemaUpdate = new SchemaUpdate( serviceRegistry, v2cfg );
		v2schemaUpdate.execute( true, true );
		assertEquals( 0, v2schemaUpdate.getExceptions().size() );
		
		new SchemaExport( serviceRegistry, v2cfg ).drop( false, true );

	}

}

