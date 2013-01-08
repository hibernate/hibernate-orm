/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.jdbc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Session;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.jdbc.internal.LogicalConnectionImpl;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.test.common.BasicTestingJdbcServiceImpl;
import org.hibernate.test.common.JdbcConnectionAccessImpl;
import org.hibernate.test.common.JournalingConnectionObserver;
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
			connection = services.getConnectionProvider().getConnection();
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
					connection.close();
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
			connection = services.getConnectionProvider().getConnection();
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
					connection.close();
				}
				catch ( SQLException ignore ) {
				}
			}
		}

		services.release();
	}
	
	@Test
	public void testBasicRelease() {
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl( null,
				ConnectionReleaseMode.AFTER_STATEMENT, services, new JdbcConnectionAccessImpl(
						services.getConnectionProvider() ) );
		JdbcCoordinatorImpl jdbcCoord = new JdbcCoordinatorImpl( logicalConnection,
				sessionImpl.getTransactionCoordinator() );
		JournalingConnectionObserver observer = new JournalingConnectionObserver();
		logicalConnection.addObserver( observer );

		try {
			PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			jdbcCoord.getResultSetReturn().execute( ps );
			assertTrue( jdbcCoord.hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 0, observer.getPhysicalConnectionReleasedCount() );
			jdbcCoord.release( ps );
			assertFalse( jdbcCoord.hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			session.close();
		}

		assertFalse( jdbcCoord.hasRegisteredResources() );
	}

	@Test
	public void testReleaseCircumventedByHeldResources() {
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl( null,
				ConnectionReleaseMode.AFTER_STATEMENT, services, new JdbcConnectionAccessImpl(
						services.getConnectionProvider() ) );
		JdbcCoordinatorImpl jdbcCoord = new JdbcCoordinatorImpl( logicalConnection,
				sessionImpl.getTransactionCoordinator() );
		JournalingConnectionObserver observer = new JournalingConnectionObserver();
		logicalConnection.addObserver( observer );

		try {
			PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			jdbcCoord.getResultSetReturn().execute( ps );
			assertTrue( jdbcCoord.hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 0, observer.getPhysicalConnectionReleasedCount() );
			jdbcCoord.release( ps );
			assertFalse( jdbcCoord.hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
	
			// open a result set and hold it open...
			ps = jdbcCoord.getStatementPreparer().prepareStatement( "select * from SANDBOX_JDBC_TST" );
			jdbcCoord.getResultSetReturn().extract( ps );
			assertTrue( jdbcCoord.hasRegisteredResources() );
			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
	
			// open a second result set
			PreparedStatement ps2 = jdbcCoord.getStatementPreparer().prepareStatement( "select * from SANDBOX_JDBC_TST" );
			jdbcCoord.getResultSetReturn().execute( ps );
			assertTrue( jdbcCoord.hasRegisteredResources() );
			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
			// and close it...
			jdbcCoord.release( ps2 );
			// the release should be circumvented...
			assertTrue( jdbcCoord.hasRegisteredResources() );
			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
	
			// let the close of the logical connection below release all resources (hopefully)...
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			jdbcCoord.close();
			session.close();
		}

		assertFalse( jdbcCoord.hasRegisteredResources() );
		assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
		assertEquals( 2, observer.getPhysicalConnectionReleasedCount() );
	}

	@Test
	public void testReleaseCircumventedManually() {
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl( null,
				ConnectionReleaseMode.AFTER_STATEMENT, services, new JdbcConnectionAccessImpl(
						services.getConnectionProvider() ) );
		JdbcCoordinatorImpl jdbcCoord = new JdbcCoordinatorImpl( logicalConnection,
				sessionImpl.getTransactionCoordinator() );
		JournalingConnectionObserver observer = new JournalingConnectionObserver();
		logicalConnection.addObserver( observer );

		try {
			PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			jdbcCoord.getResultSetReturn().execute( ps );
			assertTrue( jdbcCoord.hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 0, observer.getPhysicalConnectionReleasedCount() );
			jdbcCoord.release( ps );
			assertFalse( jdbcCoord.hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
	
			// disable releases...
			jdbcCoord.disableReleases();
	
			// open a result set...
			ps = jdbcCoord.getStatementPreparer().prepareStatement( "select * from SANDBOX_JDBC_TST" );
			jdbcCoord.getResultSetReturn().extract( ps );
			assertTrue( jdbcCoord.hasRegisteredResources() );
			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
			// and close it...
			jdbcCoord.release( ps );
			// the release should be circumvented...
			assertFalse( jdbcCoord.hasRegisteredResources() );
			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
	
			// let the close of the logical connection below release all resources (hopefully)...
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			jdbcCoord.close();
			session.close();
		}

		assertFalse( jdbcCoord.hasRegisteredResources() );
		assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
		assertEquals( 2, observer.getPhysicalConnectionReleasedCount() );
	}
}
