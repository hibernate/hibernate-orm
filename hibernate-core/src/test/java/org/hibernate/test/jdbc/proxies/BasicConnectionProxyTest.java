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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.internal.LogicalConnectionImpl;
import org.hibernate.engine.jdbc.internal.proxy.ProxyBuilder;
import org.hibernate.test.common.BasicTestingJdbcServiceImpl;
import org.hibernate.test.common.JdbcConnectionAccessImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class BasicConnectionProxyTest extends BaseUnitTestCase {
	private BasicTestingJdbcServiceImpl services = new BasicTestingJdbcServiceImpl();

	@Before
	public void setUp() {
		services.prepare( false );
	}

	@After
	public void tearDown() {
		services.release();
	}

	@Test
	public void testDatabaseMetaDataHandling() throws Throwable {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl(
				null,
				ConnectionReleaseMode.AFTER_TRANSACTION,
				services,
				new JdbcConnectionAccessImpl( services.getConnectionProvider() )
		);
		Connection proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );
		try {
			DatabaseMetaData metaData = proxiedConnection.getMetaData();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			ResultSet rs1 = metaData.getCatalogs();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			rs1.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			metaData.getCatalogs();
			metaData.getSchemas();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		}
		catch ( SQLException e ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			logicalConnection.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		}
	}

	@Test
	public void testExceptionHandling() {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl(
				null,
				ConnectionReleaseMode.AFTER_TRANSACTION,
				services,
				new JdbcConnectionAccessImpl( services.getConnectionProvider() )
		);
		Connection proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );
		try {
			proxiedConnection.prepareStatement( "select count(*) from NON_EXISTENT" ).executeQuery();
		}
		catch ( SQLException e ) {
			fail( "incorrect exception type : sqlexception" );
		}
		catch ( JDBCException ok ) {
			// expected outcome
		}
		finally {
			logicalConnection.close();
		}
	}

	@Test
	public void testBasicJdbcUsage() throws JDBCException {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl(
				null,
				ConnectionReleaseMode.AFTER_TRANSACTION,
				services,
				new JdbcConnectionAccessImpl( services.getConnectionProvider() )
		);
		Connection proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );

		try {
			Statement statement = proxiedConnection.createStatement();
			statement.execute( "drop table SANDBOX_JDBC_TST if exists" );
			statement.execute( "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertTrue( logicalConnection.isPhysicallyConnected() );
			statement.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertTrue( logicalConnection.isPhysicallyConnected() ); // after_transaction specified

			PreparedStatement ps = proxiedConnection.prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			ps.execute();

			ps = proxiedConnection.prepareStatement( "select * from SANDBOX_JDBC_TST" );
			ps.executeQuery();

			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		}
		catch ( SQLException e ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			logicalConnection.close();
		}

		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
	}
}
