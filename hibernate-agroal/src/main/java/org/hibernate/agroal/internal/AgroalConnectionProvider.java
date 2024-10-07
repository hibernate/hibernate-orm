/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.agroal.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AgroalSettings;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
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

import static org.hibernate.cfg.AgroalSettings.AGROAL_CONFIG_PREFIX;
import static org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.allowJdbcMetadataAccess;

/**
 * ConnectionProvider based on Agroal connection pool
 * To use this ConnectionProvider set: <pre> hibernate.connection.provider_class AgroalConnectionProvider </pre>
 *
 * Usual hibernate properties are supported:
 * <pre>
 *     hibernate.connection.driver_class
 *     hibernate.connection.url
 *     hibernate.connection.username
 *     hibernate.connection.password
 *     hibernate.connection.autocommit
 *     hibernate.connection.isolation
 * </pre>
 *
 * Other configuration options are available, using the {@code hibernate.agroal} prefix
 *
 * @see AgroalSettings
 * @see AgroalPropertiesReader
 * @see AvailableSettings#CONNECTION_PROVIDER
 *
 * @author Luis Barreiro
 */
public class AgroalConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

	public static final String CONFIG_PREFIX = AGROAL_CONFIG_PREFIX + ".";
	private static final long serialVersionUID = 1L;
	private AgroalDataSource agroalDataSource = null;
	private boolean isMetadataAccessAllowed = true;

	// --- Configurable

	private static String extractIsolationAsString(Map<String, Object> properties) {
		Integer isolation = ConnectionProviderInitiator.extractIsolation( properties );
		if ( isolation != null ) {
			// Agroal resolves transaction isolation from the 'nice' name
			return ConnectionProviderInitiator.toIsolationNiceName( isolation );
		}
		return null;
	}

	private static void resolveIsolationSetting(Map<String, Object> properties, AgroalConnectionFactoryConfigurationSupplier cf) {
		String isolationString = extractIsolationAsString( properties );
		if ( isolationString != null ) {
			cf.jdbcTransactionIsolation( AgroalConnectionFactoryConfiguration.TransactionIsolation.valueOf( isolationString ) );
		}
	}

	private static <T> void copyProperty(Map<String, Object> properties, String key, Consumer<T> consumer, Function<String, T> converter) {
		Object value = properties.get( key );
		if ( value instanceof String ) {
			consumer.accept( converter.apply( (String) value ) );
		}
	}

	@Override
	public void configure(Map<String, Object> props) throws HibernateException {
		isMetadataAccessAllowed = allowJdbcMetadataAccess( props );

		ConnectionInfoLogger.INSTANCE.configureConnectionPool( "Agroal" );
		try {
			AgroalPropertiesReader agroalProperties = new AgroalPropertiesReader( CONFIG_PREFIX )
					.readProperties( (Map) props ); //TODO: this is a garbage cast
			agroalProperties.modify().connectionPoolConfiguration( cp -> cp.connectionFactoryConfiguration( cf -> {
				copyProperty( props, AvailableSettings.DRIVER, cf::connectionProviderClassName, Function.identity() );
				copyProperty( props, AvailableSettings.URL, cf::jdbcUrl, Function.identity() );
				copyProperty( props, AvailableSettings.USER, cf::principal, NamePrincipal::new );
				copyProperty( props, AvailableSettings.PASS, cf::credential, SimplePassword::new );
				copyProperty( props, AvailableSettings.AUTOCOMMIT, cf::autoCommit, Boolean::valueOf );
				resolveIsolationSetting( props, cf );
				return cf;
			} ) );

			agroalDataSource = AgroalDataSource.from( agroalProperties );
		}
		catch ( Exception e ) {
			ConnectionInfoLogger.INSTANCE.unableToInstantiateConnectionPool( e );
			throw new HibernateException( e );
		}
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
		final AgroalConnectionPoolConfiguration acpc = agroalDataSource.getConfiguration().connectionPoolConfiguration();
		final AgroalConnectionFactoryConfiguration acfc = acpc.connectionFactoryConfiguration();


		return new DatabaseConnectionInfoImpl(
				acfc.jdbcUrl(),
				// Attempt to resolve the driver name from the dialect, in case it wasn't explicitly set and access to
				// the database metadata is allowed
				acfc.connectionProviderClass() != null ? acfc.connectionProviderClass().toString() : extractDriverNameFromMetadata(),
				dialect.getVersion(),
				Boolean.toString( acfc.autoCommit() ),
				acfc.jdbcTransactionIsolation() != null
						? ConnectionProviderInitiator.toIsolationNiceName( acfc.jdbcTransactionIsolation().level() )
						: null,
				acpc.minSize(),
				acpc.minSize()
		);
	}

	private String extractDriverNameFromMetadata() {
		if (isMetadataAccessAllowed) {
			try ( Connection conn = getConnection() ) {
				DatabaseMetaData dbmd = conn.getMetaData();
				return dbmd.getDriverName();
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
			ConnectionInfoLogger.INSTANCE.cleaningUpConnectionPool( agroalDataSource.getConfiguration().connectionPoolConfiguration().
																			connectionFactoryConfiguration().jdbcUrl() );
			agroalDataSource.close();
		}
	}
}
