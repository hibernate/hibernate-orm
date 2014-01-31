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

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Max Rydahl Andersen
 * @author Brett Meyer
 */
public class MigrationTest extends BaseUnitTestCase {
	@Test
	public void dummy() {
	}

//	private ServiceRegistry serviceRegistry;
//
//	@Before
//	public void setUp() {
//		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
//	}
//
//	@After
//	public void tearDown() {
//		ServiceRegistryBuilder.destroy( serviceRegistry );
//		serviceRegistry = null;
//	}
//
//	protected JdbcServices getJdbcServices() {
//		return serviceRegistry.getService( JdbcServices.class );
//	}
//
//	@Test
//	public void testSimpleColumnAddition() {
//		String resource1 = "org/hibernate/test/schemaupdate/1_Version.hbm.xml";
//		String resource2 = "org/hibernate/test/schemaupdate/2_Version.hbm.xml";
//
//		Configuration v1cfg = new Configuration();
//		v1cfg.addResource( resource1 );
//		new SchemaExport( v1cfg ).execute( false, true, true, false );
//
//		SchemaUpdate v1schemaUpdate = new SchemaUpdate( serviceRegistry, v1cfg );
//		v1schemaUpdate.execute( true, true );
//
//		assertEquals( 0, v1schemaUpdate.getExceptions().size() );
//
//		Configuration v2cfg = new Configuration();
//		v2cfg.addResource( resource2 );
//
//		SchemaUpdate v2schemaUpdate = new SchemaUpdate( serviceRegistry, v2cfg );
//		v2schemaUpdate.execute( true, true );
//		assertEquals( 0, v2schemaUpdate.getExceptions().size() );
//
//		new SchemaExport( serviceRegistry, v2cfg ).drop( false, true );
//
//	}
//
//	/**
//	 * 3_Version.hbm.xml contains a named unique constraint and an un-named
//	 * unique constraint (will receive a randomly-generated name).  Create
//	 * the original schema with 2_Version.hbm.xml.  Then, run SchemaUpdate
//	 * TWICE using 3_Version.hbm.xml.  Neither RECREATE_QUIETLY nor SKIP should
//	 * generate any exceptions.
//	 */
//	@Test
//	@TestForIssue( jiraKey = "HHH-8162" )
//	public void testConstraintUpdate() {
//		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY);
//		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.RECREATE_QUIETLY);
//		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.SKIP);
//	}
//
//	private void doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy strategy) {
//		// original
//		String resource1 = "org/hibernate/test/schemaupdate/2_Version.hbm.xml";
//		// adds unique constraint
//		String resource2 = "org/hibernate/test/schemaupdate/3_Version.hbm.xml";
//
//		Configuration v1cfg = new Configuration();
//		v1cfg.addResource( resource1 );
//		new SchemaExport( v1cfg ).execute( false, true, true, false );
//
//		// adds unique constraint
//		Configuration v2cfg = new Configuration();
//		v2cfg.getProperties().put( AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, strategy );
//		v2cfg.addResource( resource2 );
//		SchemaUpdate v2schemaUpdate = new SchemaUpdate( serviceRegistry, v2cfg );
//		v2schemaUpdate.execute( true, true );
//		assertEquals( 0, v2schemaUpdate.getExceptions().size() );
//
//		Configuration v3cfg = new Configuration();
//		v3cfg.getProperties().put( AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, strategy );
//		v3cfg.addResource( resource2 );
//		SchemaUpdate v3schemaUpdate = new SchemaUpdate( serviceRegistry, v3cfg );
//		v3schemaUpdate.execute( true, true );
//		assertEquals( 0, v3schemaUpdate.getExceptions().size() );
//
//		new SchemaExport( serviceRegistry, v3cfg ).drop( false, true );
//	}

}

