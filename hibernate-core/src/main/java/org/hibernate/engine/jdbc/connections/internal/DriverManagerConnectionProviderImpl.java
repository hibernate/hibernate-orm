/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
import java.util.concurrent.TimeUnit;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
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
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DriverManagerConnectionProviderImpl
		implements ConnectionProvider, Configurable, Stoppable, ServiceRegistryAwareService {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( DriverManagerConnectionProviderImpl.class );

	// in TimeUnit.SECONDS
	public static final String VALIDATION_INTERVAL = "hibernate.connection.pool_validation_interval";

	private boolean active = true;

	private ConcurrentLinkedQueue<Connection> connections = new ConcurrentLinkedQueue<Connection>();
	private ConnectionCreator connectionCreator;
	private ScheduledExecutorService executorService;



	// create the pool ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private transient ServiceRegistryImplementor serviceRegistry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map configurationValues) {
		log.usingHibernateBuiltInConnectionPool();

		connectionCreator = buildCreator( configurationValues );

		// todo : consider adding a min-pool-size option too

		final int maxSize = ConfigurationHelper.getInt( AvailableSettings.POOL_SIZE, configurationValues, 20 );
		final long validationInterval = ConfigurationHelper.getLong( VALIDATION_INTERVAL, configurationValues, 30 );

		log.hibernateConnectionPoolSize( maxSize );

		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleWithFixedDelay(
				new Runnable() {
					@Override
					public void run() {
						int size = connections.size();
						if ( size > maxSize ) {
							int sizeToBeRemoved = size - maxSize;
							for ( int i = 0; i < sizeToBeRemoved; i++ ) {
								connections.poll();
							}
						}
					}
				},
				validationInterval,
				validationInterval,
				TimeUnit.SECONDS
		);
	}

	private ConnectionCreator buildCreator(Map configurationValues) {
		final ConnectionCreatorBuilder connectionCreatorBuilder = new ConnectionCreatorBuilder();

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

		final Boolean autoCommit = ConfigurationHelper.getBooleanWrapper( AvailableSettings.AUTOCOMMIT, configurationValues, null );
		if ( autoCommit != null ) {
			log.autoCommitMode( autoCommit );
		}
		connectionCreatorBuilder.setAutoCommit( autoCommit );

		final Integer isolation = ConfigurationHelper.getInteger( AvailableSettings.ISOLATION, configurationValues );
		if ( isolation != null ) {
			log.jdbcIsolationLevel( Environment.isolationLevelToString( isolation ) );
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
			try{
				return (Driver) ReflectHelper.classForName( driverClassName ).newInstance();
			}
			catch ( Exception e2 ) {
				throw new ServiceException( "Specified JDBC Driver " + driverClassName + " could not be loaded", e2 );
			}
		}
	}


	// use the pool ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Connection getConnection() throws SQLException {
		if ( !active ) {
			throw new HibernateException( "Connection pool is no longer active" );
		}

		Connection connection;
		if ( (connection = connections.poll()) == null ) {
			connection = connectionCreator.createConnection();
		}

		return connection;
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		if (conn == null) {
			return;
		}

		this.connections.offer( conn );
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

		log.cleaningUpConnectionPool( connectionCreator.getUrl() );

		active = false;

		if ( executorService != null ) {
			executorService.shutdown();
		}
		executorService = null;

		for ( Connection connection : connections ) {
			try {
				connection.close();
			}
			catch (SQLException e) {
				log.unableToClosePooledConnection( e );
			}
		}
	}


	@Override
	protected void finalize() throws Throwable {
		if ( active ) {
			stop();
		}
		super.finalize();
	}


	// ConnectionCreator ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected static interface ConnectionCreator {
		public String getUrl();
		public Connection createConnection() throws SQLException;
	}

	protected static class ConnectionCreatorBuilder {
		private Driver driver;

		private String url;
		private Properties connectionProps;

		private Boolean autoCommit;
		private Integer isolation;

		public void setDriver(Driver driver) {
			this.driver = driver;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public void setConnectionProps(Properties connectionProps) {
			this.connectionProps = connectionProps;
		}

		public void setAutoCommit(Boolean autoCommit) {
			this.autoCommit = autoCommit;
		}

		public void setIsolation(Integer isolation) {
			this.isolation = isolation;
		}

		public ConnectionCreator build() {
			if ( driver == null ) {
				return new DriverManagerConnectionCreator( url, connectionProps, autoCommit, isolation );
			}
			else {
				return new DriverConnectionCreator( driver, url, connectionProps, autoCommit, isolation );
			}
		}
	}

	protected abstract static class BasicConnectionCreator implements ConnectionCreator {
		private final String url;
		private final Properties connectionProps;

		private final Boolean autocommit;
		private final Integer isolation;

		public BasicConnectionCreator(
				String url,
				Properties connectionProps,
				Boolean autocommit,
				Integer isolation) {
			this.url = url;
			this.connectionProps = connectionProps;
			this.autocommit = autocommit;
			this.isolation = isolation;
		}

		@Override
		public String getUrl() {
			return url;
		}

		@Override
		public Connection createConnection() throws SQLException {
			final Connection conn = makeConnection( url, connectionProps );

			if ( isolation != null ) {
				conn.setTransactionIsolation( isolation );
			}

			if ( autocommit != null && conn.getAutoCommit() != autocommit ) {
				conn.setAutoCommit( autocommit );
			}

			return conn;
		}

		protected abstract Connection makeConnection(String url, Properties connectionProps) throws SQLException;
	}

	protected static class DriverConnectionCreator extends BasicConnectionCreator {
		private final Driver driver;

		public DriverConnectionCreator(
				Driver driver,
				String url,
				Properties connectionProps,
				Boolean autocommit,
				Integer isolation) {
			super( url, connectionProps, autocommit, isolation );
			this.driver = driver;
		}

		@Override
		protected Connection makeConnection(String url, Properties connectionProps) throws SQLException {
			return driver.connect( url, connectionProps );
		}
	}

	protected static class DriverManagerConnectionCreator extends BasicConnectionCreator {
		public DriverManagerConnectionCreator(
				String url,
				Properties connectionProps,
				Boolean autocommit,
				Integer isolation) {
			super( url, connectionProps, autocommit, isolation );
		}

		@Override
		protected Connection makeConnection(String url, Properties connectionProps) throws SQLException {
			return DriverManager.getConnection( url, connectionProps );
		}
	}

}
