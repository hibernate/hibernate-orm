/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
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
import org.hibernate.Internal;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Database;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.internal.util.securitymanager.SystemSecurityManager;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.internal.log.ConnectionPoolingLogger.CONNECTIONS_LOGGER;
import static org.hibernate.internal.log.ConnectionPoolingLogger.CONNECTIONS_MESSAGE_LOGGER;

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

	// create the pool ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private volatile ServiceRegistryImplementor serviceRegistry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map<String, Object> configurationValues) {
		CONNECTIONS_MESSAGE_LOGGER.usingHibernateBuiltInConnectionPool();
		PooledConnections pool = buildPool( configurationValues, serviceRegistry );
		final long validationInterval = ConfigurationHelper.getLong( VALIDATION_INTERVAL, configurationValues, 30 );
		this.state = new PoolState( pool, validationInterval );
	}

	private PooledConnections buildPool(Map<String,Object> configurationValues, ServiceRegistryImplementor serviceRegistry) {
		final boolean autoCommit = ConfigurationHelper.getBoolean( AvailableSettings.AUTOCOMMIT, configurationValues );
		final int minSize = ConfigurationHelper.getInt( MIN_SIZE, configurationValues, 1 );
		final int maxSize = ConfigurationHelper.getInt( AvailableSettings.POOL_SIZE, configurationValues, 20 );
		final int initialSize = ConfigurationHelper.getInt( INITIAL_SIZE, configurationValues, minSize );

		ConnectionCreator connectionCreator = buildCreator( configurationValues, serviceRegistry );
		PooledConnections.Builder pooledConnectionBuilder = new PooledConnections.Builder(
				connectionCreator,
				autoCommit
		);
		pooledConnectionBuilder.initialSize( initialSize );
		pooledConnectionBuilder.minSize( minSize );
		pooledConnectionBuilder.maxSize( maxSize );
		pooledConnectionBuilder.validator( this );
		return pooledConnectionBuilder.build();
	}

	private static ConnectionCreator buildCreator(Map<String,Object> configurationValues, ServiceRegistryImplementor serviceRegistry) {
		final String url = (String) configurationValues.get( AvailableSettings.URL );

		String driverClassName = (String) configurationValues.get( AvailableSettings.DRIVER );
		boolean success = false;
		Driver driver = null;
		if ( driverClassName != null ) {
			driver = loadDriverIfPossible( driverClassName, serviceRegistry );
			success = true;
		}
		else if ( url != null ) {
			//try to guess the driver class from the JDBC URL
			for ( Database database: Database.values() ) {
				if ( database.matchesUrl( url ) ) {
					driverClassName = database.getDriverClassName( url );
					if ( driverClassName != null ) {
						try {
							loadDriverIfPossible(driverClassName, serviceRegistry);
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

		if ( success ) {
			CONNECTIONS_MESSAGE_LOGGER.loadedDriver( driverClassName );
		}
		else {
			//we're hoping that the driver is already loaded
			CONNECTIONS_MESSAGE_LOGGER.noDriver( AvailableSettings.DRIVER );
			StringBuilder list = new StringBuilder();
			Enumeration<Driver> drivers = DriverManager.getDrivers();
			while ( drivers.hasMoreElements() ) {
				if ( list.length() != 0) {
					list.append(", ");
				}
				list.append( drivers.nextElement().getClass().getName() );
			}
			CONNECTIONS_MESSAGE_LOGGER.loadedDrivers( list.toString() );
		}

		if ( url == null ) {
			final String msg = CONNECTIONS_MESSAGE_LOGGER.jdbcUrlNotSpecified( AvailableSettings.URL );
			CONNECTIONS_LOGGER.error( msg );
			throw new HibernateException( msg );
		}

		CONNECTIONS_MESSAGE_LOGGER.usingUrl( url );

		final Properties connectionProps = ConnectionProviderInitiator.getConnectionProperties( configurationValues );

		// if debug level is enabled, then log the password, otherwise mask it
		if ( CONNECTIONS_LOGGER.isDebugEnabled() ) {
			CONNECTIONS_MESSAGE_LOGGER.connectionProperties( connectionProps );
		}
		else {
			CONNECTIONS_MESSAGE_LOGGER.connectionProperties( ConfigurationHelper.maskOut( connectionProps, "password" ) );
		}

		final boolean autoCommit = ConfigurationHelper.getBoolean( AvailableSettings.AUTOCOMMIT, configurationValues );
		CONNECTIONS_MESSAGE_LOGGER.autoCommitMode( autoCommit );

		final Integer isolation = ConnectionProviderInitiator.extractIsolation( configurationValues );
		if ( isolation != null ) {
			CONNECTIONS_MESSAGE_LOGGER.jdbcIsolationLevel( ConnectionProviderInitiator.toIsolationNiceName( isolation ) );
		}

		final String initSql = (String) configurationValues.get( INIT_SQL );

		final Object connectionCreatorFactory = configurationValues.get( CONNECTION_CREATOR_FACTORY );
		ConnectionCreatorFactory factory = null;
		if ( connectionCreatorFactory instanceof ConnectionCreatorFactory ) {
			factory = (ConnectionCreatorFactory) connectionCreatorFactory;
		}
		else if ( connectionCreatorFactory != null ) {
			factory = loadConnectionCreatorFactory( connectionCreatorFactory.toString(), serviceRegistry );
		}
		if ( factory == null ) {
			factory = ConnectionCreatorFactoryImpl.INSTANCE;
		}
		return factory.create(
				driver,
				serviceRegistry,
				url,
				connectionProps,
				autoCommit,
				isolation,
				initSql,
				configurationValues
		);
	}

	private static Driver loadDriverIfPossible(String driverClassName, ServiceRegistryImplementor serviceRegistry) {
		if ( driverClassName == null ) {
			CONNECTIONS_LOGGER.debug( "No driver class specified" );
			return null;
		}

		if ( serviceRegistry != null ) {
			final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
			final Class<Driver> driverClass = classLoaderService.classForName( driverClassName );
			try {
				return driverClass.newInstance();
			}
			catch ( Exception e ) {
				throw new ServiceException( "Specified JDBC Driver " + driverClassName + " could not be loaded", e );
			}
		}

		try {
			return (Driver) Class.forName( driverClassName ).newInstance();
		}
		catch ( Exception e1 ) {
			throw new ServiceException( "Specified JDBC Driver " + driverClassName + " could not be loaded", e1 );
		}
	}

	private static ConnectionCreatorFactory loadConnectionCreatorFactory(String connectionCreatorFactoryClassName, ServiceRegistryImplementor serviceRegistry) {
		if ( connectionCreatorFactoryClassName == null ) {
			CONNECTIONS_LOGGER.debug( "No connection creator factory class specified" );
			return null;
		}

		if ( serviceRegistry != null ) {
			final ClassLoaderService classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
			final Class<ConnectionCreatorFactory> factoryClass =
					classLoaderService.classForName( connectionCreatorFactoryClassName );
			try {
				return factoryClass.newInstance();
			}
			catch ( Exception e ) {
				throw new ServiceException( "Specified ConnectionCreatorFactory " + connectionCreatorFactoryClassName + " could not be loaded", e );
			}
		}

		try {
			return (ConnectionCreatorFactory) Class.forName( connectionCreatorFactoryClassName ).newInstance();
		}
		catch ( Exception e1 ) {
			throw new ServiceException( "Specified ConnectionCreatorFactory " + connectionCreatorFactoryClassName + " could not be loaded", e1 );
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
			CONNECTIONS_MESSAGE_LOGGER.error( "Connection leak detected: there are " + allocationCount + " unclosed connections");
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

	//CHECKSTYLE:START_ALLOW_FINALIZER
	@Override
	protected void finalize() throws Throwable {
		if ( state != null ) {
			state.stop();
		}
		super.finalize();
	}
	//CHECKSTYLE:END_ALLOW_FINALIZER

	/**
	 * Exposed to facilitate testing only.
	 */
	public Properties getConnectionProperties() {
		BasicConnectionCreator connectionCreator = (BasicConnectionCreator) this.state.pool.connectionCreator;
		return connectionCreator.getConnectionProperties();
	}

	@Override
	public boolean isValid(Connection connection) throws SQLException {
		return true;
	}

	@Internal
	public void releasePooledConnections() {
		state.pool.releasePooledConnections();
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
			CONNECTIONS_LOGGER.debugf( "Initializing Connection pool with %s Connections", builder.initialSize );
			connectionCreator = builder.connectionCreator;
			connectionValidator = builder.connectionValidator == null
					? ConnectionValidator.ALWAYS_VALID
					: builder.connectionValidator;
			autoCommit = builder.autoCommit;
			maxSize = builder.maxSize;
			minSize = builder.minSize;
			CONNECTIONS_MESSAGE_LOGGER.hibernateConnectionPoolSize( maxSize, minSize );
			addConnections( builder.initialSize );
		}

		public void validate() {
			final int size = size();

			if ( !primed && size >= minSize ) {
				// IMPL NOTE : the purpose of primed is to allow the pool to lazily reach its
				// defined min-size.
				CONNECTIONS_LOGGER.debug( "Connection pool now considered primed; min-size will be maintained" );
				primed = true;
			}

			if ( size < minSize && primed ) {
				int numberToBeAdded = minSize - size;
				CONNECTIONS_LOGGER.debugf( "Adding %s Connections to the pool", numberToBeAdded );
				addConnections( numberToBeAdded );
			}
			else if ( size > maxSize ) {
				int numberToBeRemoved = size - maxSize;
				CONNECTIONS_LOGGER.debugf( "Removing %s Connections from the pool", numberToBeRemoved );
				removeConnections( numberToBeRemoved );
			}
		}

		public void add(Connection conn) throws SQLException {
			final Connection connection = releaseConnection( conn );
			if ( connection != null ) {
				availableConnections.offer( connection );
			}
		}

		protected Connection releaseConnection(Connection conn) {
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
			CONNECTIONS_MESSAGE_LOGGER.debug( "Connection release failed. Closing pooled connection", t );
			return null;
		}

		public Connection poll() throws SQLException {
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
			CONNECTIONS_MESSAGE_LOGGER.debug( "Connection preparation failed. Closing pooled connection", t );
			return null;
		}

		protected void closeConnection(Connection conn, Throwable t) {
			try {
				conn.close();
			}
			catch (SQLException ex) {
				CONNECTIONS_MESSAGE_LOGGER.unableToCloseConnection( ex );
				if ( t != null ) {
					t.addSuppressed( ex );
				}
			}
			finally {
				allConnections.remove( conn );
			}
		}

		public void close() throws SQLException {
			try {
				int allocationCount = allConnections.size() - availableConnections.size();
				if (allocationCount > 0) {
					CONNECTIONS_LOGGER.error( "Connection leak detected: there are " + allocationCount + " unclosed connections upon shutting down pool " + getUrl());
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

		@Internal
		public void releasePooledConnections() {
			for ( Connection connection : allConnections ) {
				closeConnection( connection, null );
			}
		}

		public static class Builder {
			private final ConnectionCreator connectionCreator;
			private ConnectionValidator connectionValidator;
			private final boolean autoCommit;
			private int initialSize = 1;
			private int minSize = 1;
			private int maxSize = 20;

			public Builder(ConnectionCreator connectionCreator, boolean autoCommit) {
				this.connectionCreator = connectionCreator;
				this.autoCommit = autoCommit;
			}

			public Builder initialSize(int initialSize) {
				this.initialSize = initialSize;
				return this;
			}

			public Builder minSize(int minSize) {
				this.minSize = minSize;
				return this;
			}

			public Builder maxSize(int maxSize) {
				this.maxSize = maxSize;
				return this;
			}

			public Builder validator(ConnectionValidator connectionValidator) {
				this.connectionValidator = connectionValidator;
				return this;
			}

			public PooledConnections build() {
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

		public PoolState(PooledConnections pool, long validationInterval) {
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

		public void stop() {
			statelock.writeLock().lock();
			try {
				if ( !active ) {
					return;
				}
				CONNECTIONS_MESSAGE_LOGGER.cleaningUpConnectionPool( pool.getUrl() );
				active = false;
				if ( executorService != null ) {
					PrivilegedAction delegateToPrivilegedAction =
							new PrivilegedAction() {

								@Override
								public Object run() {
									executorService.shutdown();
									return null;
								}
							};
					if ( SystemSecurityManager.isSecurityManagerEnabled() ) {
						AccessController.doPrivileged(
								delegateToPrivilegedAction );
					}
					else {
						delegateToPrivilegedAction.run();
					}
				}
				executorService = null;
				try {
					pool.close();
				}
				catch (SQLException e) {
					CONNECTIONS_MESSAGE_LOGGER.unableToClosePooledConnection( e );
				}
			}
			finally {
				statelock.writeLock().unlock();
			}
		}

		public Connection getConnection() throws SQLException {
			startIfNeeded();
			statelock.readLock().lock();
			try {
				return pool.poll();
			}
			finally {
				statelock.readLock().unlock();
			}
		}

		public void closeConnection(Connection conn) throws SQLException {
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

		public void validateConnections(ConnectionValidator validator) {
			if ( !active ) {
				return;
			}
			statelock.writeLock().lock();
			try {
				RuntimeException ex = null;
				for ( Iterator<Connection> iterator = pool.allConnections.iterator(); iterator.hasNext(); ) {
					final Connection connection = iterator.next();
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
			Thread thread = new Thread( runnable );
			thread.setDaemon( true );
			thread.setName( "Hibernate Connection Pool Validation Thread" );
			return thread;
		}
	}

}
