package org.hibernate.test.tool;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

import junit.framework.Test;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

/**
 * @author Anthony
 * 
 * Basic smoke test for schemaupdate/validator.
 * Dependent on schemas, and probably also HQLDB - Not possible to have in the global test suite at the moment.
 * 
 * WARNING, if you want this test to work, you need to define a default schema = SB
 * in hibernate global configuration.
 * This schema should not be the same at the default db user schema and should come after the users schema alphabetically.
 * 
 */
public class TestSchemaTools extends DatabaseSpecificFunctionalTestCase {

	public TestSchemaTools(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "tool/Team.hbm.xml" };
	}

	public boolean createSchema() {
		return false;
	}

	public void afterSessionFactoryBuilt(SessionFactoryImplementor sfi) {
		super.afterSessionFactoryBuilt( sfi );
		Session session = null;
		try {
			session = sfi.openSession();
			Statement stat = session.connection().createStatement();
			stat.execute("CREATE SCHEMA sb AUTHORIZATION DBA ");
			stat.execute(" CREATE SCHEMA sa AUTHORIZATION DBA ");
			stat.execute(" CREATE TABLE \"SA\".\"Team\" (test INTEGER) ");
			stat.close();
		}
		catch ( SQLException e ) {
			throw new RuntimeException( "could not prepare additional schemas" );
		}
		finally {
			if ( session != null ) {
				try {
					session.close();
				}
				catch( Throwable ignore ) {
				}
			}
		}
	}

	protected void prepareTest() throws Exception {
		super.prepareTest();
	}


	public boolean appliesTo(Dialect dialect) {
		return dialect instanceof HSQLDialect;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( TestSchemaTools.class );
	}
	
	public void testSchemaTools() throws Exception{
		// database schema have been created thanks to the setUp method
		// we have 2 schemas SA et SB, SB must be set as the default schema
		// used by hibernate hibernate.default_schema SB
		SchemaExport se = new SchemaExport(getCfg());
		se.create(true,true);
		
		// here we modify the generated table in order to test SchemaUpdate
		Session session = openSession();
		Connection conn = session.connection();
		Statement stat = conn.createStatement();
		stat.execute("ALTER TABLE \"SB\".\"Team\" DROP COLUMN name ");
		
		// update schema
		SchemaUpdate su = new SchemaUpdate(getCfg());
		su.execute(true,true);
		
		// we can run schema validation. Note that in the setUp method a *wrong* table
		// has been created with different column names
		// if schema validator chooses the bad db schema, then the testcase will fail (exception)
		SchemaValidator sv = new SchemaValidator(getCfg());
		sv.validate();
		
		// it's time to clean our database
		se.drop(true,true);
		
		// then the schemas and false table.

		stat.execute("DROP TABLE \"SA\".\"Team\" ");
		stat.execute(" DROP SCHEMA sa ");
		stat.execute("DROP SCHEMA sb ");
		stat.close();
		session.close();
	}
	
	public void testSchemaToolsNonQuote() throws Exception{
		// database schema have been created thanks to the setUp method
		// we have 2 schemas SA et SB, SB must be set as the default schema
		// used by hibernate hibernate.default_schema SB
		SchemaExport se = new SchemaExport(getCfg());
		se.create(true,true);
		
		// here we modify the generated table in order to test SchemaUpdate
		Session session = openSession();
		Connection conn = session.connection();
		Statement stat = conn.createStatement();
		stat.execute("ALTER TABLE \"SB\".\"TEAM\" DROP COLUMN xname ");
		
		// update schema
		SchemaUpdate su = new SchemaUpdate(getCfg());
		su.execute(true,true);
		
		// we can run schema validation. Note that in the setUp method a *wrong* table
		// has been created with different column names
		// if schema validator chooses the bad db schema, then the testcase will fail (exception)
		SchemaValidator sv = new SchemaValidator(getCfg());
		sv.validate();
		
		// it's time to clean our database
		se.drop(true,true);
		
		// then the schemas and false table.

		stat.execute("DROP TABLE \"SA\".\"Team\" ");
		stat.execute(" DROP SCHEMA sa ");
		stat.execute("DROP SCHEMA sb ");
		stat.close();
		session.close();
	}
	public void testFailingQuoteValidation() throws Exception{
		// database schema have been created thanks to the setUp method
		// we have 2 schemas SA et SB, SB must be set as the default schema
		// used by hibernate hibernate.default_schema SB
		SchemaExport se = new SchemaExport(getCfg());
		se.create(true,true);
		
		// here we modify the generated table in order to test SchemaUpdate
		Session session = openSession();
		Connection conn = session.connection();
		Statement stat = conn.createStatement();
		stat.execute("ALTER TABLE \"SB\".\"Team\" DROP COLUMN name ");
		
		// update schema
		//SchemaUpdate su = new SchemaUpdate(getCfg());
		//su.execute(true,true);
		
		try {
			SchemaValidator sv = new SchemaValidator(getCfg());
			sv.validate();
			fail("should fail since we mutated the current schema.");
		} catch(HibernateException he) {
			
		}
		
		// it's time to clean our database
		se.drop(true,true);
		
		// then the schemas and false table.

		stat.execute("DROP TABLE \"SA\".\"Team\" ");
		stat.execute(" DROP SCHEMA sa ");
		stat.execute("DROP SCHEMA sb ");
		stat.close();
		session.close();
	}

	public void testFailingNonQuoteValidation() throws Exception{
		// database schema have been created thanks to the setUp method
		// we have 2 schemas SA et SB, SB must be set as the default schema
		// used by hibernate hibernate.default_schema SB
		SchemaExport se = new SchemaExport(getCfg());
		se.create(true,true);
		
		// here we modify the generated table in order to test SchemaUpdate
		Session session = openSession();
		Connection conn = session.connection();
		Statement stat = conn.createStatement();
		stat.execute("ALTER TABLE \"SB\".\"TEAM\" DROP COLUMN xname ");
		
		// update schema
		//SchemaUpdate su = new SchemaUpdate(getCfg());
		//su.execute(true,true);
		
		try {
			SchemaValidator sv = new SchemaValidator(getCfg());
			sv.validate();
			fail("should fail since we mutated the current schema.");
		} catch(HibernateException he) {
			
		}
		
		// it's time to clean our database
		se.drop(true,true);
		
		// then the schemas and false table.

		stat.execute("DROP TABLE \"SA\".\"Team\" ");
		stat.execute(" DROP SCHEMA sa ");
		stat.execute("DROP SCHEMA sb ");
		stat.close();
		session.close();
	}

}
