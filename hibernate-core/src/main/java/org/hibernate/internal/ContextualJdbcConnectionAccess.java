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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.jfr.internal.JfrEventManager;
import org.hibernate.event.jfr.JdbcConnectionAcquisitionEvent;
import org.hibernate.event.jfr.JdbcConnectionReleaseEvent;

/**
 * @author Steve Ebersole
 */
public class ContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
	private final Object tenantIdentifier;
	private final SessionEventListener listener;
	private final MultiTenantConnectionProvider<Object> connectionProvider;
	private final SharedSessionContractImplementor session;


	public ContextualJdbcConnectionAccess(
			Object tenantIdentifier,
			SessionEventListener listener,
			MultiTenantConnectionProvider<Object> connectionProvider,
			SharedSessionContractImplementor session) {
		this.tenantIdentifier = tenantIdentifier;
		this.listener = listener;
		this.connectionProvider = connectionProvider;
		this.session = session;
	}

	@Override
	public Connection obtainConnection() throws SQLException {
		if ( tenantIdentifier == null ) {
			throw new HibernateException( "Tenant identifier required" );
		}

		final JdbcConnectionAcquisitionEvent jdbcConnectionAcquisitionEvent = JfrEventManager.beginJdbcConnectionAcquisitionEvent();
		try {
			listener.jdbcConnectionAcquisitionStart();
			return connectionProvider.getConnection( tenantIdentifier );
		}
		finally {
			JfrEventManager.completeJdbcConnectionAcquisitionEvent(
					jdbcConnectionAcquisitionEvent,
					session,
					tenantIdentifier
			);
			listener.jdbcConnectionAcquisitionEnd();
		}
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		if ( tenantIdentifier == null ) {
			throw new HibernateException( "Tenant identifier required" );
		}

		final JdbcConnectionReleaseEvent jdbcConnectionReleaseEvent = JfrEventManager.beginJdbcConnectionReleaseEvent();
		try {
			listener.jdbcConnectionReleaseStart();
			connectionProvider.releaseConnection( tenantIdentifier, connection );
		}
		finally {
			JfrEventManager.completeJdbcConnectionReleaseEvent( jdbcConnectionReleaseEvent, session, tenantIdentifier );
			listener.jdbcConnectionReleaseEnd();
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return connectionProvider.supportsAggressiveRelease();
	}
}
