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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProviderConfigurationException;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
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
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getFetchSize;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getIsolation;
import static org.hibernate.engine.jdbc.connections.internal.DatabaseConnectionInfoImpl.getSchema;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getInt;
import static org.hibernate.internal.util.config.ConfigurationHelper.getLong;

/**
 * A connection provider that uses the {@link DriverManager} directly to open connections and provides
 * a very rudimentary connection pool.
 *
 * @implNote Not intended for use in production systems!
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DriverManagerConnectionProviderImpl
		implements ConnectionProvider, Configurable, Stoppable, ServiceRegistryAwareService, ConnectionValidator {

	//Thanks to Oleg Varaksin and his article on object pooling using the {@link java.util.concurrent}
	//package, from which much of the pooling code here is derived.
	//See http://ovaraksin.blogspot.com/2013/08/simple-and-lightweight-pool.html

	public static final String MIN_SIZE = "hibernate.connection.min_pool_size";
	public static final String INITIAL_SIZE = "hibernate.connection.initial_pool_size";
	// in TimeUnit.SECONDS
	public static final String VALIDATION_INTERVAL = "hibernate.connection.pool_validation_interval";
	public static final String INIT_SQL ="hibernate.connection.init_sql";
	public static final String CONNECTION_CREATOR_FACTORY ="hibernate.connection.creator_factory_class";

	private volatile PoolState state;

	private static DatabaseConnectionInfo dbInfo;

	// create the pool ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private volatile ServiceRegistryImplementor serviceRegistry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map<String, Object> configurationValues) {
		ConnectionInfoLogger.INSTANCE.usingHibernateBuiltInConnectionPool();
		PooledConnections pool = buildPool( configurationValues, serviceRegistry );
		final long validationInterval = getLong( VALIDATION_INTERVAL, configurationValues, 30 );
		this.state = new PoolState( pool, validationInterval );
	}

	private PooledConnections buildPool(Map<String,Object> configurationValues, ServiceRegistryImplementor serviceRegistry) {
		final boolean autoCommit = getBoolean( AUTOCOMMIT, configurationValues ); // default to false
		final int minSize = getInt( MIN_SIZE, configurationValues, 1 );
		final int maxSize = getInt( POOL_SIZE, configurationValues, 20 );
		final int initialSize = getInt( INITIAL_SIZE, configurationValues, minSize );

		final ConnectionCreator creator = buildCreator( configurationValues, serviceRegistry );
		return new PooledConnections.Builder( creator, autoCommit )
				.initialSize( initialSize )
				.minSize( minSize )
				.maxSize( maxSize )
				.validator( this )
				.build();
	}

	private static ConnectionCreator buildCreator(
			Map<String,Object> configurationValues, ServiceRegistryImplementor serviceRegistry) {
		final String url = jdbcUrl( configurationValues );

		String driverClassName = (String) configurationValues.get( DRIVER );
		boolean success = false;
		Driver driver = null;
		if ( driverClassName != null ) {
			driver = loadDriverIfPossible( driverClassName, serviceRegistry );
			success = true;
		}
		else {
			//try to guess the driver class from the JDBC URL
			for ( Database database: Database.values() ) {
				if ( database.matchesUrl( url ) ) {
					driverClassName = database.getDriverClassName( url );
					if ( driverClassName != null ) {
						try {
							loadDriverIfPossible( driverClassName, serviceRegistry );
							success = true;
						}
						catch (Exception e) {
							//swallow it, since this was not
							//an explicit setting by the user
						}
						break;
					}
				}
			}
		}

		final String driverList = success ? driverClassName : driverList();

		final Properties connectionProps = getConnectionProperties( configurationValues );

		final boolean autoCommit = getBoolean( AUTOCOMMIT, configurationValues );
		final Integer isolation = extractIsolation( configurationValues );
		final String initSql = (String) configurationValues.get( INIT_SQL );

		final var connectionCreator =
				getConnectionCreatorFactory( configurationValues, serviceRegistry )
						.create(
								driver,
								serviceRegistry,
								url,
								connectionProps,
								autoCommit,
								isolation,
								initSql,
								configurationValues
						);

		dbInfo = new DatabaseConnectionInfoImpl(
				DriverManagerConnectionProviderImpl.class,
				url,
				driverList,
				null,
				SimpleDatabaseVersion.ZERO_VERSION,
				getSchema( connectionCreator ),
				getCatalog( connectionCreator ),
				Boolean.toString( autoCommit ),
				isolation != null
						? toIsolationNiceName( isolation )
						: toIsolationNiceName( getIsolation( connectionCreator ) ),
				getInt( MIN_SIZE, configurationValues, 1 ),
				getInt( POOL_SIZE, configurationValues, 20 ),
				getFetchSize( connectionCreator )
		);

		return connectionCreator;
	}

	private static String driverList() {
		//we're hoping that the driver is already loaded
		ConnectionInfoLogger.INSTANCE.jdbcDriverNotSpecified();
		final var list = new StringBuilder();
		DriverManager.drivers()
				.forEach( driver -> {
					if ( !list.isEmpty() ) {
						list.append( ", " );
					}
					list.append( driver.getClass().getName() );
				} );
		return list.toString();
	}

	private static String jdbcUrl(Map<String, Object> configurationValues) {
		final String url = (String) configurationValues.get( URL );
		if ( url == null ) {
			throw new ConnectionProviderConfigurationException( "No JDBC URL specified by property '" + JAKARTA_JDBC_URL + "'" );
		}
		return url;
	}

	private static ConnectionCreatorFactory getConnectionCreatorFactory(
			Map<String, Object> configurationValues, ServiceRegistryImplementor serviceRegistry) {
		final Object connectionCreatorFactory = configurationValues.get( CONNECTION_CREATOR_FACTORY );
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

	private static Driver loadDriverIfPossible(String driverClassName, ServiceRegistryImplementor serviceRegistry) {
		if ( driverClassName == null ) {
			ConnectionInfoLogger.INSTANCE.debug( "No driver class specified" );
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
			String connectionCreatorFactoryClassName, ServiceRegistryImplementor serviceRegistry) {
		if ( connectionCreatorFactoryClassName == null ) {
			ConnectionInfoLogger.INSTANCE.debug( "No connection creator factory class specified" );
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
				throw new ServiceException( "Specified ConnectionCreatorFactory " + connectionCreatorFactoryClassName + " could not be loaded", e );
			}
		}
		else {
			try {
				return (ConnectionCreatorFactory) Class.forName( connectionCreatorFactoryClassName ).newInstance();
			}
			catch (Exception e1) {
				throw new ServiceException(
						"Specified ConnectionCreatorFactory " + connectionCreatorFactoryClassName + " could not be loaded",
						e1 );
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
				DriverManagerConnectionProviderImpl.class,
				dbInfo.getJdbcUrl(),
				dbInfo.getJdbcDriver(),
				dialect.getClass(),
				dialect.getVersion(),
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
		return ConnectionProvider.class.equals( unwrapType ) ||
				DriverManagerConnectionProviderImpl.class.isAssignableFrom( unwrapType );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T> T unwrap(Class<T> unwrapType) {
		if ( ConnectionProvider.class.equals( unwrapType ) ||
				DriverManagerConnectionProviderImpl.class.isAssignableFrom( unwrapType ) ) {
			return (T) this;
		}
		else {
			throw new UnknownUnwrapTypeException( unwrapType );
		}
	}

	protected int getOpenConnections() {
		return state.pool.allConnections.size() - state.pool.availableConnections.size();
	}

	protected void validateConnectionsReturned() {
		int allocationCount = getOpenConnections();
		if ( allocationCount != 0 ) {
			ConnectionInfoLogger.INSTANCE.error( "Connection leak detected: there are " + allocationCount + " unclosed connections");
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

	public static class PooledConnections {

		private final ConcurrentLinkedQueue<Connection> allConnections = new ConcurrentLinkedQueue<>();
		private final ConcurrentLinkedQueue<Connection> availableConnections = new ConcurrentLinkedQueue<>();

		private final ConnectionCreator connectionCreator;
		private final ConnectionValidator connectionValidator;
		private final boolean autoCommit;
		private final int minSize;
		private final int maxSize;

		private volatile boolean primed;

		private PooledConnections(
				Builder builder) {
			ConnectionInfoLogger.INSTANCE.debugf( "Initializing Connection pool with %s Connections", builder.initialSize );
			connectionCreator = builder.connectionCreator;
			connectionValidator = builder.connectionValidator == null
					? ConnectionValidator.ALWAYS_VALID
					: builder.connectionValidator;
			autoCommit = builder.autoCommit;
			maxSize = builder.maxSize;
			minSize = builder.minSize;
			addConnections( builder.initialSize );
		}

		private void validate() {
			final int size = size();

			if ( !primed && size >= minSize ) {
				// IMPL NOTE: the purpose of primed is to allow the pool to lazily reach its
				// defined min-size.
				ConnectionInfoLogger.INSTANCE.debug( "Connection pool now considered primed; min-size will be maintained" );
				primed = true;
			}

			if ( size < minSize && primed ) {
				int numberToBeAdded = minSize - size;
				ConnectionInfoLogger.INSTANCE.debugf( "Adding %s Connections to the pool", numberToBeAdded );
				addConnections( numberToBeAdded );
			}
			else if ( size > maxSize ) {
				int numberToBeRemoved = size - maxSize;
				ConnectionInfoLogger.INSTANCE.debugf( "Removing %s Connections from the pool", numberToBeRemoved );
				removeConnections( numberToBeRemoved );
			}
		}

		private void add(Connection conn) {
			final Connection connection = releaseConnection( conn );
			if ( connection != null ) {
				availableConnections.offer( connection );
			}
		}

		private Connection releaseConnection(Connection conn) {
			Exception t = null;
			try {
				conn.setAutoCommit( true );
				conn.clearWarnings();
				if ( connectionValidator.isValid( conn ) ) {
					return conn;
				}
			}
			catch (SQLException ex) {
				t = ex;
			}
			closeConnection( conn, t );
			ConnectionInfoLogger.INSTANCE.debug( "Connection release failed. Closing pooled connection", t );
			return null;
		}

		private Connection poll() {
			Connection conn;
			do {
				conn = availableConnections.poll();
				if ( conn == null ) {
					synchronized (allConnections) {
						if ( allConnections.size() < maxSize ) {
							addConnections( 1 );
							return poll();
						}
					}
					throw new HibernateException(
							"The internal connection pool has reached its maximum size and no connection is currently available" );
				}
				conn = prepareConnection( conn );
			} while ( conn == null );
			return conn;
		}

		protected Connection prepareConnection(Connection conn) {
			Exception t = null;
			try {
				conn.setAutoCommit( autoCommit );
				if ( connectionValidator.isValid( conn ) ) {
					return conn;
				}
			}
			catch (SQLException ex) {
				t = ex;
			}
			closeConnection( conn, t );
			ConnectionInfoLogger.INSTANCE.debug( "Connection preparation failed. Closing pooled connection", t );
			return null;
		}

		protected void closeConnection(Connection conn, Throwable t) {
			try {
				conn.close();
			}
			catch (SQLException ex) {
				ConnectionInfoLogger.INSTANCE.unableToClosePooledConnection( ex );
				if ( t != null ) {
					t.addSuppressed( ex );
				}
			}
			finally {
				if ( !allConnections.remove( conn ) ) {
					ConnectionInfoLogger.INSTANCE.debug( "Connection remove failed." );
				}
			}
		}

		public void close() throws SQLException {
			try {
				final int allocationCount = allConnections.size() - availableConnections.size();
				if (allocationCount > 0) {
					ConnectionInfoLogger.INSTANCE.error( "Connection leak detected: there are " + allocationCount + " unclosed connections upon shutting down pool " + getUrl());
				}
			}
			finally {
				removeConnections( Integer.MAX_VALUE );
			}
		}

		public int size() {
			return allConnections.size();
		}

		protected void removeConnections(int numberToBeRemoved) {
			for ( int i = 0; i < numberToBeRemoved; i++ ) {
				final Connection connection = availableConnections.poll();
				if ( connection == null ) {
					break;
				}
				closeConnection( connection, null );
			}
		}

		protected void addConnections(int numberOfConnections) {
			for ( int i = 0; i < numberOfConnections; i++ ) {
				Connection connection = connectionCreator.createConnection();
				allConnections.add( connection );
				availableConnections.add( connection );
			}
		}

		public String getUrl() {
			return connectionCreator.getUrl();
		}

		private static class Builder {
			private final ConnectionCreator connectionCreator;
			private ConnectionValidator connectionValidator;
			private final boolean autoCommit;
			private int initialSize = 1;
			private int minSize = 1;
			private int maxSize = 20;

			private Builder(ConnectionCreator connectionCreator, boolean autoCommit) {
				this.connectionCreator = connectionCreator;
				this.autoCommit = autoCommit;
			}

			private Builder initialSize(int initialSize) {
				this.initialSize = initialSize;
				return this;
			}

			private Builder minSize(int minSize) {
				this.minSize = minSize;
				return this;
			}

			private Builder maxSize(int maxSize) {
				this.maxSize = maxSize;
				return this;
			}

			private Builder validator(ConnectionValidator connectionValidator) {
				this.connectionValidator = connectionValidator;
				return this;
			}

			private PooledConnections build() {
				return new PooledConnections( this );
			}
		}
	}

	private static class PoolState implements Runnable {

		//Protecting any lifecycle state change:
		private final ReadWriteLock statelock = new ReentrantReadWriteLock();
		private volatile boolean active = false;
		private ScheduledExecutorService executorService;

		private final PooledConnections pool;
		private final long validationInterval;

		private PoolState(PooledConnections pool, long validationInterval) {
			this.pool = pool;
			this.validationInterval = validationInterval;
		}

		private void startIfNeeded() {
			if ( active ) {
				return;
			}
			statelock.writeLock().lock();
			try {
				if ( active ) {
					return;
				}
				executorService = Executors.newSingleThreadScheduledExecutor( new ValidationThreadFactory() );
				executorService.scheduleWithFixedDelay(
						this,
						validationInterval,
						validationInterval,
						TimeUnit.SECONDS
				);
				active = true;
			}
			finally {
				statelock.writeLock().unlock();
			}
		}

		@Override
		public void run() {
			if ( active ) {
				pool.validate();
			}
		}

		private void stop() {
			statelock.writeLock().lock();
			try {
				if ( !active ) {
					return;
				}
				ConnectionInfoLogger.INSTANCE.cleaningUpConnectionPool( pool.getUrl() );
				active = false;
				if ( executorService != null ) {
					executorService.shutdown();
				}
				executorService = null;
				try {
					pool.close();
				}
				catch (SQLException e) {
					ConnectionInfoLogger.INSTANCE.unableToDestroyConnectionPool( e );
				}
			}
			finally {
				statelock.writeLock().unlock();
			}
		}

		private Connection getConnection() {
			startIfNeeded();
			statelock.readLock().lock();
			try {
				return pool.poll();
			}
			finally {
				statelock.readLock().unlock();
			}
		}

		private void closeConnection(Connection conn) {
			if (conn == null) {
				return;
			}
			startIfNeeded();
			statelock.readLock().lock();
			try {
				pool.add( conn );
			}
			finally {
				statelock.readLock().unlock();
			}
		}

		private void validateConnections(ConnectionValidator validator) {
			if ( !active ) {
				return;
			}
			statelock.writeLock().lock();
			try {
				RuntimeException ex = null;
				for ( Connection connection : pool.allConnections ) {
					SQLException e = null;
					boolean isValid = false;
					try {
						isValid = validator.isValid( connection );
					}
					catch (SQLException sqlException) {
						e = sqlException;
					}
					if ( !isValid ) {
						pool.closeConnection( connection, e );
						if ( ex == null ) {
							ex = new RuntimeException( e );
						}
						else if ( e != null ) {
							ex.addSuppressed( e );
						}
					}
				}
				if ( ex != null ) {
					throw ex;
				}
			}
			finally {
				statelock.writeLock().unlock();
			}
		}
	}

	private static class ValidationThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable runnable) {
			final Thread thread = new Thread( runnable );
			thread.setDaemon( true );
			thread.setName( "Hibernate Connection Pool Validation Thread" );
			return thread;
		}
	}

}
