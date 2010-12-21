/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.connection;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.util.ReflectHelper;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * A connection provider that uses <tt>java.sql.DriverManager</tt>. This provider
 * also implements a very rudimentary connection pool.
 * @see ConnectionProvider
 * @author Gavin King
 */
public class DriverManagerConnectionProvider implements ConnectionProvider {

	private String url;
	private Properties connectionProps;
	private Integer isolation;
	private final ArrayList pool = new ArrayList();
	private int poolSize;
	private int checkedOut = 0;
	private boolean autocommit;

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                DriverManagerConnectionProvider.class.getPackage().getName());

	public void configure(Properties props) throws HibernateException {

		String driverClass = props.getProperty(Environment.DRIVER);

		poolSize = ConfigurationHelper.getInt(Environment.POOL_SIZE, props, 20); //default pool size 20
        LOG.usingHibernateConnectionPool();
        LOG.hibernateConnectionPoolSize(poolSize);

		autocommit = ConfigurationHelper.getBoolean(Environment.AUTOCOMMIT, props);
        LOG.autocommmitMode(autocommit);

		isolation = ConfigurationHelper.getInteger(Environment.ISOLATION, props);
		if (isolation!=null)
 LOG.jdbcIsolationLevel(Environment.isolationLevelToString(isolation.intValue()));

        if (driverClass == null) LOG.unspecifiedJdbcDriver(Environment.DRIVER);
		else {
			try {
				// trying via forName() first to be as close to DriverManager's semantics
				Class.forName(driverClass);
			}
			catch (ClassNotFoundException cnfe) {
				try {
					ReflectHelper.classForName(driverClass);
				}
				catch (ClassNotFoundException e) {
					String msg = "JDBC Driver class not found: " + driverClass;
                    LOG.jdbcDriverClassNotFound(msg, e.getMessage());
					throw new HibernateException(msg, e);
				}
			}
		}

		url = props.getProperty( Environment.URL );
		if ( url == null ) {
			String msg = "JDBC URL was not specified by property " + Environment.URL;
            LOG.unspecifiedJdbcUrl(msg);
			throw new HibernateException( msg );
		}

		connectionProps = ConnectionProviderFactory.getConnectionProperties( props );

        LOG.usingDriver(driverClass, url);
		// if debug level is enabled, then log the password, otherwise mask it
        if (LOG.isDebugEnabled()) LOG.connectionProperties(connectionProps);
        else LOG.connectionProperties(ConfigurationHelper.maskOut(connectionProps, "password"));
	}

	public Connection getConnection() throws SQLException {

        LOG.checkedOutConnections(checkedOut);

		synchronized (pool) {
			if ( !pool.isEmpty() ) {
				int last = pool.size() - 1;
                if (LOG.isTraceEnabled()) {
                    LOG.usingPooledJdbcConnection(last);
					checkedOut++;
				}
				Connection pooled = (Connection) pool.remove(last);
				if (isolation!=null) pooled.setTransactionIsolation( isolation.intValue() );
				if ( pooled.getAutoCommit()!=autocommit ) pooled.setAutoCommit(autocommit);
				return pooled;
			}
		}

        LOG.openingNewJdbcConnection();
		Connection conn = DriverManager.getConnection(url, connectionProps);
		if (isolation!=null) conn.setTransactionIsolation( isolation.intValue() );
		if ( conn.getAutoCommit()!=autocommit ) conn.setAutoCommit(autocommit);

        LOG.createdConnection(url, conn.getTransactionIsolation());
        if (LOG.isTraceEnabled()) checkedOut++;

		return conn;
	}

	public void closeConnection(Connection conn) throws SQLException {

        if (LOG.isDebugEnabled()) checkedOut--;

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
		close();
	}

	public void close() {

        LOG.cleaningConnectionPool(url);

		Iterator iter = pool.iterator();
		while ( iter.hasNext() ) {
			try {
				( (Connection) iter.next() ).close();
			}
			catch (SQLException sqle) {
                LOG.unableToClosePooledConnection(sqle.getMessage());
			}
		}
		pool.clear();

	}

	/**
	 * @see ConnectionProvider#supportsAggressiveRelease()
	 */
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
        void autocommmitMode( boolean autocommit );

        @LogMessage( level = TRACE )
        @Message( value = "Total checked-out connections: %d" )
        void checkedOutConnections( int checkedOut );

        @LogMessage( level = INFO )
        @Message( value = "Cleaning up connection pool: %s" )
        void cleaningConnectionPool( String url );

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
        @Message( value = "Hibernate connection pool size: %s" )
        void hibernateConnectionPoolSize( int poolSize );

        @LogMessage( level = ERROR )
        @Message( value = "%s\n%s" )
        void jdbcDriverClassNotFound( String message,
                                      String errorMessage );

        @LogMessage( level = INFO )
        @Message( value = "JDBC isolation level: %s" )
        void jdbcIsolationLevel( String isolationLevelToString );

        @LogMessage( level = DEBUG )
        @Message( value = "Opening new JDBC connection" )
        void openingNewJdbcConnection();

        @LogMessage( level = TRACE )
        @Message( value = "Returning connection to pool, pool size: %d" )
        void returningConnectionToPool( int i );

        @LogMessage( level = WARN )
        @Message( value = "Problem closing pooled connection\n%s" )
        void unableToClosePooledConnection( String message );

        @LogMessage( level = WARN )
        @Message( value = "No JDBC Driver class was specified by property %s" )
        void unspecifiedJdbcDriver( String driver );

        @LogMessage( level = ERROR )
        @Message( value = "%s" )
        void unspecifiedJdbcUrl( String message );

        @LogMessage( level = INFO )
        @Message( value = "Using driver: %s at URL: %s" )
        void usingDriver( String driverClass,
                          String url );

        @LogMessage( level = INFO )
        @Message( value = "Using Hibernate built-in connection pool (not for production use!)" )
        void usingHibernateConnectionPool();

        @LogMessage( level = TRACE )
        @Message( value = "Using pooled JDBC connection, pool size: %d" )
        void usingPooledJdbcConnection( int last );
    }
}







