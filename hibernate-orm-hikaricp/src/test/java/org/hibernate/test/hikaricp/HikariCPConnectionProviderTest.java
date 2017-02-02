/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hikaricp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Brett Meyer
 */
public class HikariCPConnectionProviderTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testHikariCPConnectionProvider() throws Exception {
		JdbcServices jdbcServices = serviceRegistry().getService( JdbcServices.class );
		ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
				ConnectionProviderJdbcConnectionAccess.class,
				jdbcServices.getBootstrapJdbcConnectionAccess()
		);
		assertTyping( HikariCPConnectionProvider.class, connectionAccess.getConnectionProvider() );

		HikariCPConnectionProvider hikariCP = (HikariCPConnectionProvider) connectionAccess.getConnectionProvider();
		// For simplicity's sake, using the following in hibernate.properties:
		// hibernate.hikari.minimumPoolSize 2
		// hibernate.hikari.maximumPoolSize 2
		final List<Connection> conns = new ArrayList<Connection>();
		for ( int i = 0; i < 2; i++ ) {
			Connection conn = hikariCP.getConnection();
			assertNotNull( conn );
			assertFalse( conn.isClosed() );
			conns.add( conn );
		}

		try {
			hikariCP.getConnection();
			fail( "SQLException expected -- no more connections should have been available in the pool." );
		}
		catch (SQLException e) {
			// expected
			assertTrue( e.getMessage().contains( "Connection is not available, request timed out after" ) );
		}

		for ( Connection conn : conns ) {
			hikariCP.closeConnection( conn );
			assertTrue( conn.isClosed() );
		}

		releaseSessionFactory();

		try {
			hikariCP.getConnection();
			fail( "Exception expected -- the pool should have been shutdown." );
		}
		catch (Exception e) {
			// expected
			assertTrue( e.getMessage().contains( "has been closed" ) );
		}
	}
}
