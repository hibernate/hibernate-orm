/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

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

	public ConnectionProviderDelegate() {
	}

	public ConnectionProviderDelegate(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map configurationValues) {
		if ( connectionProvider == null ) {
			@SuppressWarnings("unchecked")
			Map<String, Object> settings = new HashMap<>( configurationValues );
			settings.remove( AvailableSettings.CONNECTION_PROVIDER );
			connectionProvider = ConnectionProviderInitiator.INSTANCE.initiateService(
					settings,
					serviceRegistry
			);
			if ( connectionProvider instanceof Configurable ) {
				Configurable configurableConnectionProvider = (Configurable) connectionProvider;
				configurableConnectionProvider.configure( settings );
			}
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		return connectionProvider.getConnection();
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		connectionProvider.closeConnection( conn );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return connectionProvider.supportsAggressiveRelease();
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
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
