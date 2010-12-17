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

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.internal.LogicalConnectionImpl;
import org.hibernate.engine.jdbc.internal.proxy.ProxyBuilder;
import org.hibernate.test.common.BasicTestingJdbcServiceImpl;
import org.hibernate.testing.junit.UnitTestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class BasicConnectionProxyTest extends UnitTestCase {
	private BasicTestingJdbcServiceImpl services = new BasicTestingJdbcServiceImpl();

	public BasicConnectionProxyTest(String string) {
		super( string );
	}

	public void setUp() {
		services.prepare( false );
	}

	public void tearDown() {
		services.release();
	}

	public void testDatabaseMetaDataHandling() throws Throwable {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl(
				null,
				ConnectionReleaseMode.AFTER_TRANSACTION,
				services,
				null
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
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			logicalConnection.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		}
	}

	public void testExceptionHandling() {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl(
				null,
				ConnectionReleaseMode.AFTER_TRANSACTION,
				services,
				null
		);
		Connection proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );
		try {
			proxiedConnection.prepareStatement( "select count(*) from NON_EXISTENT" ).executeQuery();
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		catch ( JDBCException ok ) {
			// expected outcome
		}
		finally {
			logicalConnection.close();
		}
	}

	public void testBasicJdbcUsage() throws JDBCException {
		LogicalConnectionImpl logicalConnection = new LogicalConnectionImpl(
				null,
				ConnectionReleaseMode.AFTER_TRANSACTION,
				services,
				null
		);
		Connection proxiedConnection = ProxyBuilder.buildConnection( logicalConnection );

		try {
			Statement stmnt = proxiedConnection.createStatement();
			stmnt.execute( "drop table SANDBOX_JDBC_TST if exists" );
			stmnt.execute( "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertTrue( logicalConnection.isPhysicallyConnected() );
			stmnt.close();
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
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			logicalConnection.close();
		}

		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
	}
}
