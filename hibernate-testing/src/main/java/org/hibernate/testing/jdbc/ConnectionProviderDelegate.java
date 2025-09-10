/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.testing.jdbc.GradleParallelTestingUsernameResolver.resolveFromSettings;

/**
 * This {@link ConnectionProvider} extends any other ConnectionProvider
 * that would be used by default taken the current configuration properties.
 *
 * @author Vlad Mihalcea
 */
public class ConnectionProviderDelegate implements
		ConnectionProvider,
		Configurable,
		ServiceRegistryAwareService,
		Stoppable {

	private ServiceRegistryImplementor serviceRegistry;

	private ConnectionProvider connectionProvider;
	private boolean configured;
	private final boolean forceSupportsAggressiveRelease;

	public ConnectionProviderDelegate(){
		this(false);
	}

	public ConnectionProviderDelegate(boolean forceSupportsAggressiveRelease) {
		this.forceSupportsAggressiveRelease = forceSupportsAggressiveRelease;
	}

	public ConnectionProviderDelegate(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
		this.forceSupportsAggressiveRelease = false;
	}

	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	public void setConnectionProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map<String, Object> configurationValues) {
		if ( !configured ) {
			if ( connectionProvider == null ) {
				resolveFromSettings( configurationValues );
				Map<String, Object> settings = new HashMap<>( configurationValues );
				settings.remove( AvailableSettings.CONNECTION_PROVIDER );
				connectionProvider = ConnectionProviderInitiator.INSTANCE.initiateService(
						settings,
						serviceRegistry
				);
			}
			if ( connectionProvider instanceof ServiceRegistryAwareService ) {
				( (ServiceRegistryAwareService) connectionProvider ).injectServices( serviceRegistry );
			}
			if ( connectionProvider instanceof Configurable ) {
				Configurable configurableConnectionProvider = (Configurable) connectionProvider;
				configurableConnectionProvider.configure( configurationValues );
			}
			configured = true;
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connectionProvider.getConnection();
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connectionProvider.closeConnection( connection );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return forceSupportsAggressiveRelease
			|| connectionProvider.supportsAggressiveRelease();
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return connectionProvider.getDatabaseConnectionInfo( dialect );
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect, ExtractedDatabaseMetaData metaData) {
		return connectionProvider.getDatabaseConnectionInfo( dialect, metaData );
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return connectionProvider.isUnwrappableAs( unwrapType );
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		return connectionProvider.unwrap( unwrapType );
	}

	@Override
	public void stop() {
		if ( connectionProvider instanceof Stoppable ) {
			( (Stoppable) connectionProvider ).stop();
		}
	}


}
