package org.hibernate.internal;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.SessionEventListener;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;

/**
 * @author Steve Ebersole
 */
class NonContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
	private final SessionEventListener listener;
	private final ConnectionProvider connectionProvider;

	NonContextualJdbcConnectionAccess(
			SessionEventListener listener,
			ConnectionProvider connectionProvider) {
		this.listener = listener;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Connection obtainConnection() throws SQLException {
		try {
			listener.jdbcConnectionAcquisitionStart();
			return connectionProvider.getConnection();
		}
		finally {
			listener.jdbcConnectionAcquisitionEnd();
		}
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		try {
			listener.jdbcConnectionReleaseStart();
			connectionProvider.closeConnection( connection );
		}
		finally {
			listener.jdbcConnectionReleaseEnd();
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return connectionProvider.supportsAggressiveRelease();
	}
}
