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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.util.ReflectHelper;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * A connection provider that uses the {@link java.sql.DriverManager} directly to open connections and provides
 * a very rudimentary connection pool.
 * <p/>
 * IMPL NOTE : not intended for production use!
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DriverManagerConnectionProviderImpl implements ConnectionProvider, Configurable, Stoppable {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                DriverManagerConnectionProviderImpl.class.getPackage().getName());

	private String url;
	private Properties connectionProps;
	private Integer isolation;
	private int poolSize;
	private boolean autocommit;

	private final ArrayList<Connection> pool = new ArrayList<Connection>();
	private int checkedOut = 0;

	public void configure(Map configurationValues) {
        LOG.usingHibernateBuiltInConnectionPool();

		String driverClassName = (String) configurationValues.get( Environment.DRIVER );
        if (driverClassName == null) LOG.jdbcDriverNotSpecified(Environment.DRIVER);
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
        LOG.hibernateConnectionPoolSize(poolSize);

		autocommit = ConfigurationHelper.getBoolean( Environment.AUTOCOMMIT, configurationValues );
        LOG.autoCommitMode(autocommit);

		isolation = ConfigurationHelper.getInteger( Environment.ISOLATION, configurationValues );
        if (isolation != null) LOG.jdbcIsolationLevel(Environment.isolationLevelToString(isolation.intValue()));

		url = (String) configurationValues.get( Environment.URL );
		if ( url == null ) {
            String msg = LOG.jdbcUrlNotSpecified(Environment.URL);
            LOG.error(msg);
			throw new HibernateException( msg );
		}

		connectionProps = ConnectionProviderInitiator.getConnectionProperties( configurationValues );

        LOG.usingDriver(driverClassName, url);
		// if debug level is enabled, then log the password, otherwise mask it
        if (LOG.isDebugEnabled()) LOG.connectionProperties(connectionProps);
        else LOG.connectionProperties(ConfigurationHelper.maskOut(connectionProps, "password"));
	}

	public void stop() {
        LOG.cleaningUpConnectionPool(url);

		for ( Connection connection : pool ) {
			try {
				connection.close();
			}
			catch (SQLException sqle) {
                LOG.warn(LOG.unableToClosePooledConnection(), sqle);
			}
		}
		pool.clear();
	}

	public Connection getConnection() throws SQLException {
        LOG.totalCheckedOutConnection(checkedOut);

		// essentially, if we have available connections in the pool, use one...
		synchronized (pool) {
			if ( !pool.isEmpty() ) {
				int last = pool.size() - 1;
                if (LOG.isTraceEnabled()) {
                    LOG.usingPooledJdbcConnection(last);
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

        LOG.openingNewJdbcConnection();
		Connection conn = DriverManager.getConnection( url, connectionProps );
		if ( isolation != null ) {
			conn.setTransactionIsolation( isolation.intValue() );
		}
		if ( conn.getAutoCommit() != autocommit ) {
			conn.setAutoCommit(autocommit);
		}

        LOG.createdConnection(url, conn.getTransactionIsolation());

		checkedOut++;

		return conn;
	}

	public void closeConnection(Connection conn) throws SQLException {
		checkedOut--;

		// add to the pool if the max size is not yet reached.
		synchronized (pool) {
			int currentSize = pool.size();
			if ( currentSize < poolSize ) {
                LOG.returningConnectionToPool(currentSize + 1);
				pool.add(conn);
				return;
			}
		}

        LOG.closingJdbcConnection();
		conn.close();
	}

	@Override
    protected void finalize() {
		stop();
	}

	public boolean supportsAggressiveRelease() {
		return false;
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = INFO )
        @Message( value = "Autocommit mode: %s" )
        void autoCommitMode( boolean autocommit );

        @LogMessage( level = INFO )
        @Message( value = "Cleaning up connection pool [%s]" )
        void cleaningUpConnectionPool( String url );

        @LogMessage( level = DEBUG )
        @Message( value = "Closing JDBC connection" )
        void closingJdbcConnection();

        @LogMessage( level = INFO )
        @Message( value = "Connection properties: %s" )
        void connectionProperties( Properties connectionProps );

        @LogMessage( level = DEBUG )
        @Message( value = "Created connection to: %s, Isolation Level: %d" )
        void createdConnection( String url,
                                int transactionIsolation );

        @LogMessage( level = INFO )
        @Message( value = "Hibernate connection pool size: %d" )
        void hibernateConnectionPoolSize( int poolSize );

        @LogMessage( level = WARN )
        @Message( value = "no JDBC Driver class was specified by property %s" )
        void jdbcDriverNotSpecified( String driver );

        @LogMessage( level = INFO )
        @Message( value = "JDBC isolation level: %s" )
        void jdbcIsolationLevel( String isolationLevelToString );

        @Message( value = "JDBC URL was not specified by property %s" )
        String jdbcUrlNotSpecified( String url );

        @LogMessage( level = DEBUG )
        @Message( value = "Opening new JDBC connection" )
        void openingNewJdbcConnection();

        @LogMessage( level = TRACE )
        @Message( value = "Returning connection to pool, pool size: %d" )
        void returningConnectionToPool( int i );

        @LogMessage( level = TRACE )
        @Message( value = "Total checked-out connections: %d" )
        void totalCheckedOutConnection( int checkedOut );

        @Message( value = "Problem closing pooled connection" )
        Object unableToClosePooledConnection();

        @LogMessage( level = INFO )
        @Message( value = "using driver [%s] at URL [%s]" )
        void usingDriver( String driverClassName,
                          String url );

        @LogMessage( level = INFO )
        @Message( value = "Using Hibernate built-in connection pool (not for production use!)" )
        void usingHibernateBuiltInConnectionPool();

        @LogMessage( level = TRACE )
        @Message( value = "Using pooled JDBC connection, pool size: %d" )
        void usingPooledJdbcConnection( int last );
    }
}
