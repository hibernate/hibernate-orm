/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.spi;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Wrapped;

/**
 * A contract for obtaining JDBC connections and, optionally, for pooling connections.
 * <p>
 * Implementors must provide a public default constructor.
 * <p>
 * A {@code ConnectionProvider} may be selected using the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#CONNECTION_PROVIDER}.
 * <p>
 * It's not usual for an application to implement its own {@code ConnectionProvider}.
 * Instead, the Hibernate project provides pre-built implementations for a variety of
 * connection pools as add-on modules.
 * <p>
 * On the other hand, this is an extremely important extension point for integration
 * with containers and frameworks.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @see org.hibernate.cfg.AvailableSettings#CONNECTION_PROVIDER
 */
public interface ConnectionProvider extends Service, Wrapped {
	/**
	 * Obtains a connection for Hibernate use according to the underlying strategy of this provider.
	 *
	 * @return The obtained JDBC connection
	 *
	 * @throws SQLException Indicates a problem opening a connection
	 * @throws org.hibernate.HibernateException Indicates a problem otherwise obtaining a connection.
	 */
	Connection getConnection() throws SQLException;

	/**
	 * Release a connection from Hibernate use.
	 *
	 * @param connection The JDBC connection to release
	 *
	 * @throws SQLException Indicates a problem closing the connection
	 * @throws org.hibernate.HibernateException Indicates a problem otherwise releasing a connection.
	 */
	void closeConnection(Connection connection) throws SQLException;

	/**
	 * Does this connection provider support aggressive release of JDBC connections and later
	 * re-acquisition of those connections if needed?
	 * <p>
	 * This is used in conjunction with {@link org.hibernate.ConnectionReleaseMode#AFTER_STATEMENT}
	 * to aggressively release JDBC connections.  However, the configured {@link ConnectionProvider}
	 * must support re-acquisition of the same underlying connection for that semantic to work.
	 * <p>
	 * Typically, this is only true in managed environments where a container tracks connections
	 * by transaction or thread.
	 * <p>
	 * Note that JTA semantic depends on the fact that the underlying connection provider does
	 * support aggressive release.
	 *
	 * @return {@code true} if aggressive releasing is supported; {@code false} otherwise.
	 */
	boolean supportsAggressiveRelease();

	/**
	 * @return an informative instance of {@link DatabaseConnectionInfo} for logging.
	 *
	 * @since 6.6
	 */
	default DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return new DatabaseConnectionInfoImpl( dialect );
	}

	/**
	 * @return an informative instance of {@link DatabaseConnectionInfo} for logging.
	 *
	 * @since 7.0
	 */
	default DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect, ExtractedDatabaseMetaData metaData) {
		return getDatabaseConnectionInfo( dialect );
	}
}
