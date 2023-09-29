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
import java.util.Objects;

import org.hibernate.SessionEventListener;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.jfr.internal.JfrEventManager;
import org.hibernate.event.jfr.JdbcConnectionAcquisitionEvent;
import org.hibernate.event.jfr.JdbcConnectionReleaseEvent;

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
		final JdbcConnectionAcquisitionEvent jdbcConnectionAcquisitionEvent = JfrEventManager.beginJdbcConnectionAcquisitionEvent();
		try {
			listener.jdbcConnectionAcquisitionStart();
			return connectionProvider.getConnection();
		}
		finally {
			JfrEventManager.completeJdbcConnectionAcquisitionEvent(
					jdbcConnectionAcquisitionEvent,
					session,
					null
			);
			listener.jdbcConnectionAcquisitionEnd();
		}
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		final JdbcConnectionReleaseEvent jdbcConnectionReleaseEvent = JfrEventManager.beginJdbcConnectionReleaseEvent();
		try {
			listener.jdbcConnectionReleaseStart();
			connectionProvider.closeConnection( connection );
		}
		finally {
			JfrEventManager.completeJdbcConnectionReleaseEvent( jdbcConnectionReleaseEvent, session, null );
			listener.jdbcConnectionReleaseEnd();
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return connectionProvider.supportsAggressiveRelease();
	}
}
