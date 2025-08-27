/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import java.sql.Connection;
import java.sql.SQLException;
import jakarta.persistence.PersistenceException;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;

import org.jboss.logging.Logger;

import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_MESSAGE_LOGGER;

/**
 * Implementation of JdbcConnectionAccess for use in cases where we
 * leverage a ConnectionProvider for access to JDBC Connections.
 *
 * @author Steve Ebersole
 */
public class JdbcConnectionAccessConnectionProviderImpl implements JdbcConnectionAccess {
	private static final Logger log = Logger.getLogger( JdbcConnectionAccessConnectionProviderImpl.class );

	private final ConnectionProvider connectionProvider;
	private final Connection jdbcConnection;
	private final boolean wasInitiallyAutoCommit;

	public JdbcConnectionAccessConnectionProviderImpl(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;

		try {
			this.jdbcConnection = connectionProvider.getConnection();
		}
		catch (SQLException e) {
			throw new PersistenceException( "Unable to obtain JDBC Connection", e );
		}

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
		if ( connection != this.jdbcConnection ) {
			throw new PersistenceException(
					String.format(
							"Connection [%s] passed back to %s was not the one obtained [%s] from it",
							connection,
							JdbcConnectionAccessConnectionProviderImpl.class.getName(),
							jdbcConnection
					)
			);
		}

		// Reset auto-commit
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

		// Release the connection
		connectionProvider.closeConnection( jdbcConnection );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}
}
