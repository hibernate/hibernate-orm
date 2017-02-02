/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
public class ContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
	private final String tenantIdentifier;
	private final SessionEventListener listener;
	private final MultiTenantConnectionProvider connectionProvider;

	public ContextualJdbcConnectionAccess(
			String tenantIdentifier,
			SessionEventListener listener,
			MultiTenantConnectionProvider connectionProvider) {
		this.tenantIdentifier = tenantIdentifier;
		this.listener = listener;
		this.connectionProvider = connectionProvider;
	}

	@Override
	public Connection obtainConnection() throws SQLException {
		if ( tenantIdentifier == null ) {
			throw new HibernateException( "Tenant identifier required!" );
		}

		try {
			listener.jdbcConnectionAcquisitionStart();
			return connectionProvider.getConnection( tenantIdentifier );
		}
		finally {
			listener.jdbcConnectionAcquisitionEnd();
		}
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		if ( tenantIdentifier == null ) {
			throw new HibernateException( "Tenant identifier required!" );
		}

		try {
			listener.jdbcConnectionReleaseStart();
			connectionProvider.releaseConnection( tenantIdentifier, connection );
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
