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
package org.hibernate.test.jdbc.proxies;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.jdbc.internal.LogicalConnectionImpl;
import org.hibernate.engine.jdbc.spi.ConnectionObserver;
import org.hibernate.engine.jdbc.internal.proxy.ProxyBuilder;
import org.hibernate.stat.ConcurrentStatisticsImpl;
import org.hibernate.test.common.BasicTestingJdbcServiceImpl;
import org.hibernate.testing.junit.UnitTestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class AggressiveReleaseTest extends UnitTestCase {

	private static final Logger log = LoggerFactory.getLogger( AggressiveReleaseTest.class );
	private BasicTestingJdbcServiceImpl services = new BasicTestingJdbcServiceImpl();

	private static class ConnectionCounter implements ConnectionObserver {
		public int obtainCount = 0;
		public int releaseCount = 0;

		public void physicalConnectionObtained(Connection connection) {
			obtainCount++;
		}

		public void physicalConnectionReleased() {
			releaseCount++;
		}

		public void logicalConnectionClosed() {
		}
	}

	public AggressiveReleaseTest(String string) {
		super( string );
	}

	public void setUp() throws SQLException {
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
					log.warn( "could not close statement used to set up schema", ignore );
				}
			}
			if ( connection != null ) {
				try {
					connection.close();
				}
				catch ( SQLException ignore ) {
					log.warn( "could not close connection used to set up schema", ignore );
				}
			}
		}
	}

	public void tearDown() throws SQLException {
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
					log.warn( "could not close statement used to set up schema", ignore );
				}
			}
			if ( connection != null ) {
				try {
					connection.close();
				}
				catch ( SQLException ignore ) {
					log.warn( "could not close connection used to set up schema", ignore );
				}
			}
		}

		services.release();
	}

	public void testBasicRelease() {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl( null, ConnectionReleaseMode.AFTER_STATEMENT, services );
		Connection proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );
		ConnectionCounter observer = new ConnectionCounter();
		logicalConnection.addObserver( observer );

		try {
			PreparedStatement ps = proxiedConnection.prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			ps.execute();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.obtainCount );
			assertEquals( 0, observer.releaseCount );
			ps.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.obtainCount );
			assertEquals( 1, observer.releaseCount );
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			logicalConnection.close();
		}

		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
	}

	public void testReleaseCircumventedByHeldResources() {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl( null, ConnectionReleaseMode.AFTER_STATEMENT, services );
		Connection proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );
		ConnectionCounter observer = new ConnectionCounter();
		logicalConnection.addObserver( observer );

		try {
			PreparedStatement ps = proxiedConnection.prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			ps.execute();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.obtainCount );
			assertEquals( 0, observer.releaseCount );
			ps.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.obtainCount );
			assertEquals( 1, observer.releaseCount );

			// open a result set and hold it open...
			ps = proxiedConnection.prepareStatement( "select * from SANDBOX_JDBC_TST" );
			ps.executeQuery();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 2, observer.obtainCount );
			assertEquals( 1, observer.releaseCount );

			// open a second result set
			PreparedStatement ps2 = proxiedConnection.prepareStatement( "select * from SANDBOX_JDBC_TST" );
			ps2.execute();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 2, observer.obtainCount );
			assertEquals( 1, observer.releaseCount );
			// and close it...
			ps2.close();
			// the release should be circumvented...
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 2, observer.obtainCount );
			assertEquals( 1, observer.releaseCount );

			// let the close of the logical connection below release all resources (hopefully)...
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			logicalConnection.close();
		}

		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		assertEquals( 2, observer.obtainCount );
		assertEquals( 2, observer.releaseCount );
	}

	public void testReleaseCircumventedManually() {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl( null, ConnectionReleaseMode.AFTER_STATEMENT, services );
		Connection proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );
		ConnectionCounter observer = new ConnectionCounter();
		logicalConnection.addObserver( observer );

		try {
			PreparedStatement ps = proxiedConnection.prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			ps.execute();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.obtainCount );
			assertEquals( 0, observer.releaseCount );
			ps.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.obtainCount );
			assertEquals( 1, observer.releaseCount );

			// disable releases...
			logicalConnection.disableReleases();

			// open a result set...
			ps = proxiedConnection.prepareStatement( "select * from SANDBOX_JDBC_TST" );
			ps.executeQuery();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 2, observer.obtainCount );
			assertEquals( 1, observer.releaseCount );
			// and close it...
			ps.close();
			// the release should be circumvented...
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 2, observer.obtainCount );
			assertEquals( 1, observer.releaseCount );

			// let the close of the logical connection below release all resources (hopefully)...
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			logicalConnection.close();
		}

		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		assertEquals( 2, observer.obtainCount );
		assertEquals( 2, observer.releaseCount );
	}
}
