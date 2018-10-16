/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.agroal.internal;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalPropertiesReader;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * ConnectionProvider based on Agroal connection pool
 * To use this ConnectionProvider set: <pre> hibernate.connection.provider_class AgroalConnectionProvider </pre>
 * ( @see AvailableSettings#CONNECTION_PROVIDER )
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
 * Other configuration options are available, using the <pre>hibernate.agroal</pre> prefix ( @see AgroalPropertiesReader )
 *
 * @author Luis Barreiro
 */
public class AgroalConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

	public static final String CONFIG_PREFIX = "hibernate.agroal.";
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger( AgroalConnectionProvider.class );
	private AgroalDataSource agroalDataSource = null;

	// --- Configurable

	private static void resolveIsolationSetting(Map<String, String> properties, AgroalConnectionFactoryConfigurationSupplier cf) {
		Integer isolation = ConnectionProviderInitiator.extractIsolation( properties );
		if ( isolation != null ) {
			// Agroal resolves transaction isolation from the 'nice' name
			String isolationString = ConnectionProviderInitiator.toIsolationNiceName( isolation );
			cf.jdbcTransactionIsolation( AgroalConnectionFactoryConfiguration.TransactionIsolation.valueOf( isolationString ) );
		}
	}

	private static <T> void copyProperty(Map<String, String> properties, String key, Consumer<T> consumer, Function<String, T> converter) {
		String value = properties.get( key );
		if ( value != null ) {
			consumer.accept( converter.apply( value ) );
		}
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public void configure(Map props) throws HibernateException {
		LOGGER.debug( "Configuring Agroal" );
		try {
			AgroalPropertiesReader agroalProperties = new AgroalPropertiesReader( CONFIG_PREFIX ).readProperties( props );
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
			throw new HibernateException( e );
		}
		LOGGER.debug( "Agroal Configured" );
	}

	// --- ConnectionProvider

	@Override
	public Connection getConnection() throws SQLException {
		return agroalDataSource == null ? null : agroalDataSource.getConnection();
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		conn.close();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		// Agroal supports integration with Narayana as the JTA provider, that would enable aggressive release
		// That logic is similar with what Hibernate does (however with better performance since it's integrated in the pool)
		// and therefore that integration is not leveraged right now.
		return false;
	}

	@Override
	@SuppressWarnings( "rawtypes" )
	public boolean isUnwrappableAs(Class unwrapType) {
		return ConnectionProvider.class.equals( unwrapType ) || AgroalConnectionProvider.class.isAssignableFrom( unwrapType ) || DataSource.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType ) || AgroalConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
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
		agroalDataSource.close();
	}
}
