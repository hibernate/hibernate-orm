package org.hibernate.internal;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

/**
 * @author Steve Ebersole
 */
class ContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
	private AbstractSessionImpl abstractSession;
	private final SessionEventListener listener;
	private final MultiTenantConnectionProvider connectionProvider;

	ContextualJdbcConnectionAccess(
			AbstractSessionImpl abstractSession,
			SessionEventListener listener,
			MultiTenantConnectionProvider connectionProvider) {
		this.abstractSession = abstractSession;
		this.listener = listener;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Connection obtainConnection() throws SQLException {
		if ( abstractSession.tenantIdentifier == null ) {
			throw new HibernateException( "Tenant identifier required!" );
		}

		try {
			listener.jdbcConnectionAcquisitionStart();
			return connectionProvider.getConnection( abstractSession.tenantIdentifier );
		}
		finally {
			listener.jdbcConnectionAcquisitionEnd();
		}
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		if ( abstractSession.tenantIdentifier == null ) {
			throw new HibernateException( "Tenant identifier required!" );
		}

		try {
			listener.jdbcConnectionReleaseStart();
			connectionProvider.releaseConnection( abstractSession.tenantIdentifier, connection );
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
