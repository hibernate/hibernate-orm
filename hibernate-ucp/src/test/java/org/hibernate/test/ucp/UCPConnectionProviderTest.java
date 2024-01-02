package org.hibernate.test.ucp;

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.oracleucp.internal.UCPConnectionProvider;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "The jTDS driver doesn't implement Connection#isValid so this fails")
public class UCPConnectionProviderTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testUCPConnectionProvider() throws Exception {
		JdbcServices jdbcServices = serviceRegistry().getService( JdbcServices.class );
		ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
				ConnectionProviderJdbcConnectionAccess.class,
				jdbcServices.getBootstrapJdbcConnectionAccess()
		);
		assertTyping( UCPConnectionProvider.class, connectionAccess.getConnectionProvider() );

		UCPConnectionProvider ucpProvider = (UCPConnectionProvider) connectionAccess.getConnectionProvider();
		final List<Connection> conns = new ArrayList<Connection>();
		for ( int i = 0; i < 2; i++ ) {
			Connection conn = ucpProvider.getConnection();
			assertNotNull( conn );
			assertFalse( conn.isClosed() );
			conns.add( conn );
		}

		try {
			ucpProvider.getConnection();
			fail( "SQLException expected -- no more connections should have been available in the pool." );
		}
		catch (SQLException e) {
			// expected
			assertTrue( e.getMessage().contains( "Failed to get a connection" ) );
		}

		for ( Connection conn : conns ) {
			ucpProvider.closeConnection( conn );
			assertTrue( conn.isClosed() );
		}

		releaseSessionFactory();

		try {
			ucpProvider.getConnection();
			fail( "Exception expected -- the pool should have been shutdown." );
		}
		catch (Exception e) {
			// expected
			assertTrue( e.getMessage().contains( "Failed to get a connection" ) );
		}
	}
}
