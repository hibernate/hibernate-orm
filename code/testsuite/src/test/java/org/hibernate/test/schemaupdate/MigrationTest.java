package org.hibernate.test.schemaupdate;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.cfg.Configuration;
import org.hibernate.junit.UnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

/**
 * @author Max Rydahl Andersen
 */
public class MigrationTest extends UnitTestCase {

	public MigrationTest(String str) {
		super( str );
	}

	public static Test suite() {
		return new TestSuite( MigrationTest.class );
	}

	public void testSimpleColumnAddition() {
		String resource1 = "org/hibernate/test/schemaupdate/1_Version.hbm.xml";
		String resource2 = "org/hibernate/test/schemaupdate/2_Version.hbm.xml";

		Configuration v1cfg = new Configuration();
		v1cfg.addResource( resource1 );
		new SchemaExport( v1cfg ).execute( false, true, true, false );

		SchemaUpdate v1schemaUpdate = new SchemaUpdate( v1cfg );
		v1schemaUpdate.execute( true, true );

		assertEquals( 0, v1schemaUpdate.getExceptions().size() );

		Configuration v2cfg = new Configuration();
		v2cfg.addResource( resource2 );

		SchemaUpdate v2schemaUpdate = new SchemaUpdate( v2cfg );
		v2schemaUpdate.execute( true, true );
		assertEquals( 0, v2schemaUpdate.getExceptions().size() );

	}

}

