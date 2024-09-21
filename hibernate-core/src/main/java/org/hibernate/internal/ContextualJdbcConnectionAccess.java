/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateMonitoringEvent;

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

		final EventManager eventManager = session.getEventManager();
		final HibernateMonitoringEvent jdbcConnectionAcquisitionEvent = eventManager.beginJdbcConnectionAcquisitionEvent();
		try {
			listener.jdbcConnectionAcquisitionStart();
			return connectionProvider.getConnection( tenantIdentifier );
		}
		finally {
			eventManager.completeJdbcConnectionAcquisitionEvent(
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

		final EventManager eventManager = session.getEventManager();
		final HibernateMonitoringEvent jdbcConnectionReleaseEvent = eventManager.beginJdbcConnectionReleaseEvent();
		try {
			listener.jdbcConnectionReleaseStart();
			connectionProvider.releaseConnection( tenantIdentifier, connection );
		}
		finally {
			eventManager.completeJdbcConnectionReleaseEvent( jdbcConnectionReleaseEvent, session, tenantIdentifier );
			listener.jdbcConnectionReleaseEnd();
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return connectionProvider.supportsAggressiveRelease();
	}
}
