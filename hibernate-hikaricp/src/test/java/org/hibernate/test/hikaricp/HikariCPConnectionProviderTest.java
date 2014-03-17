/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.test.hikaricp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class HikariCPConnectionProviderTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testHikariCPConnectionProvider() throws Exception {
		JdbcServices jdbcServices = serviceRegistry().getService( JdbcServices.class );
		ConnectionProvider provider = jdbcServices.getConnectionProvider();
		assertTrue( provider instanceof HikariCPConnectionProvider );

		HikariCPConnectionProvider hikariCP = (HikariCPConnectionProvider) provider;
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
			assertTrue( e.getMessage().contains( "Timeout" ) );
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
			assertTrue( e.getMessage().contains( "shutdown" ) );
		}
	}
}
