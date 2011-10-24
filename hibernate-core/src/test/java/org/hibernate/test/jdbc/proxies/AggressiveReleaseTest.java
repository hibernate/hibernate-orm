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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.jdbc.internal.LogicalConnectionImpl;
import org.hibernate.engine.jdbc.internal.proxy.ProxyBuilder;
import org.hibernate.test.common.BasicTestingJdbcServiceImpl;
import org.hibernate.test.common.JdbcConnectionAccessImpl;
import org.hibernate.test.common.JournalingConnectionObserver;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class AggressiveReleaseTest extends BaseUnitTestCase {
	private BasicTestingJdbcServiceImpl services = new BasicTestingJdbcServiceImpl();

	@Before
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

	@After
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
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl(
				null,
				ConnectionReleaseMode.AFTER_STATEMENT,
				services ,
				new JdbcConnectionAccessImpl( services.getConnectionProvider() )
		);
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

	@Test
	public void testReleaseCircumventedByHeldResources() {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl(
				null,
				ConnectionReleaseMode.AFTER_STATEMENT,
				services,
				new JdbcConnectionAccessImpl( services.getConnectionProvider() )
		);
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

	@Test
	public void testReleaseCircumventedManually() {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl(
				null,
				ConnectionReleaseMode.AFTER_STATEMENT,
				services,
				new JdbcConnectionAccessImpl( services.getConnectionProvider() ) 
		);
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
