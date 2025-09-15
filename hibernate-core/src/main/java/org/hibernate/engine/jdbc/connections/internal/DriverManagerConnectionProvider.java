/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.internal.log.ConnectionInfoLogger;

import static org.hibernate.cfg.JdbcSettings.AUTOCOMMIT;
import static org.hibernate.cfg.JdbcSettings.DRIVER;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_URL;
import static org.hibernate.cfg.JdbcSettings.POOL_SIZE;
import static org.hibernate.cfg.JdbcSettings.URL;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.extractIsolation;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.getConnectionProperties;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.toIsolationNiceName;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getCatalog;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getDriverName;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getFetchSize;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getIsolation;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getSchema;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.hasCatalog;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.hasSchema;
import static org.hibernate.internal.log.ConnectionInfoLogger.CONNECTION_INFO_LOGGER;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInt;
import static org.hibernate.internal.util.config.ConfigurationHelper.getLong;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

/**
 * A connection provider that uses the {@link DriverManager} directly to open connections and provides
 * a very rudimentary connection pool.
 *
 * @implNote Not intended for use in production systems!
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DriverManagerConnectionProvider
		implements ConnectionProvider, Configurable, Stoppable, ServiceRegistryAwareService, ConnectionValidator {

	public static final String MIN_SIZE = "hibernate.connection.min_pool_size";
	public static final String INITIAL_SIZE = "hibernate.connection.initial_pool_size";
	// in TimeUnit.SECONDS
	public static final String VALIDATION_INTERVAL = "hibernate.connection.pool_validation_interval";
	public static final String INIT_SQL ="hibernate.connection.init_sql";
	public static final String CONNECTION_CREATOR_FACTORY ="hibernate.connection.creator_factory_class";

	private volatile PoolState state;

	private static DatabaseConnectionInfo dbInfo;

	// create the pool ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private volatile ServiceRegistry serviceRegistry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map<String, Object> configurationValues) {
		CONNECTION_INFO_LOGGER.usingHibernateBuiltInConnectionPool();
		final PooledConnections pool = buildPool( configurationValues, serviceRegistry );
		final long validationInterval = getLong( VALIDATION_INTERVAL, configurationValues, 30 );
		state = new PoolState( pool, validationInterval );
	}

	private PooledConnections buildPool(Map<String,Object> configuration, ServiceRegistry serviceRegistry) {
		// connection settings
		final String url = jdbcUrl( configuration );
		final String driverClassName = getString( DRIVER, configuration );
		final Properties connectionProps = getConnectionProperties( configuration );
		final boolean autoCommit = getBoolean( AUTOCOMMIT, configuration );  // default autocommit to false
		final Integer isolation = extractIsolation( configuration );
		final String initSql = getString( INIT_SQL, configuration );

		// pool settings
		final int minSize = getInt( MIN_SIZE, configuration, 1 );
		final int maxSize = getInt( POOL_SIZE, configuration, 20 );
		final int initialSize = getInt( INITIAL_SIZE, configuration, minSize );

		final Driver driver = loadDriver( driverClassName, serviceRegistry, url );
		if ( driver == null ) {
			//we're hoping that the driver is already loaded
			logAvailableDrivers();
		}

		final var connectionCreator =
				getConnectionCreatorFactory( configuration, serviceRegistry )
						.create(
								driver,
								serviceRegistry,
								url,
								connectionProps,
								autoCommit,
								isolation,
								initSql,
								configuration
						);

		try ( var connection = connectionCreator.createConnection() ) {
			dbInfo = new DatabaseConnectionInfoImpl(
					DriverManagerConnectionProvider.class,
					url,
					getDriverName( connection ),
					null,
					SimpleDatabaseVersion.ZERO_VERSION,
					hasSchema( connection ),
					hasCatalog( connection ),
					getSchema( connection ),
					getCatalog( connection ),
					Boolean.toString( autoCommit ),
					isolation != null
							? toIsolationNiceName( isolation )
							: toIsolationNiceName( getIsolation( connection ) ),
					minSize,
					maxSize,
					getFetchSize( connection )
			);
			if ( !connection.getAutoCommit() ) {
				connection.rollback();
			}
		}
		catch (SQLException e) {
			throw new JDBCConnectionException( "Could not create connection", e );
		}

		return new PooledConnections.Builder( connectionCreator )
				.autoCommit( autoCommit )
				.initialSize( initialSize )
				.minSize( minSize )
				.maxSize( maxSize )
				.validator( this )
				.build();
	}

	private static Driver loadDriver(String driverClassName, ServiceRegistry serviceRegistry, String url) {
		if ( driverClassName != null ) {
			return loadDriverIfPossible( driverClassName, serviceRegistry );
		}
		else {
			// try to guess the driver class from the JDBC URL
			for ( var database: Database.values() ) {
				if ( database.matchesUrl( url ) ) {
					final String databaseDriverClassName = database.getDriverClassName( url );
					if ( databaseDriverClassName != null ) {
						try {
							return loadDriverIfPossible( databaseDriverClassName, serviceRegistry );
						}
						catch (Exception e) {
							// swallow it, since this was not an explicit setting by the user
						}
					}
				}
			}
			return null;
		}
	}

	private static void logAvailableDrivers() {
		CONNECTION_INFO_LOGGER.jdbcDriverNotSpecified();
		final var list = new StringBuilder();
		DriverManager.drivers()
				.forEach( driver -> {
					if ( !list.isEmpty() ) {
						list.append( ", " );
					}
					list.append( driver.getClass().getName() );
				} );
		CONNECTION_INFO_LOGGER.availableJdbcDrivers( list.toString() );
	}

	private static String jdbcUrl(Map<String, Object> configuration) {
		final String url = (String) configuration.get( URL );
		if ( url == null ) {
			throw new ConnectionProviderConfigurationException( "No JDBC URL specified by property '" + JAKARTA_JDBC_URL + "'" );
		}
		return url;
	}

	private static ConnectionCreatorFactory getConnectionCreatorFactory(
			Map<String, Object> configuration, ServiceRegistry serviceRegistry) {
		final Object connectionCreatorFactory = configuration.get( CONNECTION_CREATOR_FACTORY );
		final ConnectionCreatorFactory factory;
		if ( connectionCreatorFactory instanceof ConnectionCreatorFactory instance ) {
			factory = instance;
		}
		else if ( connectionCreatorFactory != null ) {
			factory = loadConnectionCreatorFactory( connectionCreatorFactory.toString(), serviceRegistry );
		}
		else {
			factory = null;
		}
		return factory == null ? ConnectionCreatorFactoryImpl.INSTANCE : factory;
	}

	private static Driver loadDriverIfPossible(String driverClassName, ServiceRegistry serviceRegistry) {
		if ( driverClassName == null ) {
			CONNECTION_INFO_LOGGER.debug( "No driver class specified" );
			return null;
		}
		else if ( serviceRegistry != null ) {
			final Class<Driver> driverClass =
					serviceRegistry.requireService( ClassLoaderService.class )
							.classForName( driverClassName );
			try {
				return driverClass.newInstance();
			}
			catch ( Exception e ) {
				throw new ServiceException( "Specified JDBC Driver " + driverClassName + " could not be loaded", e );
			}
		}
		else {
			try {
				return (Driver) Class.forName( driverClassName ).newInstance();
			}
			catch (Exception e1) {
				throw new ServiceException( "Specified JDBC Driver " + driverClassName + " could not be loaded", e1 );
			}
		}
	}

	private static ConnectionCreatorFactory loadConnectionCreatorFactory(
			String connectionCreatorFactoryClassName, ServiceRegistry serviceRegistry) {
		if ( connectionCreatorFactoryClassName == null ) {
			CONNECTION_INFO_LOGGER.debug( "No connection creator factory class specified" );
			return null;
		}
		else if ( serviceRegistry != null ) {
			final Class<ConnectionCreatorFactory> factoryClass =
					serviceRegistry.requireService( ClassLoaderService.class )
							.classForName( connectionCreatorFactoryClassName );
			try {
				return factoryClass.newInstance();
			}
			catch ( Exception e ) {
				throw new ServiceException( "Specified ConnectionCreatorFactory " + connectionCreatorFactoryClassName
											+ " could not be loaded", e );
			}
		}
		else {
			try {
				return (ConnectionCreatorFactory) Class.forName( connectionCreatorFactoryClassName ).newInstance();
			}
			catch (Exception e1) {
				throw new ServiceException( "Specified ConnectionCreatorFactory " + connectionCreatorFactoryClassName
											+ " could not be loaded", e1 );
			}
		}
	}


	// use the pool ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Connection getConnection() throws SQLException {
		if ( state == null ) {
			throw new IllegalStateException( "Cannot get a connection as the driver manager is not properly initialized" );
		}
		return state.getConnection();
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		if ( state == null ) {
			throw new IllegalStateException( "Cannot close a connection as the driver manager is not properly initialized" );
		}
		state.closeConnection( connection );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return new DatabaseConnectionInfoImpl(
				DriverManagerConnectionProvider.class,
				dbInfo.getJdbcUrl(),
				dbInfo.getJdbcDriver(),
				dialect.getClass(),
				dialect.getVersion(),
				dbInfo.hasSchema(),
				dbInfo.hasCatalog(),
				dbInfo.getSchema(),
				dbInfo.getCatalog(),
				dbInfo.getAutoCommitMode(),
				dbInfo.getIsolationLevel(),
				dbInfo.getPoolMinSize(),
				dbInfo.getPoolMaxSize(),
				dbInfo.getJdbcFetchSize()
		);
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return unwrapType.isAssignableFrom( DriverManagerConnectionProvider.class );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( unwrapType.isAssignableFrom( DriverManagerConnectionProvider.class ) ) {
			return (T) this;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	protected int getOpenConnections() {
		return state.getPool().getOpenConnectionCount();
	}

	protected void validateConnectionsReturned() {
		final int allocationCount = getOpenConnections();
		if ( allocationCount != 0 ) {
			CONNECTION_INFO_LOGGER.error( "Connection leak detected: there are " + allocationCount + " unclosed connections");
		}
	}

	protected void validateConnections(ConnectionValidator validator) {
		state.validateConnections( validator );
	}

	// destroy the pool ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void stop() {
		if ( state != null ) {
			state.stop();
			validateConnectionsReturned();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if ( state != null ) {
			state.stop();
		}
		super.finalize();
	}

	@Override
	public boolean isValid(Connection connection) throws SQLException {
		return true;
	}
}
