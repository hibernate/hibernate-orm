/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.hibernate.SessionEventListener;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;

/**
 * @author Steve Ebersole
 */
public class NonContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
	private final SessionEventListener listener;
	private final ConnectionProvider connectionProvider;
	private final SharedSessionContractImplementor session;

	public NonContextualJdbcConnectionAccess(
			SessionEventListener listener,
			ConnectionProvider connectionProvider,
			SharedSessionContractImplementor session) {
		Objects.requireNonNull( listener );
		Objects.requireNonNull( connectionProvider );
		this.listener = listener;
		this.connectionProvider = connectionProvider;
		this.session = session;
	}

	@Override
	public Connection obtainConnection() throws SQLException {
		final EventMonitor eventMonitor = session.getEventMonitor();
		final DiagnosticEvent jdbcConnectionAcquisitionEvent = eventMonitor.beginJdbcConnectionAcquisitionEvent();
		try {
			listener.jdbcConnectionAcquisitionStart();
			return connectionProvider.getConnection();
		}
		finally {
			eventMonitor.completeJdbcConnectionAcquisitionEvent(
					jdbcConnectionAcquisitionEvent,
					session,
					null
			);
			listener.jdbcConnectionAcquisitionEnd();
		}
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		final EventMonitor eventMonitor = session.getEventMonitor();
		final DiagnosticEvent jdbcConnectionReleaseEvent = eventMonitor.beginJdbcConnectionReleaseEvent();
		try {
			listener.jdbcConnectionReleaseStart();
			connectionProvider.closeConnection( connection );
		}
		finally {
			eventMonitor.completeJdbcConnectionReleaseEvent( jdbcConnectionReleaseEvent, session, null );
			listener.jdbcConnectionReleaseEnd();
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return connectionProvider.supportsAggressiveRelease();
	}
}
