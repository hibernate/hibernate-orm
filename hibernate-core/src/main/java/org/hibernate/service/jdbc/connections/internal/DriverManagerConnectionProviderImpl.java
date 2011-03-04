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
package org.hibernate.service.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.UnknownUnwrapTypeException;
import org.hibernate.util.ReflectHelper;

/**
 * A connection provider that uses the {@link java.sql.DriverManager} directly to open connections and provides
 * a very rudimentary connection pool.
 * <p/>
 * IMPL NOTE : not intended for production use!
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnnecessaryUnboxing"})
public class DriverManagerConnectionProviderImpl implements ConnectionProvider, Configurable, Stoppable {
	private static final Logger log = LoggerFactory.getLogger( DriverManagerConnectionProviderImpl.class );

	private String url;
	private Properties connectionProps;
	private Integer isolation;
	private int poolSize;
	private boolean autocommit;

	private final ArrayList<Connection> pool = new ArrayList<Connection>();
	private int checkedOut = 0;

	private boolean stopped;

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

	public void configure(Map configurationValues) {
		log.info( "Using Hibernate built-in connection pool (not for production use!)" );

		String driverClassName = (String) configurationValues.get( Environment.DRIVER );
		if ( driverClassName == null ) {
			log.warn( "no JDBC Driver class was specified by property " + Environment.DRIVER );
		}
		else {
			try {
				// trying via forName() first to be as close to DriverManager's semantics
				Class.forName( driverClassName );
			}
			catch ( ClassNotFoundException cnfe ) {
				try {
					ReflectHelper.classForName( driverClassName );
				}
				catch ( ClassNotFoundException e ) {
					throw new HibernateException( "Specified JDBC Driver " + driverClassName + " class not found", e );
				}
			}
		}

		poolSize = ConfigurationHelper.getInt( Environment.POOL_SIZE, configurationValues, 20 ); // default pool size 20
		log.info( "Hibernate connection pool size: " + poolSize );

		autocommit = ConfigurationHelper.getBoolean( Environment.AUTOCOMMIT, configurationValues );
		log.info("autocommit mode: " + autocommit);

		isolation = ConfigurationHelper.getInteger( Environment.ISOLATION, configurationValues );
		if (isolation!=null)
		log.info( "JDBC isolation level: " + Environment.isolationLevelToString( isolation.intValue() ) );

		url = (String) configurationValues.get( Environment.URL );
		if ( url == null ) {
			String msg = "JDBC URL was not specified by property " + Environment.URL;
			log.error( msg );
			throw new HibernateException( msg );
		}

		connectionProps = ConnectionProviderInitiator.getConnectionProperties( configurationValues );

		log.info( "using driver [" + driverClassName + "] at URL [" + url + "]" );
		// if debug level is enabled, then log the password, otherwise mask it
		if ( log.isDebugEnabled() ) {
			log.info( "connection properties: " + connectionProps );
		}
		else if ( log.isInfoEnabled() ) {
			log.info( "connection properties: " + ConfigurationHelper.maskOut( connectionProps, "password" ) );
		}
	}

	public void stop() {
		log.info( "cleaning up connection pool [" + url + "]" );

		for ( Connection connection : pool ) {
			try {
				connection.close();
			}
			catch (SQLException sqle) {
				log.warn( "problem closing pooled connection", sqle );
			}
		}
		pool.clear();
		stopped = true;
	}

	public Connection getConnection() throws SQLException {
		log.trace( "total checked-out connections: " + checkedOut );

		// essentially, if we have available connections in the pool, use one...
		synchronized (pool) {
			if ( !pool.isEmpty() ) {
				int last = pool.size() - 1;
				if ( log.isTraceEnabled() ) {
					log.trace( "using pooled JDBC connection, pool size: " + last );
					checkedOut++;
				}
				Connection pooled = pool.remove(last);
				if ( isolation != null ) {
					pooled.setTransactionIsolation( isolation.intValue() );
				}
				if ( pooled.getAutoCommit() != autocommit ) {
					pooled.setAutoCommit( autocommit );
				}
				return pooled;
			}
		}

		// otherwise we open a new connection...

		log.debug( "opening new JDBC connection" );
		Connection conn = DriverManager.getConnection( url, connectionProps );
		if ( isolation != null ) {
			conn.setTransactionIsolation( isolation.intValue() );
		}
		if ( conn.getAutoCommit() != autocommit ) {
			conn.setAutoCommit(autocommit);
		}

		if ( log.isDebugEnabled() ) {
			log.debug( "created connection to: " + url + ", Isolation Level: " + conn.getTransactionIsolation() );
		}

		checkedOut++;

		return conn;
	}

	public void closeConnection(Connection conn) throws SQLException {
		checkedOut--;

		// add to the pool if the max size is not yet reached.
		synchronized (pool) {
			int currentSize = pool.size();
			if ( currentSize < poolSize ) {
				if ( log.isTraceEnabled() ) log.trace("returning connection to pool, pool size: " + (currentSize + 1) );
				pool.add(conn);
				return;
			}
		}

		log.debug("closing JDBC connection");
		conn.close();
	}

	protected void finalize() throws Throwable {
		if ( !stopped ) {
			stop();
		}
		super.finalize();
	}

	public boolean supportsAggressiveRelease() {
		return false;
	}
}
