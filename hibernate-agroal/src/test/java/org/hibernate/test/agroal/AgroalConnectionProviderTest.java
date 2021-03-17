/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.agroal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.agroal.internal.AgroalConnectionProvider;

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
public class AgroalConnectionProviderTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testAgroalConnectionProvider() throws Exception {
		JdbcServices jdbcServices = serviceRegistry().getService( JdbcServices.class );
		ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
				ConnectionProviderJdbcConnectionAccess.class,
				jdbcServices.getBootstrapJdbcConnectionAccess()
		);
		assertTyping( AgroalConnectionProvider.class, connectionAccess.getConnectionProvider() );

		AgroalConnectionProvider agroalConnectionProvider = (AgroalConnectionProvider) connectionAccess.getConnectionProvider();
		// For simplicity's sake, using the following in hibernate.properties:
		// hibernate.agroal.maxSize 2
		// hibernate.agroal.minSize 2
		List<Connection> conns = new ArrayList<>();
		for ( int i = 0; i < 2; i++ ) {
			Connection conn = agroalConnectionProvider.getConnection();
			assertNotNull( conn );
			assertFalse( conn.isClosed() );
			conns.add( conn );
		}

		try {
			agroalConnectionProvider.getConnection();
			fail( "SQLException expected -- no more connections should have been available in the pool." );
		}
		catch (SQLException e) {
			// expected
			assertTrue( e.getMessage().contains( "timeout" ) );
		}

		for ( Connection conn : conns ) {
			agroalConnectionProvider.closeConnection( conn );
			assertTrue( conn.isClosed() );
		}

		releaseSessionFactory();

		try {
			agroalConnectionProvider.getConnection();
			fail( "Exception expected -- the pool should have been shutdown." );
		}
		catch (Exception e) {
			// expected
			assertTrue( e.getMessage() + " does not contain 'closed' or 'shutting down'",
					e.getMessage().contains( "closed" ) || e.getMessage().contains( "shutting down" ) );
		}
	}
}
