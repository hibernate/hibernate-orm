/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

/**
 * @author Steve Ebersole
 */
class ContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
	private final Object tenantIdentifier;
	private final boolean readOnly;
	private final SessionEventListener listener;
	private final MultiTenantConnectionProvider<Object> connectionProvider;
	private final SharedSessionContractImplementor session;


	ContextualJdbcConnectionAccess(
			Object tenantIdentifier,
			boolean readOnly,
			SessionEventListener listener,
			MultiTenantConnectionProvider<Object> connectionProvider,
			SharedSessionContractImplementor session) {
		this.tenantIdentifier = tenantIdentifier;
		this.readOnly = readOnly;
		this.listener = listener;
		this.connectionProvider = connectionProvider;
		this.session = session;
	}

	@Override
	public Connection obtainConnection() throws SQLException {
		if ( tenantIdentifier == null ) {
			throw new HibernateException( "Tenant identifier required" );
		}

		final var eventMonitor = session.getEventMonitor();
		final var connectionAcquisitionEvent = eventMonitor.beginJdbcConnectionAcquisitionEvent();
		try {
			listener.jdbcConnectionAcquisitionStart();
			return readOnly
					? connectionProvider.getReadOnlyConnection( tenantIdentifier )
					: connectionProvider.getConnection( tenantIdentifier );
		}
		finally {
			eventMonitor.completeJdbcConnectionAcquisitionEvent( connectionAcquisitionEvent, session, tenantIdentifier );
			listener.jdbcConnectionAcquisitionEnd();
		}
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		if ( tenantIdentifier == null ) {
			throw new HibernateException( "Tenant identifier required" );
		}

		final var eventMonitor = session.getEventMonitor();
		final var connectionReleaseEvent = eventMonitor.beginJdbcConnectionReleaseEvent();
		try {
			listener.jdbcConnectionReleaseStart();
			if ( readOnly ) {
				connectionProvider.releaseReadOnlyConnection( tenantIdentifier, connection );
			}
			else {
				connectionProvider.releaseConnection( tenantIdentifier, connection );
			}
		}
		finally {
			eventMonitor.completeJdbcConnectionReleaseEvent( connectionReleaseEvent, session, tenantIdentifier );
			listener.jdbcConnectionReleaseEnd();
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return connectionProvider.supportsAggressiveRelease();
	}
}
