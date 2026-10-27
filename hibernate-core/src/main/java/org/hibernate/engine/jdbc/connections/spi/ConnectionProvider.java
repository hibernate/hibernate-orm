package org.hibernate.engine.jdbc.connections.spi;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;
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
	 * @throws org.hibernate.HibernateException Indicates a problem obtaining a connection.
	 */
	@Nonnull
	Connection getConnection() throws SQLException;

	/**
	 * Obtains a connection for Hibernate use according to the underlying strategy of this provider,
	 * using the given credentials.
	 *
	 * @param user The database user
	 * @param password The database password
	 * @return The obtained JDBC connection
	 *
	 * @throws SQLException Indicates a problem opening a connection
	 * @throws org.hibernate.HibernateException Indicates a problem obtaining a connection.
	 *
	 * @since 7.3
	 */
	@Incubating(since = "7.3")
	@Nonnull
	default Connection getConnection(String user, String password) throws SQLException {
		throw new UnsupportedOperationException(
				"ConnectionProvider does not support contextual credentials: "
						+ getClass().getTypeName()
						+ " (use a different ConnectionProvider for credentials-based multitenancy)" );
	}

	/**
	 * Obtain a connection from the named data source, using the configured credentials
	 * or the data source's defaults.
	 *
	 * @param dataSourceName The data source JNDI name
	 * @throws UnsupportedOperationException if named data sources are not supported
	 * @since 8.0
	 */
	@Incubating(since = "8.0")
	@Nonnull
	default Connection getConnection(String dataSourceName) throws SQLException {
		throw new UnsupportedOperationException( "ConnectionProvider does not support named data sources: "
				+ getClass().getTypeName() );
	}

	/**
	 * Obtain a connection from the named data source using the given credentials.
	 *
	 * @param dataSourceName The data source JNDI name
	 * @param user The database user
	 * @param password The database password
	 * @throws UnsupportedOperationException if named data sources with contextual credentials are not supported
	 * @since 8.0
	 */
	@Incubating(since = "8.0")
	@Nonnull
	default Connection getConnection(String dataSourceName, String user, String password) throws SQLException {
		throw new UnsupportedOperationException( "ConnectionProvider does not support named data sources with contextual credentials: "
				+ getClass().getTypeName() );
	}

	/**
	 * Obtain a connection for read-only use from the named data source.
	 *
	 * @implNote By default, delegates to {@link #getConnection(String)}. An override
	 * returning a distinct replica must also override {@link #closeReadOnlyConnection(Connection)}.
	 * @since 8.0
	 */
	@Incubating(since = "8.0")
	@Nonnull
	default Connection getReadOnlyConnection(String dataSourceName) throws SQLException {
		return getConnection( dataSourceName );
	}

	/**
	 * Obtain a connection for read-only use from the named data source using the given credentials.
	 *
	 * @implNote By default, delegates to {@link #getConnection(String, String, String)}. An override
	 * returning a distinct replica must also override {@link #closeReadOnlyConnection(Connection)}.
	 * @since 8.0
	 */
	@Incubating(since = "8.0")
	@Nonnull
	default Connection getReadOnlyConnection(String dataSourceName, String user, String password) throws SQLException {
		return getConnection( dataSourceName, user, password );
	}

	/**
	 * Obtains a connection to a read-only replica for use according to the underlying
	 * strategy of this provider.
	 *
	 * @return The obtained JDBC connection
	 *
	 * @throws SQLException Indicates a problem opening a connection
	 * @throws org.hibernate.HibernateException Indicates a problem obtaining a connection.
	 *
	 * @implNote This default implementation simply calls {@link #getConnection()},
	 * which returns a connection to a writable replica. If this operation is overridden
	 * to return a connection to a distinct read-only replica, the matching operation
	 * {@link #closeReadOnlyConnection(Connection)} must also be overridden.
	 *
	 * @since 7.2
	 */
	@Incubating(since = "7.2")
	@Nonnull
	default Connection getReadOnlyConnection() throws SQLException {
		return getConnection();
	}

	/**
	 * Release a connection from Hibernate use.
	 *
	 * @param connection The JDBC connection to release
	 *
	 * @throws SQLException Indicates a problem closing the connection
	 * @throws org.hibernate.HibernateException Indicates a problem releasing a connection.
	 */
	void closeConnection(@Nonnull Connection connection) throws SQLException;

	/**
	 * Release a connection to a read-only replica from Hibernate use.
	 *
	 * @param connection The JDBC connection to release
	 *
	 * @throws SQLException Indicates a problem closing the connection
	 * @throws org.hibernate.HibernateException Indicates a problem otherwise releasing a connection.
	 *
	 * @implNote This default implementation simply calls
	 *           {@link #closeConnection(Connection)}. If
	 *           {@link #getReadOnlyConnection()} is overridden to return a
	 *           connection to a distinct read-only replica, this operation must also
	 *           be overridden.
	 *
	 * @since 7.2
	 */
	@Incubating(since = "7.2")
	default void closeReadOnlyConnection(@Nonnull Connection connection) throws SQLException {
		closeConnection( connection );
	}

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
	 * Does this connection provider correctly set the
	 * {@linkplain java.sql.Connection#setSchema schema}
	 * of the returned JDBC connections?
	 * @return {@code true} if the connection provider handles this;
	 *         {@code false} if the client should set the schema
	 *
	 * @implNote If necessary, a {@code ConnectionProvider} may
	 * call {@link org.hibernate.context.spi.MultiTenancy#getTenantSchemaMapper}
	 * to obtain the {@link org.hibernate.context.spi.TenantSchemaMapper}.
	 */
	@Incubating(since = "7.2")
	default boolean handlesConnectionSchema() {
		return false;
	}

	/**
	 * Does this connection provider correctly set the
	 * {@linkplain java.sql.Connection#setReadOnly read-only mode}
	 * of the returned JDBC connections?
	 * @return {@code true} if the connection provider handles this;
	 *         {@code false} if the client should set the read-only mode
	 */
	@Incubating(since = "7.2")
	default boolean handlesConnectionReadOnly() {
		return false;
	}

	/**
	 * @return an informative instance of {@link DatabaseConnectionInfo} for logging.
	 *
	 * @since 6.6
	 */
	@Nonnull
	default DatabaseConnectionInfo getDatabaseConnectionInfo(@Nonnull Dialect dialect) {
		return new DatabaseConnectionInfoImpl( dialect );
	}

	/**
	 * @return an informative instance of {@link DatabaseConnectionInfo} for logging.
	 *
	 * @since 7.0
	 */
	@Nonnull
	default DatabaseConnectionInfo getDatabaseConnectionInfo(@Nonnull Dialect dialect, @Nonnull ExtractedDatabaseMetaData metaData) {
		return getDatabaseConnectionInfo( dialect );
	}
}
