/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.vibur.internal;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import org.vibur.dbcp.ViburDBCPDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import static org.hibernate.cfg.AvailableSettings.*;

/**
 * <p>ViburDBCP connection provider for Hibernate integration.
 * <p>
 * To use this connection provider set:
 * <pre>
 *   hibernate.connection.provider_class ViburDBCPConnectionProvider
 * </pre>
 * <p>
 * Supported Hibernate properties are:
 * <pre>
 *   hibernate.connection.driver_class
 *   hibernate.connection.url
 *   hibernate.connection.username
 *   hibernate.connection.password
 *   hibernate.connection.isolation
 *   hibernate.connection.autocommit
 * </pre>
 * <p>
 * All {@link org.vibur.dbcp.ViburConfig} properties are also supported via using the
 * {@code hibernate.vibur} prefix.
 *
 * @author Simeon Malchev
 * @see ConnectionProvider
 */
public class ViburDBCPConnectionProvider implements ConnectionProvider, Configurable, Stoppable {
	private static final String VIBUR_PREFIX = "hibernate.vibur.";

	private ViburDBCPDataSource dataSource = null;

	@Override
	public void configure(Map<String, Object> configurationValues) {
		dataSource = new ViburDBCPDataSource( transform( configurationValues ) );
		dataSource.start();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		conn.close();
	}

	@Override
	public void stop() {
		if ( dataSource != null ) {
			dataSource.terminate();
			dataSource = null;
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return ConnectionProvider.class.equals( unwrapType ) ||
				ViburDBCPConnectionProvider.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> unwrapType) {
		if ( isUnwrappableAs( unwrapType ) ) {
			return (T) this;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	private static Properties transform(Map<String, Object> configurationValues) {
		Properties result = new Properties();

		String driverClassName = getDriverClassName( configurationValues );
		if ( driverClassName != null ) {
			result.setProperty( "driverClassName", driverClassName );
		}
		String jdbcUrl = getUrl( configurationValues );
		if ( jdbcUrl != null ) {
			result.setProperty( "jdbcUrl", jdbcUrl );
		}

		String username = getUser( configurationValues );
		if ( username != null ) {
			result.setProperty( "username", username );
		}
		String password = getPassword( configurationValues );
		if ( password != null ) {
			result.setProperty( "password", password );
		}

		String defaultTransactionIsolationValue = (String) configurationValues.get( ISOLATION );
		if ( defaultTransactionIsolationValue != null ) {
			result.setProperty( "defaultTransactionIsolationValue", defaultTransactionIsolationValue );
		}
		String defaultAutoCommit = (String) configurationValues.get( AUTOCOMMIT );
		if ( defaultAutoCommit != null ) {
			result.setProperty( "defaultAutoCommit", defaultAutoCommit );
		}

		for ( Map.Entry<String, Object> entry : configurationValues.entrySet() ) {
			String key = entry.getKey();
			if ( key.startsWith( VIBUR_PREFIX ) ) {
				key = key.substring( VIBUR_PREFIX.length() );
				result.setProperty( key, (String) entry.getValue() );
			}
		}

		return result;
	}

	/**
	 * Visible for testing purposes.
	 */
	public ViburDBCPDataSource getDataSource() {
		return dataSource;
	}

	private static String getDriverClassName(Map<String, Object> configValues) {
		final String driverClassName = (String) configValues.get( AvailableSettings.DRIVER );
		if ( driverClassName != null ) {
			return driverClassName;
		}
		return (String) configValues.get( AvailableSettings.JAKARTA_JDBC_DRIVER );
	}

	private static String getUrl(Map<String, Object> configValues) {
		final String url = (String) configValues.get( AvailableSettings.URL );
		if ( url != null ) {
			return url;
		}
		return (String) configValues.get( AvailableSettings.JAKARTA_JDBC_URL );
	}

	public static String getPassword(Map<String, Object> configValues) {
		final String password = (String) configValues.get( Environment.PASS );
		if ( password != null ) {
			return password;
		}
		return (String) configValues.get( Environment.JAKARTA_JDBC_PASSWORD );
	}

	public static String getUser(Map<String, Object> configValues) {
		final String user = (String) configValues.get( Environment.USER );
		if ( user != null ) {
			return user;
		}
		return (String) configValues.get( Environment.JAKARTA_JDBC_USER );
	}
}
