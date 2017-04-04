/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jdbc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.testing.boot.BasicTestingJdbcServiceImpl;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class AggressiveReleaseTest extends BaseCoreFunctionalTestCase {
	
	private BasicTestingJdbcServiceImpl services = new BasicTestingJdbcServiceImpl();
	
	@Override
	protected void prepareTest() throws Exception {
		services.prepare( true );

		Connection connection = null;
		Statement stmnt = null;
		try {
			connection = services.getBootstrapJdbcConnectionAccess().obtainConnection();
			stmnt = connection.createStatement();
			stmnt.execute( "drop table SANDBOX_JDBC_TST if exists" );
			stmnt.execute( "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
		}
		finally {
			if ( stmnt != null ) {
				try {
					stmnt.close();
				}
				catch ( SQLException ignore ) {
				}
			}
			if ( connection != null ) {
				try {
					services.getBootstrapJdbcConnectionAccess().releaseConnection( connection );
				}
				catch ( SQLException ignore ) {
				}
			}
		}
	}
	
	@Override
	protected void cleanupTest() throws Exception {
		Connection connection = null;
		Statement stmnt = null;
		try {
			connection = services.getBootstrapJdbcConnectionAccess().obtainConnection();
			stmnt = connection.createStatement();
			stmnt.execute( "drop table SANDBOX_JDBC_TST if exists" );
		}
		finally {
			if ( stmnt != null ) {
				try {
					stmnt.close();
				}
				catch ( SQLException ignore ) {
				}
			}
			if ( connection != null ) {
				try {
					services.getBootstrapJdbcConnectionAccess().releaseConnection( connection );
				}
				catch ( SQLException ignore ) {
				}
			}
		}

		services.release();
	}
	
	@Test
	public void testBasicRelease() {
//		Session session = openSession();
//		SessionImplementor sessionImpl = (SessionImplementor) session;
//
//		LogicalConnectionImplementor logicalConnection = new LogicalConnectionImpl( null,
//				ConnectionReleaseMode.AFTER_STATEMENT, services, new JdbcConnectionAccessImpl(
//						services.getConnectionProvider() ) );
//
//		JdbcCoordinatorImpl jdbcCoord = new JdbcCoordinatorImpl( logicalConnection,
//				sessionImpl );
//		JournalingConnectionObserver observer = new JournalingConnectionObserver();
//		logicalConnection.addObserver( observer );
//
//		try {
//			PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
//			ps.setLong( 1, 1 );
//			ps.setString( 2, "name" );
//			jdbcCoord.getResultSetReturn().execute( ps );
//			assertTrue( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 0, observer.getPhysicalConnectionReleasedCount() );
//			jdbcCoord.release( ps );
//			assertFalse( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
//		}
//		catch ( SQLException sqle ) {
//			fail( "incorrect exception type : sqlexception" );
//		}
//		finally {
//			session.close();
//		}
//
//		assertFalse( jdbcCoord.hasRegisteredResources() );
	}

	@Test
	public void testReleaseCircumventedByHeldResources() {
//		Session session = openSession();
//		SessionImplementor sessionImpl = (SessionImplementor) session;
//
//		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl( null,
//				ConnectionReleaseMode.AFTER_STATEMENT, services, new JdbcConnectionAccessImpl(
//						services.getConnectionProvider() ) );
//		JdbcCoordinatorImpl jdbcCoord = new JdbcCoordinatorImpl( logicalConnection,
//				sessionImpl.getTransactionCoordinator() );
//		JournalingConnectionObserver observer = new JournalingConnectionObserver();
//		logicalConnection.addObserver( observer );
//
//		try {
//			PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
//			ps.setLong( 1, 1 );
//			ps.setString( 2, "name" );
//			jdbcCoord.getResultSetReturn().execute( ps );
//			assertTrue( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 0, observer.getPhysicalConnectionReleasedCount() );
//			jdbcCoord.release( ps );
//			assertFalse( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
//
//			// open a result set and hold it open...
//			ps = jdbcCoord.getStatementPreparer().prepareStatement( "select * from SANDBOX_JDBC_TST" );
//			jdbcCoord.getResultSetReturn().extract( ps );
//			assertTrue( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
//
//			// open a second result set
//			PreparedStatement ps2 = jdbcCoord.getStatementPreparer().prepareStatement( "select * from SANDBOX_JDBC_TST" );
//			jdbcCoord.getResultSetReturn().execute( ps );
//			assertTrue( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
//			// and close it...
//			jdbcCoord.release( ps2 );
//			// the release should be circumvented...
//			assertTrue( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
//
//			// let the close of the logical connection below release all resources (hopefully)...
//		}
//		catch ( SQLException sqle ) {
//			fail( "incorrect exception type : sqlexception" );
//		}
//		finally {
//			jdbcCoord.close();
//			session.close();
//		}
//
//		assertFalse( jdbcCoord.hasRegisteredResources() );
//		assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
//		assertEquals( 2, observer.getPhysicalConnectionReleasedCount() );
	}

	@Test
	public void testReleaseCircumventedManually() {
//		Session session = openSession();
//		SessionImplementor sessionImpl = (SessionImplementor) session;
//
//		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl( null,
//				ConnectionReleaseMode.AFTER_STATEMENT, services, new JdbcConnectionAccessImpl(
//						services.getConnectionProvider() ) );
//		JdbcCoordinatorImpl jdbcCoord = new JdbcCoordinatorImpl( logicalConnection,
//				sessionImpl.getTransactionCoordinator() );
//		JournalingConnectionObserver observer = new JournalingConnectionObserver();
//		logicalConnection.addObserver( observer );
//
//		try {
//			PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
//			ps.setLong( 1, 1 );
//			ps.setString( 2, "name" );
//			jdbcCoord.getResultSetReturn().execute( ps );
//			assertTrue( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 0, observer.getPhysicalConnectionReleasedCount() );
//			jdbcCoord.release( ps );
//			assertFalse( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
//
//			// disable releases...
//			jdbcCoord.disableReleases();
//
//			// open a result set...
//			ps = jdbcCoord.getStatementPreparer().prepareStatement( "select * from SANDBOX_JDBC_TST" );
//			jdbcCoord.getResultSetReturn().extract( ps );
//			assertTrue( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
//			// and close it...
//			jdbcCoord.release( ps );
//			// the release should be circumvented...
//			assertFalse( jdbcCoord.hasRegisteredResources() );
//			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
//			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
//
//			// let the close of the logical connection below release all resources (hopefully)...
//		}
//		catch ( SQLException sqle ) {
//			fail( "incorrect exception type : sqlexception" );
//		}
//		finally {
//			jdbcCoord.close();
//			session.close();
//		}
//
//		assertFalse( jdbcCoord.hasRegisteredResources() );
//		assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
//		assertEquals( 2, observer.getPhysicalConnectionReleasedCount() );
	}
}
