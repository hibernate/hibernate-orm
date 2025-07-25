/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.agroal.internal;

import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation;
import org.hibernate.HibernateException;
import org.hibernate.cfg.AgroalSettings;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalPropertiesReader;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hibernate.cfg.AgroalSettings.AGROAL_CONFIG_PREFIX;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.toIsolationNiceName;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getFetchSize;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getIsolation;
import static org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.allowJdbcMetadataAccess;

/**
 * {@link ConnectionProvider} based on Agroal connection pool.
 * <p>
 * To force the use of this {@code ConnectionProvider} set
 * {@value org.hibernate.cfg.JdbcSettings#CONNECTION_PROVIDER}
 * to {@code agroal}.
 * <p>
 * Usual hibernate connection properties are supported:
 * <pre>
 *     hibernate.connection.driver_class
 *     hibernate.connection.url
 *     hibernate.connection.username
 *     hibernate.connection.password
 *     hibernate.connection.autocommit
 *     hibernate.connection.isolation
 * </pre>
 * <p>
 * Other configuration options are available, using the {@code hibernate.agroal} prefix.
 *
 * @see AgroalSettings
 * @see AgroalPropertiesReader
 * @see AvailableSettings#CONNECTION_PROVIDER
 *
 * @author Luis Barreiro
 */
public class AgroalConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

	public static final String CONFIG_PREFIX = AGROAL_CONFIG_PREFIX + ".";

	@Serial
	private static final long serialVersionUID = 1L;
	private AgroalDataSource agroalDataSource = null;
	private boolean isMetadataAccessAllowed = true;

	// --- Configurable

	private static String extractIsolationAsString(Map<String, Object> properties) {
		final Integer isolation = ConnectionProviderInitiator.extractIsolation( properties );
		return isolation != null ?
				// Agroal resolves transaction isolation from the 'nice' name
				toIsolationNiceName( isolation )
				: null;
	}

	private static void resolveIsolationSetting(Map<String, Object> properties, AgroalConnectionFactoryConfigurationSupplier cf) {
		final String isolationString = extractIsolationAsString( properties );
		if ( isolationString != null ) {
			cf.jdbcTransactionIsolation( TransactionIsolation.valueOf( isolationString ) );
		}
	}

	private static <T> void copyProperty(Map<String, Object> properties, String key, Consumer<T> consumer, Function<String, T> converter) {
		final Object value = properties.get( key );
		if ( value != null ) {
			consumer.accept( converter.apply( value.toString() ) );
		}
	}

	@Override
	public void configure(Map<String, Object> properties) throws HibernateException {
		isMetadataAccessAllowed = allowJdbcMetadataAccess( properties );

		ConnectionInfoLogger.INSTANCE.configureConnectionPool( "Agroal" );
		try {
			final Map<String, String> config = toStringValuedProperties( properties );
			if ( !properties.containsKey( AgroalSettings.AGROAL_MAX_SIZE ) ) {
				final String maxSize =
						properties.containsKey( JdbcSettings.POOL_SIZE )
								? properties.get( JdbcSettings.POOL_SIZE ).toString()
								: String.valueOf( 10 );
				config.put( AgroalSettings.AGROAL_MAX_SIZE, maxSize );
			}
			final AgroalPropertiesReader agroalProperties =
					new AgroalPropertiesReader( CONFIG_PREFIX )
							.readProperties( config );
			agroalProperties.modify()
					.connectionPoolConfiguration( cp -> cp.connectionFactoryConfiguration( cf -> {
				copyProperty( properties, JdbcSettings.DRIVER, cf::connectionProviderClassName, identity() );
				copyProperty( properties, JdbcSettings.URL, cf::jdbcUrl, identity() );
				copyProperty( properties, JdbcSettings.USER, cf::principal, NamePrincipal::new );
				copyProperty( properties, JdbcSettings.PASS, cf::credential, SimplePassword::new );
				copyProperty( properties, JdbcSettings.AUTOCOMMIT, cf::autoCommit, Boolean::valueOf );
				resolveIsolationSetting( properties, cf );
				return cf;
			} ) );

			agroalDataSource = AgroalDataSource.from( agroalProperties );
		}
		catch ( Exception e ) {
			ConnectionInfoLogger.INSTANCE.unableToInstantiateConnectionPool( e );
			throw new ConnectionProviderConfigurationException(
					"Could not configure Agroal: " + e.getMessage(),  e );
		}
	}

	private static Map<String,String> toStringValuedProperties(Map<String,Object> properties) {
		return properties.entrySet().stream()
				.collect( toMap( Map.Entry::getKey, e -> e.getValue().toString() ) );
	}

	// --- ConnectionProvider

	@Override
	public Connection getConnection() throws SQLException {
		return agroalDataSource == null ? null : agroalDataSource.getConnection();
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		// Agroal supports integration with Narayana as the JTA provider, that would enable aggressive release
		// That logic is similar with what Hibernate does (however with better performance since it's integrated in the pool)
		// and therefore that integration is not leveraged right now.
		return false;
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		final AgroalConnectionPoolConfiguration acpc =
				agroalDataSource.getConfiguration().connectionPoolConfiguration();
		final AgroalConnectionFactoryConfiguration acfc = acpc.connectionFactoryConfiguration();
		return new DatabaseConnectionInfoImpl(
				AgroalConnectionProvider.class,
				acfc.jdbcUrl(),
				// Attempt to resolve the driver name from the dialect,
				// in case it wasn't explicitly set and access to the
				// database metadata is allowed
				acfc.connectionProviderClass() != null
						? acfc.connectionProviderClass().toString()
						: extractDriverNameFromMetadata(),
				dialect.getVersion(),
				Boolean.toString( acfc.autoCommit() ),
				acfc.jdbcTransactionIsolation() != null && acfc.jdbcTransactionIsolation().isDefined()
						? toIsolationNiceName( acfc.jdbcTransactionIsolation().level() )
						: toIsolationNiceName( getIsolation( agroalDataSource ) ),
				acpc.minSize(),
				acpc.maxSize(),
				getFetchSize( agroalDataSource )
		);
	}

	private String extractDriverNameFromMetadata() {
		if (isMetadataAccessAllowed) {
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
			|| AgroalConnectionProvider.class.isAssignableFrom( unwrapType )
			|| DataSource.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType )
				|| AgroalConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		if ( DataSource.class.isAssignableFrom( unwrapType ) ) {
			return (T) agroalDataSource;
		}
		throw new UnknownUnwrapTypeException( unwrapType );
	}

	// --- Stoppable

	@Override
	public void stop() {
		if ( agroalDataSource != null ) {
			ConnectionInfoLogger.INSTANCE.cleaningUpConnectionPool(
					agroalDataSource.getConfiguration()
							.connectionPoolConfiguration()
							.connectionFactoryConfiguration()
							.jdbcUrl() );
			agroalDataSource.close();
		}
	}
}
