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
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;
import org.jboss.logging.Logger;

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
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, DriverManagerConnectionProviderImpl.class.getName() );

	private String url;
	private Properties connectionProps;
	private Integer isolation;
	private int poolSize;
	private boolean autocommit;

	//Access guarded by synchronization on the pool instance
	private final ArrayList<Connection> pool = new ArrayList<Connection>();
	private final AtomicInteger checkedOut = new AtomicInteger();

	private boolean stopped;

	private transient ServiceRegistryImplementor serviceRegistry;
	
	private Driver driver;

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

	@Override
	public void configure(Map configurationValues) {
		LOG.usingHibernateBuiltInConnectionPool();

		final String driverClassName = (String) configurationValues.get( AvailableSettings.DRIVER );
		if ( driverClassName == null ) {
			LOG.jdbcDriverNotSpecified( AvailableSettings.DRIVER );
		}
		else if ( serviceRegistry != null ) {
			try {
				driver = (Driver) serviceRegistry.getService(
						ClassLoaderService.class ).classForName( driverClassName )
						.newInstance();
			}
			catch ( Exception e ) {
				throw new ClassLoadingException(
						"Specified JDBC Driver " + driverClassName
						+ " could not be loaded", e
				);
			}
		}
		// guard dog, mostly for making test pass
		else {
			try {
				// trying via forName() first to be as close to DriverManager's semantics
				driver = (Driver) Class.forName( driverClassName ).newInstance();
			}
			catch ( Exception e1 ) {
				try{
					driver = (Driver) ReflectHelper.classForName( driverClassName ).newInstance();
				}
				catch ( Exception e2 ) {
					throw new HibernateException( "Specified JDBC Driver " + driverClassName + " could not be loaded", e2 );
				}
			}
		}

		// default pool size 20
		poolSize = ConfigurationHelper.getInt( AvailableSettings.POOL_SIZE, configurationValues, 20 );
		LOG.hibernateConnectionPoolSize( poolSize );

		autocommit = ConfigurationHelper.getBoolean( AvailableSettings.AUTOCOMMIT, configurationValues );
		LOG.autoCommitMode( autocommit );

		isolation = ConfigurationHelper.getInteger( AvailableSettings.ISOLATION, configurationValues );
		if ( isolation != null ) {
			LOG.jdbcIsolationLevel( Environment.isolationLevelToString( isolation ) );
		}

		url = (String) configurationValues.get( AvailableSettings.URL );
		if ( url == null ) {
			final String msg = LOG.jdbcUrlNotSpecified( AvailableSettings.URL );
			LOG.error( msg );
			throw new HibernateException( msg );
		}

		connectionProps = ConnectionProviderInitiator.getConnectionProperties( configurationValues );

		LOG.usingDriver( driverClassName, url );
		// if debug level is enabled, then log the password, otherwise mask it
		if ( LOG.isDebugEnabled() ) {
			LOG.connectionProperties( connectionProps );
		}
		else {
			LOG.connectionProperties( ConfigurationHelper.maskOut( connectionProps, "password" ) );
		}
	}

	@Override
	public void stop() {
		LOG.cleaningUpConnectionPool( url );

		synchronized ( pool ) {
			for ( Connection connection : pool ) {
				try {
					connection.close();
				}
				catch (SQLException sqle) {
					LOG.unableToClosePooledConnection( sqle );
				}
			}
			pool.clear();
		}
		stopped = true;
	}

	@Override
	public Connection getConnection() throws SQLException {
		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( traceEnabled ) {
			LOG.tracev( "Total checked-out connections: {0}", checkedOut.intValue() );
		}

		// essentially, if we have available connections in the pool, use one...
		synchronized (pool) {
			if ( !pool.isEmpty() ) {
				final int last = pool.size() - 1;
				if ( traceEnabled ) {
					LOG.tracev( "Using pooled JDBC connection, pool size: {0}", last );
				}
				final Connection pooled = pool.remove( last );
				if ( isolation != null ) {
					pooled.setTransactionIsolation( isolation );
				}
				if ( pooled.getAutoCommit() != autocommit ) {
					pooled.setAutoCommit( autocommit );
				}
				checkedOut.incrementAndGet();
				return pooled;
			}
		}

		// otherwise we open a new connection...
		final boolean debugEnabled = LOG.isDebugEnabled();
		if ( debugEnabled ) {
			LOG.debug( "Opening new JDBC connection" );
		}
		
		final Connection conn;
		if ( driver != null ) {
			// If a Driver is available, completely circumvent
			// DriverManager#getConnection.  It attempts to double check
			// ClassLoaders before using a Driver.  This does not work well in
			// OSGi environments without wonky workarounds.
			conn = driver.connect( url, connectionProps );
		}
		else {
			// If no Driver, fall back on the original method.
			conn = DriverManager.getConnection( url, connectionProps );
		}
		
		if ( isolation != null ) {
			conn.setTransactionIsolation( isolation );
		}
		if ( conn.getAutoCommit() != autocommit ) {
			conn.setAutoCommit( autocommit );
		}

		if ( debugEnabled ) {
			LOG.debugf( "Created connection to: %s, Isolation Level: %s", url, conn.getTransactionIsolation() );
		}

		checkedOut.incrementAndGet();
		return conn;
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		checkedOut.decrementAndGet();

		final boolean traceEnabled = LOG.isTraceEnabled();
		// add to the pool if the max size is not yet reached.
		synchronized ( pool ) {
			final int currentSize = pool.size();
			if ( currentSize < poolSize ) {
				if ( traceEnabled ) {
					LOG.tracev( "Returning connection to pool, pool size: {0}", ( currentSize + 1 ) );
				}
				pool.add( conn );
				return;
			}
		}

		LOG.debug( "Closing JDBC connection" );
		conn.close();
	}

	@Override
	protected void finalize() throws Throwable {
		if ( !stopped ) {
			stop();
		}
		super.finalize();
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
}
