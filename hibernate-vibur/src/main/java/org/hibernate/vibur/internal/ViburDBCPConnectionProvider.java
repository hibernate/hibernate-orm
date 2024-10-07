/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.vibur.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import org.vibur.dbcp.ViburDBCPDataSource;

import static org.hibernate.cfg.AvailableSettings.AUTOCOMMIT;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.ISOLATION;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;
import static org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.allowJdbcMetadataAccess;

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

	private static final String VIBUR_CONFIG_PREFIX = "hibernate.vibur";
	private static final String VIBUR_PREFIX = VIBUR_CONFIG_PREFIX + ".";

	private ViburDBCPDataSource dataSource = null;
	private boolean isMetadataAccessAllowed = true;

	@Override
	public void configure(Map<String, Object> configurationValues) {
		isMetadataAccessAllowed = allowJdbcMetadataAccess( configurationValues );

		ConnectionInfoLogger.INSTANCE.configureConnectionPool( "Vibur" );

		dataSource = new ViburDBCPDataSource( transform( configurationValues ) );
		dataSource.start();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public void stop() {
		if ( dataSource != null ) {
			ConnectionInfoLogger.INSTANCE.cleaningUpConnectionPool( VIBUR_CONFIG_PREFIX );
			dataSource.terminate();
			dataSource = null;
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return new DatabaseConnectionInfoImpl(
				dataSource.getJdbcUrl(),
				// Attempt to resolve the driver name from the dialect, in case it wasn't explicitly set and access to
				// the database metadata is allowed
				!StringHelper.isBlank( dataSource.getDriverClassName() ) ? dataSource.getDriverClassName() : extractDriverNameFromMetadata(),
				dialect.getVersion(),
				String.valueOf( dataSource.getDefaultAutoCommit() ),
				dataSource.getDefaultTransactionIsolation(),
				dataSource.getPoolInitialSize(),
				dataSource.getPoolMaxSize()
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
			|| ViburDBCPConnectionProvider.class.isAssignableFrom( unwrapType );
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

		String driverClassName = (String) configurationValues.get( DRIVER );
		if ( driverClassName != null ) {
			result.setProperty( "driverClassName", driverClassName );
		}

		String jdbcUrl = (String) configurationValues.get( URL );
		if ( jdbcUrl != null ) {
			result.setProperty( "jdbcUrl", jdbcUrl );
		}

		String username = (String) configurationValues.get( USER );
		if ( username != null ) {
			result.setProperty( "username", username );
		}
		String password = (String) configurationValues.get( PASS );
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
}
