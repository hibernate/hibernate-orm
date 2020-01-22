package org.hibernate.test.dialect.functional;

import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Nathan Xu
 */
@RequiresDialect(MariaDB103Dialect.class)
@FailureExpected(jiraKey = "HHH-13373", message = "currently db user has no privilege to create secondary db and sequence within it")
public class MariaDBExtractSequenceMatadataTest extends BaseCoreFunctionalTestCase {

	private String secondaryDbName = "test_db";
	private String secondarySequenceName = "test_sequence";
	
	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			String currentDbName = session.doReturningWork( connection -> {
				try ( Statement stmt = connection.createStatement() ) {
					try ( ResultSet resultSet = stmt.executeQuery( "SELECT DATABASE()" ) ) {
						return resultSet.getString( 0 );
					}
				}
			});
			session.doWork( connection -> {
				try ( Statement stmt = connection.createStatement() ) {
					stmt.execute( "CREATE DATABASE " + secondaryDbName );
					stmt.execute( "USE " + secondaryDbName );
					stmt.execute( "CREATE SEQUENCE " + secondarySequenceName );
					stmt.execute( "USE " + currentDbName );
					stmt.execute( "DROP SEQUENCE IF EXISTS " + secondarySequenceName );
				}
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13373")
	public void testHibernateLaunchedSuccessfully() {
		doInHibernate( this::sessionFactory, s -> {} );
	}

	@Override
	protected void cleanupTest() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				try ( Statement stmt = connection.createStatement() ) {
					stmt.execute(  "DROP DATABASE " + secondaryDbName );
				}
				catch (Exception e) {
					// Ignore
				}
			} );
		} );
	}
	
}
