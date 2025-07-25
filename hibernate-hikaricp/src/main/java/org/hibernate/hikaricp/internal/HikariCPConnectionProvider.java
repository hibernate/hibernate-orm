/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.hikaricp.internal;

import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.toIsolationNiceName;
import static org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.allowJdbcMetadataAccess;
import static org.hibernate.hikaricp.internal.HikariConfigurationUtil.loadConfiguration;
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
	private boolean isMetadataAccessAllowed = true;

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
	public void configure(Map<String, Object> configurationValues) throws HibernateException {
		try {
			isMetadataAccessAllowed = allowJdbcMetadataAccess( configurationValues );
			ConnectionInfoLogger.INSTANCE.configureConnectionPool( "HikariCP" );
			hikariConfig = loadConfiguration( configurationValues );
			hikariDataSource = new HikariDataSource( hikariConfig );
		}
		catch (Exception e) {
			ConnectionInfoLogger.INSTANCE.unableToInstantiateConnectionPool( e );
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
		return new DatabaseConnectionInfoImpl(
				HikariCPConnectionProvider.class,
				hikariConfig.getJdbcUrl(),
				// Attempt to resolve the driver name from the dialect,
				// in case it wasn't explicitly set and access to the
				// database metadata is allowed
				isBlank( hikariConfig.getDriverClassName() )
						? extractDriverNameFromMetadata()
						: hikariConfig.getDriverClassName(),
				dialect.getVersion(),
				Boolean.toString( hikariConfig.isAutoCommit() ),
				hikariConfig.getTransactionIsolation() != null
						? hikariConfig.getTransactionIsolation()
						: toIsolationNiceName( getIsolation( hikariDataSource ) ),
				hikariConfig.getMinimumIdle(),
				hikariConfig.getMaximumPoolSize(),
				getFetchSize( hikariDataSource )
		);
	}

	private static Integer getFetchSize(DataSource dataSource) {
		try ( var conn = dataSource.getConnection() ) {
			try ( var statement = conn.createStatement() ) {
				return statement.getFetchSize();
			}
		}
		catch ( SQLException ignored ) {
			return null;
		}
	}

	private static Integer getIsolation(DataSource dataSource) {
		try ( var conn = dataSource.getConnection() ) {
			return conn.getTransactionIsolation();
		}
		catch ( SQLException ignored ) {
			return null;
		}
	}

	private String extractDriverNameFromMetadata() {
		if ( isMetadataAccessAllowed ) {
			try ( Connection conn = getConnection() ) {
				return conn.getMetaData().getDriverName();
			}
			catch (SQLException e) {
				// Do nothing
			}
		}
		return null;
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return ConnectionProvider.class.equals( unwrapType )
			|| HikariCPConnectionProvider.class.isAssignableFrom( unwrapType )
			|| DataSource.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType )
				|| HikariCPConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else if ( DataSource.class.isAssignableFrom( unwrapType ) ) {
			return (T) hikariDataSource;
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
			ConnectionInfoLogger.INSTANCE.cleaningUpConnectionPool( "HikariCP" );
			hikariDataSource.close();
		}
	}
}
