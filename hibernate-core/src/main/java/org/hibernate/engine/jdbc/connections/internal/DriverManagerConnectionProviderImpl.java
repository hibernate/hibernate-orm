/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.ConnectionPoolingLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

/**
 * A connection provider that uses the {@link java.sql.DriverManager} directly to open connections and provides
 * a very rudimentary connection pool.
 * <p/>
 * IMPL NOTE : not intended for production use!
 * <p/>
 * Thanks to Oleg Varaksin and his article on object pooling using the {@link java.util.concurrent} package, from
 * which much of the pooling code here is derived.  See http://ovaraksin.blogspot.com/2013/08/simple-and-lightweight-pool.html
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DriverManagerConnectionProviderImpl
		implements ConnectionProvider, Configurable, Stoppable, ServiceRegistryAwareService {

	private static final ConnectionPoolingLogger log = ConnectionPoolingLogger.CONNECTIONS_LOGGER;

	public static final String MIN_SIZE = "hibernate.connection.min_pool_size";
	public static final String INITIAL_SIZE = "hibernate.connection.initial_pool_size";
	// in TimeUnit.SECONDS
	public static final String VALIDATION_INTERVAL = "hibernate.connection.pool_validation_interval";

	private boolean active = true;

	private ScheduledExecutorService executorService;
	private PooledConnections pool;


	// create the pool ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private ServiceRegistryImplementor serviceRegistry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map configurationValues) {
		log.usingHibernateBuiltInConnectionPool();

		pool = buildPool( configurationValues );

		final long validationInterval = ConfigurationHelper.getLong( VALIDATION_INTERVAL, configurationValues, 30 );
		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleWithFixedDelay(
				new Runnable() {
					private boolean primed;
					@Override
					public void run() {
						pool.validate();
					}
				},
				validationInterval,
				validationInterval,
				TimeUnit.SECONDS
		);
	}

	private PooledConnections buildPool(Map configurationValues) {
		final boolean autoCommit = ConfigurationHelper.getBoolean(
				AvailableSettings.AUTOCOMMIT,
				configurationValues,
				false
		);
		final int minSize = ConfigurationHelper.getInt( MIN_SIZE, configurationValues, 1 );
		final int maxSize = ConfigurationHelper.getInt( AvailableSettings.POOL_SIZE, configurationValues, 20 );
		final int initialSize = ConfigurationHelper.getInt( INITIAL_SIZE, configurationValues, minSize );

		ConnectionCreator connectionCreator = buildCreator( configurationValues );
		PooledConnections.Builder pooledConnectionBuilder = new PooledConnections.Builder(
				connectionCreator,
				autoCommit
		);
		pooledConnectionBuilder.initialSize( initialSize );
		pooledConnectionBuilder.minSize( minSize );
		pooledConnectionBuilder.maxSize( maxSize );

		return pooledConnectionBuilder.build();
	}

	private ConnectionCreator buildCreator(Map configurationValues) {
		final ConnectionCreatorBuilder connectionCreatorBuilder = new ConnectionCreatorBuilder( serviceRegistry );

		final String driverClassName = (String) configurationValues.get( AvailableSettings.DRIVER );
		connectionCreatorBuilder.setDriver( loadDriverIfPossible( driverClassName ) );

		final String url = (String) configurationValues.get( AvailableSettings.URL );
		if ( url == null ) {
			final String msg = log.jdbcUrlNotSpecified( AvailableSettings.URL );
			log.error( msg );
			throw new HibernateException( msg );
		}
		connectionCreatorBuilder.setUrl( url );

		log.usingDriver( driverClassName, url );

		final Properties connectionProps = ConnectionProviderInitiator.getConnectionProperties( configurationValues );

		// if debug level is enabled, then log the password, otherwise mask it
		if ( log.isDebugEnabled() ) {
			log.connectionProperties( connectionProps );
		}
		else {
			log.connectionProperties( ConfigurationHelper.maskOut( connectionProps, "password" ) );
		}
		connectionCreatorBuilder.setConnectionProps( connectionProps );

		final boolean autoCommit = ConfigurationHelper.getBoolean( AvailableSettings.AUTOCOMMIT, configurationValues, false );
		log.autoCommitMode( autoCommit );
		connectionCreatorBuilder.setAutoCommit( autoCommit );

		final Integer isolation = ConnectionProviderInitiator.extractIsolation( configurationValues );
		if ( isolation != null ) {
			log.jdbcIsolationLevel( ConnectionProviderInitiator.toIsolationNiceName( isolation ) );
		}
		connectionCreatorBuilder.setIsolation( isolation );

		return connectionCreatorBuilder.build();
	}

	private Driver loadDriverIfPossible(String driverClassName) {
		if ( driverClassName == null ) {
			log.debug( "No driver class specified" );
			return null;
		}

		if ( serviceRegistry != null ) {
			final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
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


	// use the pool ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Connection getConnection() throws SQLException {
		if ( !active ) {
			throw new HibernateException( "Connection pool is no longer active" );
		}

		return pool.poll();
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		if (conn == null) {
			return;
		}

		pool.add( conn );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
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


	// destroy the pool ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void stop() {
		if ( !active ) {
			return;
		}

		log.cleaningUpConnectionPool( pool.getUrl() );

		active = false;

		if ( executorService != null ) {
			executorService.shutdown();
		}
		executorService = null;

		try {
			pool.close();
		}
		catch (SQLException e) {
			log.unableToClosePooledConnection( e );
		}
	}

	//CHECKSTYLE:START_ALLOW_FINALIZER
	@Override
	protected void finalize() throws Throwable {
		if ( active ) {
			stop();
		}
		super.finalize();
	}
	//CHECKSTYLE:END_ALLOW_FINALIZER

	public static class PooledConnections {

		private final ConcurrentLinkedQueue<Connection> allConnections = new ConcurrentLinkedQueue<Connection>();
		private final ConcurrentLinkedQueue<Connection> availableConnections = new ConcurrentLinkedQueue<Connection>();

		private static final CoreMessageLogger log = CoreLogging.messageLogger( DriverManagerConnectionProviderImpl.class );

		private final ConnectionCreator connectionCreator;
		private final boolean autoCommit;
		private final int minSize;
		private final int maxSize;

		private boolean primed;

		private PooledConnections(
				Builder builder) {
			log.debugf( "Initializing Connection pool with %s Connections", builder.initialSize );
			connectionCreator = builder.connectionCreator;
			autoCommit = builder.autoCommit;
			maxSize = builder.maxSize;
			minSize = builder.minSize;
			log.hibernateConnectionPoolSize( maxSize, minSize );
			addConnections( builder.initialSize );
		}

		public void validate() {
			final int size = size();

			if ( !primed && size >= minSize ) {
				// IMPL NOTE : the purpose of primed is to allow the pool to lazily reach its
				// defined min-size.
				log.debug( "Connection pool now considered primed; min-size will be maintained" );
				primed = true;
			}

			if ( size < minSize && primed ) {
				int numberToBeAdded = minSize - size;
				log.debugf( "Adding %s Connections to the pool", numberToBeAdded );
				addConnections( numberToBeAdded );
			}
			else if ( size > maxSize ) {
				int numberToBeRemoved = size - maxSize;
				log.debugf( "Removing %s Connections from the pool", numberToBeRemoved );
				removeConnections( numberToBeRemoved );
			}
		}

		public void add(Connection conn) throws SQLException {
			conn.setAutoCommit( true );
			conn.clearWarnings();
			availableConnections.offer( conn );
		}

		public Connection poll() throws SQLException {
			Connection conn = availableConnections.poll();
			if ( conn == null ) {
				synchronized (allConnections) {
					if(allConnections.size() < maxSize) {
						addConnections( 1 );
						return poll();
					}
				}
				throw new HibernateException( "The internal connection pool has reached its maximum size and no connection is currently available!" );
			}
			conn.setAutoCommit( autoCommit );
			return conn;
		}

		public void close() throws SQLException {
			try {
				int allocationCount = allConnections.size() - availableConnections.size();
				if(allocationCount > 0) {
					log.error( "Connection leak detected: there are " + allocationCount + " unclosed connections upon shutting down pool " + getUrl());
				}
			}
			finally {
				for ( Connection connection : allConnections ) {
					connection.close();
				}
			}
		}

		public int size() {
			return availableConnections.size();
		}

		protected void removeConnections(int numberToBeRemoved) {
			for ( int i = 0; i < numberToBeRemoved; i++ ) {
				Connection connection = availableConnections.poll();
				try {
					if ( connection != null ) {
						connection.close();
					}
					allConnections.remove( connection );
				}
				catch (SQLException e) {
					log.unableToCloseConnection( e );
				}
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

		public static class Builder {
			private final ConnectionCreator connectionCreator;
			private boolean autoCommit;
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

			public PooledConnections build() {
				return new PooledConnections( this );
			}
		}
	}
}
