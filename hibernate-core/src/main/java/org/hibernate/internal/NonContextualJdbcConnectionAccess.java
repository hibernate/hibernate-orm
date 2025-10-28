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

/**
 * @author Steve Ebersole
 */
public class NonContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
	private final boolean readOnly;
	private final SessionEventListener listener;
	private final ConnectionProvider connectionProvider;
	private final SharedSessionContractImplementor session;

	public NonContextualJdbcConnectionAccess(
			boolean readOnly,
			SessionEventListener listener,
			ConnectionProvider connectionProvider,
			SharedSessionContractImplementor session) {
		Objects.requireNonNull( listener );
		Objects.requireNonNull( connectionProvider );
		this.readOnly = readOnly;
		this.listener = listener;
		this.connectionProvider = connectionProvider;
		this.session = session;
	}

	@Override
	public Connection obtainConnection() throws SQLException {
		final var eventMonitor = session.getEventMonitor();
		final var connectionAcquisitionEvent = eventMonitor.beginJdbcConnectionAcquisitionEvent();
		try {
			listener.jdbcConnectionAcquisitionStart();
			return readOnly
					? connectionProvider.getReadOnlyConnection()
					: connectionProvider.getConnection();
		}
		finally {
			eventMonitor.completeJdbcConnectionAcquisitionEvent( connectionAcquisitionEvent, session, null );
			listener.jdbcConnectionAcquisitionEnd();
		}
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		final var eventMonitor = session.getEventMonitor();
		final var connectionReleaseEvent = eventMonitor.beginJdbcConnectionReleaseEvent();
		try {
			listener.jdbcConnectionReleaseStart();
			if ( readOnly ) {
				connectionProvider.closeReadOnlyConnection( connection );
			}
			else {
				connectionProvider.closeConnection( connection );
			}
		}
		finally {
			eventMonitor.completeJdbcConnectionReleaseEvent( connectionReleaseEvent, session, null );
			listener.jdbcConnectionReleaseEnd();
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return connectionProvider.supportsAggressiveRelease();
	}
}
