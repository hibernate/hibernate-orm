/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.vibur.internal;

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
	@SuppressWarnings("unchecked")
	public void configure(Map configurationValues) {
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
	public boolean isUnwrappableAs(Class unwrapType) {
		return ConnectionProvider.class.equals( unwrapType ) ||
				ViburDBCPConnectionProvider.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T> T unwrap(Class<T> unwrapType) {
		if ( isUnwrappableAs( unwrapType ) ) {
			return (T) this;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	private static Properties transform(Map<String, String> configurationValues) {
		Properties result = new Properties();

		String driverClassName = configurationValues.get( DRIVER );
		if ( driverClassName != null ) {
			result.setProperty( "driverClassName", driverClassName );
		}
		String jdbcUrl = configurationValues.get( URL );
		if ( jdbcUrl != null ) {
			result.setProperty( "jdbcUrl", jdbcUrl );
		}

		String username = configurationValues.get( USER );
		if ( username != null ) {
			result.setProperty( "username", username );
		}
		String password = configurationValues.get( PASS );
		if ( password != null ) {
			result.setProperty( "password", password );
		}

		String defaultTransactionIsolationValue = configurationValues.get( ISOLATION );
		if ( defaultTransactionIsolationValue != null ) {
			result.setProperty( "defaultTransactionIsolationValue", defaultTransactionIsolationValue );
		}
		String defaultAutoCommit = configurationValues.get( AUTOCOMMIT );
		if ( defaultAutoCommit != null ) {
			result.setProperty( "defaultAutoCommit", defaultAutoCommit );
		}

		for ( Map.Entry<String, String> entry : configurationValues.entrySet() ) {
			String key = entry.getKey();
			if ( key.startsWith( VIBUR_PREFIX ) ) {
				key = key.substring( VIBUR_PREFIX.length() );
				result.setProperty( key, entry.getValue() );
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
}
