/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import java.sql.Connection;
import java.sql.SQLException;
import jakarta.persistence.PersistenceException;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;

import org.jboss.logging.Logger;

import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_MESSAGE_LOGGER;

/**
 * Implementation of JdbcConnectionAccess for cases where we are provided
 * a JDBC Connection to use.
 *
 * @author Steve Ebersole
 */
public class JdbcConnectionAccessProvidedConnectionImpl implements JdbcConnectionAccess {
	private static final Logger log = Logger.getLogger( JdbcConnectionAccessProvidedConnectionImpl.class );

	private final Connection jdbcConnection;
	private final boolean wasInitiallyAutoCommit;

	public JdbcConnectionAccessProvidedConnectionImpl(Connection jdbcConnection) {
		this.jdbcConnection = jdbcConnection;

		boolean wasInitiallyAutoCommit;
		try {
			wasInitiallyAutoCommit = jdbcConnection.getAutoCommit();
			if ( !wasInitiallyAutoCommit ) {
				try {
					jdbcConnection.setAutoCommit( true );
				}
				catch (SQLException exception) {
					throw new PersistenceException(
							String.format(
									"Could not set provided connection [%s] to auto-commit mode" +
											" (needed for schema generation)",
									jdbcConnection
							),
							exception
					);
				}
			}
		}
		catch (SQLException ignore) {
			wasInitiallyAutoCommit = false;
		}

		log.tracef( "wasInitiallyAutoCommit=%s", wasInitiallyAutoCommit );
		this.wasInitiallyAutoCommit = wasInitiallyAutoCommit;
	}

	@Override
	public Connection obtainConnection() throws SQLException {
		return jdbcConnection;
	}

	@Override
	public void releaseConnection(Connection connection) throws SQLException {
		// NOTE: reset auto-commit, but *do not* close the Connection.
		//       The application handed us this connection.

		if ( !wasInitiallyAutoCommit ) {
			try {
				if ( jdbcConnection.getAutoCommit() ) {
					jdbcConnection.setAutoCommit( false );
				}
			}
			catch (SQLException exception) {
				JDBC_MESSAGE_LOGGER.unableToResetAutoCommitDisabled( exception );
			}
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}
}
