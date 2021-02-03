/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * A special connection provider that is shared across test runs for better performance.
 *
 * @author Christian Beikov
 */
public class SharedDriverManagerConnectionProviderImpl extends DriverManagerConnectionProviderImpl {

	private static final SharedDriverManagerConnectionProviderImpl INSTANCE = new SharedDriverManagerConnectionProviderImpl();

	public static SharedDriverManagerConnectionProviderImpl getInstance() {
		return INSTANCE;
	}

	private Config config;

	@Override
	public void configure(Map configurationValues) {
		final Config c = new Config( configurationValues );
		if ( !c.isCompatible( config ) ) {
			if ( config != null ) {
				super.stop();
			}
			super.configure( configurationValues );
			config = c;
		}
	}

	@Override
	public boolean isValid(Connection connection) throws SQLException {
		// Wait at most 5 seconds to validate a connection is still valid
		return connection.isValid( 5 );
	}

	@Override
	public void stop() {
		// No need to stop as this is a shared instance
		validateConnectionsReturned();
	}

	public void reset() {
		super.stop();
		config = null;
	}

	private static class Config {
		private final boolean autoCommit;
		private final int minSize;
		private final int maxSize;
		private final int initialSize;
		private final String driverClassName;
		private final String url;
		private final Properties connectionProps;
		private final Integer isolation;

		public Config(Map configurationValues) {
			this.autoCommit = ConfigurationHelper.getBoolean( AvailableSettings.AUTOCOMMIT, configurationValues, false );
			this.minSize = ConfigurationHelper.getInt( MIN_SIZE, configurationValues, 2 );
			this.maxSize = ConfigurationHelper.getInt( AvailableSettings.POOL_SIZE, configurationValues, 20 );
			this.initialSize = ConfigurationHelper.getInt( INITIAL_SIZE, configurationValues, minSize );
			this.driverClassName = (String) configurationValues.get( AvailableSettings.DRIVER );
			this.url = (String) configurationValues.get( AvailableSettings.URL );
			this.connectionProps = ConnectionProviderInitiator.getConnectionProperties( configurationValues );
			this.isolation = ConnectionProviderInitiator.extractIsolation( configurationValues );
		}

		boolean isCompatible(Config config) {
			return config != null && autoCommit == config.autoCommit && minSize == config.minSize
					&& maxSize == config.maxSize && initialSize == config.initialSize
					&& driverClassName.equals( config.driverClassName )
					&& url.equals( config.url )
					&& connectionProps.equals( config.connectionProps )
					&& Objects.equals( isolation, config.isolation );
		}

	}
}
