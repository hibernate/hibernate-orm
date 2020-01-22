package org.hibernate.test.dialect.functional;

import java.sql.*;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.*;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Nathan Xu
 */
@RequiresDialect(MariaDB103Dialect.class)
@FailureExpected(jiraKey = "HHH-13373", message = "currently db user has no privilege to create secondary db and sequence within it")
public class MariaDBExtractSequenceMatadataTest extends BaseCoreFunctionalTestCase {

	private static String primaryDbName;
	private static String primarySequenceName = "seq_HHH13373";

	private static String secondaryDbName = "secondary_db_HHH13373";
	private static String secondarySequenceName = "secondary_seq_HHH13373";
	
	@BeforeClassOnce
	public static void setUpDBs() throws Exception {
		try ( Connection conn = getConnection() ) {
			try ( Statement stmt = conn.createStatement() ) {
				try ( ResultSet resultSet = stmt.executeQuery( "SELECT DATABASE()" ) ) {
					assert resultSet.next();
					primaryDbName = resultSet.getString( 1 );
				}
				stmt.execute( "CREATE DATABASE " + secondaryDbName );
				stmt.execute( "USE " + secondaryDbName );
				stmt.execute( "CREATE SEQUENCE " + secondarySequenceName );
				stmt.execute( "USE " + primaryDbName );
				stmt.execute( "DROP SEQUENCE IF EXISTS " + secondarySequenceName );
				stmt.execute( "CREATE SEQUENCE IF NOT EXISTS " + primarySequenceName );
			}
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13373")
	public void testHibernateLaunchedSuccessfully() {
		JdbcEnvironment jdbcEnvironment = serviceRegistry().getService( JdbcEnvironment.class );
		Assert.assertFalse( jdbcEnvironment.getExtractedDatabaseMetaData().getSequenceInformationList().isEmpty() );
	}

	@AfterClassOnce
	public static void tearDownDBs() {
		try ( Connection conn = getConnection() ) {
			try ( Statement stmt = conn.createStatement() ) {
				stmt.execute(  "DROP DATABASE " + secondaryDbName );
			}
			catch ( Exception e ) {
				// Ignore
			}
		}
		catch ( Exception e ) {
			// Ignore
		}
	}
	
	private static Connection getConnection() throws SQLException {
		String url = Environment.getProperties().getProperty( Environment.URL );
		String user = Environment.getProperties().getProperty( Environment.USER );
		String password = Environment.getProperties().getProperty( Environment.PASS );
		return DriverManager.getConnection( url, user, password );
	}
	
}
