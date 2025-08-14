/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.spi;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Wrapped;

/**
 * A specialized {@link Connection} provider contract used when the application is using
 * multi-tenancy support requiring tenant-aware connections.
 * <p>
 * A {@code MultiTenantConnectionProvider} may be selected using the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#MULTI_TENANT_CONNECTION_PROVIDER}.
 * <p>
 * An application usually implements its own custom {@code MultiTenantConnectionProvider}
 * by subclassing {@link AbstractMultiTenantConnectionProvider}.
 *
 * @param <T> The tenant identifier type
 *
 * @author Steve Ebersole
 *
 * @see AbstractMultiTenantConnectionProvider
 * @see org.hibernate.cfg.AvailableSettings#MULTI_TENANT_CONNECTION_PROVIDER
 */
public interface MultiTenantConnectionProvider<T> extends Service, Wrapped {
	/**
	 * Allows access to the database metadata of the underlying database(s) in situations
	 * where we do not have a tenant id (like startup processing, for example).
	 *
	 * @return The database metadata.
	 *
	 * @throws SQLException Indicates a problem opening a connection
	 */
	Connection getAnyConnection() throws SQLException;

	/**
	 * Release a connection obtained from {@link #getAnyConnection}
	 *
	 * @param connection The JDBC connection to release
	 *
	 * @throws SQLException Indicates a problem closing the connection
	 */
	void releaseAnyConnection(Connection connection) throws SQLException;

	/**
	 * Obtains a connection for use according to the underlying strategy of this provider.
	 *
	 * @param tenantIdentifier The identifier of the tenant for which to get a connection
	 *
	 * @return The obtained JDBC connection
	 *
	 * @throws SQLException Indicates a problem opening a connection
	 * @throws org.hibernate.HibernateException Indicates a problem obtaining a connection
	 */
	Connection getConnection(T tenantIdentifier) throws SQLException;

	/**
	 * Obtains a connection to a read-only replica for use according to the underlying
	 * strategy of this provider.
	 *
	 * @param tenantIdentifier The identifier of the tenant for which to get a connection
	 *
	 * @return The obtained JDBC connection
	 *
	 * @throws SQLException Indicates a problem opening a connection
	 * @throws org.hibernate.HibernateException Indicates a problem obtaining a connection
	 *
	 * @since 7.2
	 */
	@Incubating
	default Connection getReadOnlyConnection(T tenantIdentifier)
			throws SQLException {
		throw new UnsupportedOperationException( "No read-only replica is available" );
	}

	/**
	 * Release a connection from Hibernate use.
	 *
	 * @param connection The JDBC connection to release
	 * @param tenantIdentifier The identifier of the tenant.
	 *
	 * @throws SQLException Indicates a problem closing the connection
	 * @throws org.hibernate.HibernateException Indicates a problem releasing a connection
	 */
	void releaseConnection(T tenantIdentifier, Connection connection) throws SQLException;

	/**
	 * Release a connection to a read-only replica from Hibernate use.
	 *
	 * @param connection The JDBC connection to release
	 * @param tenantIdentifier The identifier of the tenant.
	 *
	 * @throws SQLException Indicates a problem closing the connection
	 * @throws org.hibernate.HibernateException Indicates a problem releasing a connection
	 *
	 * @since 7.2
	 */
	@Incubating
	default void releaseReadOnlyConnection(T tenantIdentifier, Connection connection)
			throws SQLException {
		throw new UnsupportedOperationException( "No read-only replica is available" );
	}

	/**
	 * Does this connection provider support aggressive release of JDBC connections and later
	 * re-acquisition of those connections if needed?
	 * <p>
	 * This is used in conjunction with {@link org.hibernate.ConnectionReleaseMode#AFTER_STATEMENT}
	 * to aggressively release JDBC connections. However, the configured {@link ConnectionProvider}
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

	default DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return new DatabaseConnectionInfoImpl( dialect );
	}

}
