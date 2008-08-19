//$Id: ExtendsTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.extendshbm;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.junit.UnitTestCase;

/**
 * @author Gavin King
 */
public class ExtendsTest extends UnitTestCase {

	public ExtendsTest(String str) {
		super( str );
	}

	public static Test suite() {
		return new TestSuite( ExtendsTest.class );
	}

	private String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	public void testAllInOne() {
		Configuration cfg = new Configuration();

		cfg.addResource( getBaseForMappings() + "extendshbm/allinone.hbm.xml" );
		assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Customer" ) );
		assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Person" ) );
		assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Employee" ) );
	}

	public void testOutOfOrder() {
		Configuration cfg = new Configuration();

		try {
			cfg.addResource( getBaseForMappings() + "extendshbm/Customer.hbm.xml" );
			assertNull(
					"cannot be in the configuration yet!",
					cfg.getClassMapping( "org.hibernate.test.extendshbm.Customer" )
			);
			cfg.addResource( getBaseForMappings() + "extendshbm/Person.hbm.xml" );
			cfg.addResource( getBaseForMappings() + "extendshbm/Employee.hbm.xml" );

			cfg.buildSessionFactory();

			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Customer" ) );
			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Person" ) );
			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Employee" ) );

		}
		catch ( HibernateException e ) {
			fail( "should not fail with exception! " + e );
		}

	}

	public void testNwaitingForSuper() {
		Configuration cfg = new Configuration();

		try {
			cfg.addResource( getBaseForMappings() + "extendshbm/Customer.hbm.xml" );
			assertNull(
					"cannot be in the configuration yet!",
					cfg.getClassMapping( "org.hibernate.test.extendshbm.Customer" )
			);
			cfg.addResource( getBaseForMappings() + "extendshbm/Employee.hbm.xml" );
			assertNull(
					"cannot be in the configuration yet!",
					cfg.getClassMapping( "org.hibernate.test.extendshbm.Employee" )
			);
			cfg.addResource( getBaseForMappings() + "extendshbm/Person.hbm.xml" );

			cfg.buildMappings();

			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Person" ) );
			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Employee" ) );
			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Customer" ) );


		}
		catch ( HibernateException e ) {
			e.printStackTrace();
			fail( "should not fail with exception! " + e );

		}

	}

	public void testMissingSuper() {
		Configuration cfg = new Configuration();

		try {
			cfg.addResource( getBaseForMappings() + "extendshbm/Customer.hbm.xml" );
			assertNull(
					"cannot be in the configuration yet!",
					cfg.getClassMapping( "org.hibernate.test.extendshbm.Customer" )
			);
			cfg.addResource( getBaseForMappings() + "extendshbm/Employee.hbm.xml" );

			cfg.buildSessionFactory();

			fail( "Should not be able to build sessionfactory without a Person" );
		}
		catch ( HibernateException e ) {

		}

	}

	public void testAllSeparateInOne() {
		Configuration cfg = new Configuration();

		try {
			cfg.addResource( getBaseForMappings() + "extendshbm/allseparateinone.hbm.xml" );

			cfg.buildSessionFactory();

			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Customer" ) );
			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Person" ) );
			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Employee" ) );

		}
		catch ( HibernateException e ) {
			fail( "should not fail with exception! " + e );
		}

	}

	public void testJoinedSubclassAndEntityNamesOnly() {
		Configuration cfg = new Configuration();

		try {
			cfg.addResource( getBaseForMappings() + "extendshbm/entitynames.hbm.xml" );

			cfg.buildMappings();

			assertNotNull( cfg.getClassMapping( "EntityHasName" ) );
			assertNotNull( cfg.getClassMapping( "EntityCompany" ) );

		}
		catch ( HibernateException e ) {
			e.printStackTrace();
			fail( "should not fail with exception! " + e );

		}
	}

	public void testEntityNamesWithPackage() {
		Configuration cfg = new Configuration();
		try {
			cfg.addResource( getBaseForMappings() + "extendshbm/packageentitynames.hbm.xml" );

			cfg.buildMappings();

			assertNotNull( cfg.getClassMapping( "EntityHasName" ) );
			assertNotNull( cfg.getClassMapping( "EntityCompany" ) );

		}
		catch ( HibernateException e ) {
			e.printStackTrace();
			fail( "should not fail with exception! " + e );

		}
	}


	public void testUnionSubclass() {
		Configuration cfg = new Configuration();

		try {
			cfg.addResource( getBaseForMappings() + "extendshbm/unionsubclass.hbm.xml" );

			cfg.buildMappings();

			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Person" ) );
			assertNotNull( cfg.getClassMapping( "org.hibernate.test.extendshbm.Customer" ) );

		}
		catch ( HibernateException e ) {
			e.printStackTrace();
			fail( "should not fail with exception! " + e );

		}
	}

}

