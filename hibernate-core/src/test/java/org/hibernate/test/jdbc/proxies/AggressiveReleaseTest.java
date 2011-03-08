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

import static org.hibernate.TestLogger.LOG;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.jdbc.internal.LogicalConnectionImpl;
import org.hibernate.engine.jdbc.internal.proxy.ProxyBuilder;
import org.hibernate.test.common.BasicTestingJdbcServiceImpl;
import org.hibernate.test.common.JournalingConnectionObserver;
import org.hibernate.testing.junit.UnitTestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class AggressiveReleaseTest extends UnitTestCase {

	private BasicTestingJdbcServiceImpl services = new BasicTestingJdbcServiceImpl();

	public AggressiveReleaseTest(String string) {
		super( string );
	}

	@Override
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
                    LOG.warn("could not close statement used to set up schema", ignore);
				}
			}
			if ( connection != null ) {
				try {
					connection.close();
				}
				catch ( SQLException ignore ) {
                    LOG.warn("could not close connection used to set up schema", ignore);
				}
			}
		}
	}

	@Override
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
                    LOG.warn("could not close statement used to set up schema", ignore);
				}
			}
			if ( connection != null ) {
				try {
					connection.close();
				}
				catch ( SQLException ignore ) {
                    LOG.warn("could not close connection used to set up schema", ignore);
				}
			}
		}

		services.release();
	}

	public void testBasicRelease() {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl( null, ConnectionReleaseMode.AFTER_STATEMENT, services );
		Connection proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );
		JournalingConnectionObserver observer = new JournalingConnectionObserver();
		logicalConnection.addObserver( observer );

		try {
			PreparedStatement ps = proxiedConnection.prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			ps.execute();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 0, observer.getPhysicalConnectionReleasedCount() );
			ps.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
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
		JournalingConnectionObserver observer = new JournalingConnectionObserver();
		logicalConnection.addObserver( observer );

		try {
			PreparedStatement ps = proxiedConnection.prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			ps.execute();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 0, observer.getPhysicalConnectionReleasedCount() );
			ps.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );

			// open a result set and hold it open...
			ps = proxiedConnection.prepareStatement( "select * from SANDBOX_JDBC_TST" );
			ps.executeQuery();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );

			// open a second result set
			PreparedStatement ps2 = proxiedConnection.prepareStatement( "select * from SANDBOX_JDBC_TST" );
			ps2.execute();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
			// and close it...
			ps2.close();
			// the release should be circumvented...
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );

			// let the close of the logical connection below release all resources (hopefully)...
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			logicalConnection.close();
		}

		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
		assertEquals( 2, observer.getPhysicalConnectionReleasedCount() );
	}

	public void testReleaseCircumventedManually() {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl( null, ConnectionReleaseMode.AFTER_STATEMENT, services );
		Connection proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );
		JournalingConnectionObserver observer = new JournalingConnectionObserver();
		logicalConnection.addObserver( observer );

		try {
			PreparedStatement ps = proxiedConnection.prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			ps.execute();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 0, observer.getPhysicalConnectionReleasedCount() );
			ps.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 1, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );

			// disable releases...
			logicalConnection.disableReleases();

			// open a result set...
			ps = proxiedConnection.prepareStatement( "select * from SANDBOX_JDBC_TST" );
			ps.executeQuery();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );
			// and close it...
			ps.close();
			// the release should be circumvented...
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
			assertEquals( 1, observer.getPhysicalConnectionReleasedCount() );

			// let the close of the logical connection below release all resources (hopefully)...
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			logicalConnection.close();
		}

		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		assertEquals( 2, observer.getPhysicalConnectionObtainedCount() );
		assertEquals( 2, observer.getPhysicalConnectionReleasedCount() );
	}
}
