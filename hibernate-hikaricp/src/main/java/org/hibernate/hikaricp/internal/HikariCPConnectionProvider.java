/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.hikaricp.internal;

import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.toIsolationNiceName;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getCatalog;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getDriverName;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getFetchSize;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getIsolation;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getSchema;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.hasCatalog;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.hasSchema;
import static org.hibernate.hikaricp.internal.HikariConfigurationUtil.loadConfiguration;
import static org.hibernate.internal.log.ConnectionInfoLogger.CONNECTION_INFO_LOGGER;
import static org.hibernate.internal.util.StringHelper.isBlank;

/**
 * {@link ConnectionProvider} based on HikariCP connection pool.
 * <p>
 * To force the use of this {@code ConnectionProvider} set
 * {@value org.hibernate.cfg.JdbcSettings#CONNECTION_PROVIDER}
 * to {@code hikari} or {@code hikaricp}.
 *
 * @author Brett Wooldridge
 * @author Luca Burgazzoli
 */
public class HikariCPConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

	@Serial
	private static final long serialVersionUID = -9131625057941275711L;

	/**
	 * HikariCP configuration.
	 */
	private HikariConfig hikariConfig = null;

	/**
	 * HikariCP data source.
	 */
	private HikariDataSource hikariDataSource = null;

	// *************************************************************************
	// Configurable
	// *************************************************************************

	@Override
	public void configure(Map<String, Object> configuration) throws HibernateException {
		try {
			CONNECTION_INFO_LOGGER.configureConnectionPool( "HikariCP" );
			hikariConfig = loadConfiguration( configuration );
			hikariDataSource = new HikariDataSource( hikariConfig );
		}
		catch (Exception e) {
			CONNECTION_INFO_LOGGER.unableToInstantiateConnectionPool( e );
			throw new ConnectionProviderConfigurationException(
					"Could not configure HikariCP: " + e.getMessage(),  e );
		}
	}

	// *************************************************************************
	// ConnectionProvider
	// *************************************************************************

	@Override
	public Connection getConnection() throws SQLException {
		return hikariDataSource != null ? hikariDataSource.getConnection() : null;
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		try ( var connection = hikariDataSource.getConnection() ) {
			final var info = new DatabaseConnectionInfoImpl(
					HikariCPConnectionProvider.class,
					hikariConfig.getJdbcUrl(),
					// Attempt to resolve the driver name from the dialect,
					// in case it wasn't explicitly set and access to the
					// database metadata is allowed
					isBlank( hikariConfig.getDriverClassName() )
							? getDriverName( connection )
							: hikariConfig.getDriverClassName(),
					dialect.getClass(),
					dialect.getVersion(),
					hasSchema( connection ),
					hasCatalog( connection ),
					hikariConfig.getSchema() != null
							? hikariConfig.getSchema()
							: getSchema( connection ),
					hikariConfig.getCatalog() != null
							? hikariConfig.getCatalog()
							: getCatalog( connection ),
					Boolean.toString( hikariConfig.isAutoCommit() ),
					hikariConfig.getTransactionIsolation() != null
							? hikariConfig.getTransactionIsolation()
							: toIsolationNiceName( getIsolation( connection ) ),
					hikariConfig.getMinimumIdle(),
					hikariConfig.getMaximumPoolSize(),
					getFetchSize( connection )
			);
			if ( !connection.getAutoCommit() ) {
				connection.rollback();
			}
			return info;
		}
		catch (SQLException e) {
			throw new JDBCConnectionException( "Could not create connection", e );
		}
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return unwrapType.isAssignableFrom( HikariCPConnectionProvider.class )
			|| unwrapType.isAssignableFrom( HikariDataSource.class )
			|| unwrapType.isAssignableFrom( HikariConfig.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
		if ( unwrapType.isAssignableFrom( HikariCPConnectionProvider.class ) ) {
			return (T) this;
		}
		else if ( unwrapType.isAssignableFrom( HikariDataSource.class ) ) {
			return (T) hikariDataSource;
		}
		else if ( unwrapType.isAssignableFrom( HikariConfig.class ) ) {
			return (T) hikariConfig;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	// *************************************************************************
	// Stoppable
	// *************************************************************************

	@Override
	public void stop() {
		if ( hikariDataSource != null ) {
			CONNECTION_INFO_LOGGER.cleaningUpConnectionPool( "HikariCP" );
			hikariDataSource.close();
		}
	}
}
