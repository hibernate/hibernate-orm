/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.boot;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class JdbcConnectionAccessImpl implements JdbcConnectionAccess {
	private final ConnectionProvider connectionProvider;

	public JdbcConnectionAccessImpl(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	public JdbcConnectionAccessImpl(ServiceRegistry serviceRegistry) {
		this( serviceRegistry.getService( ConnectionProvider.class ) );
	}

	@Override
	public Connection obtainConnection() throws SQLException {
		return connectionProvider.getConnection();
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		connectionProvider.closeConnection( connection );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return connectionProvider.supportsAggressiveRelease();
	}
}
