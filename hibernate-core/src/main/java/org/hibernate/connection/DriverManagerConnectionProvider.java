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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.ReflectHelper;

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

	private static final Logger log = LoggerFactory.getLogger(DriverManagerConnectionProvider.class);

	public void configure(Properties props) throws HibernateException {

		String driverClass = props.getProperty(Environment.DRIVER);

		poolSize = PropertiesHelper.getInt(Environment.POOL_SIZE, props, 20); //default pool size 20
		log.info("Using Hibernate built-in connection pool (not for production use!)");
		log.info("Hibernate connection pool size: " + poolSize);
		
		autocommit = PropertiesHelper.getBoolean(Environment.AUTOCOMMIT, props);
		log.info("autocommit mode: " + autocommit);

		isolation = PropertiesHelper.getInteger(Environment.ISOLATION, props);
		if (isolation!=null)
		log.info( "JDBC isolation level: " + Environment.isolationLevelToString( isolation.intValue() ) );

		if (driverClass==null) {
			log.warn("no JDBC Driver class was specified by property " + Environment.DRIVER);
		}
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
					log.error( msg, e );
					throw new HibernateException(msg, e);
				}
			}
		}

		url = props.getProperty( Environment.URL );
		if ( url == null ) {
			String msg = "JDBC URL was not specified by property " + Environment.URL;
			log.error( msg );
			throw new HibernateException( msg );
		}

		connectionProps = ConnectionProviderFactory.getConnectionProperties( props );

		log.info( "using driver: " + driverClass + " at URL: " + url );
		// if debug level is enabled, then log the password, otherwise mask it
		if ( log.isDebugEnabled() ) {
			log.info( "connection properties: " + connectionProps );
		} 
		else if ( log.isInfoEnabled() ) {
			log.info( "connection properties: " + PropertiesHelper.maskOut(connectionProps, "password") );
		}

	}

	public Connection getConnection() throws SQLException {

		if ( log.isTraceEnabled() ) log.trace( "total checked-out connections: " + checkedOut );

		synchronized (pool) {
			if ( !pool.isEmpty() ) {
				int last = pool.size() - 1;
				if ( log.isTraceEnabled() ) {
					log.trace("using pooled JDBC connection, pool size: " + last);
					checkedOut++;
				}
				Connection pooled = (Connection) pool.remove(last);
				if (isolation!=null) pooled.setTransactionIsolation( isolation.intValue() );
				if ( pooled.getAutoCommit()!=autocommit ) pooled.setAutoCommit(autocommit);
				return pooled;
			}
		}

		log.debug("opening new JDBC connection");
		Connection conn = DriverManager.getConnection(url, connectionProps);
		if (isolation!=null) conn.setTransactionIsolation( isolation.intValue() );
		if ( conn.getAutoCommit()!=autocommit ) conn.setAutoCommit(autocommit);

		if ( log.isDebugEnabled() ) {
			log.debug( "created connection to: " + url + ", Isolation Level: " + conn.getTransactionIsolation() );
		}
		if ( log.isTraceEnabled() ) checkedOut++;

		return conn;
	}

	public void closeConnection(Connection conn) throws SQLException {

		if ( log.isDebugEnabled() ) checkedOut--;

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

	protected void finalize() {
		close();
	}

	public void close() {

		log.info("cleaning up connection pool: " + url);

		Iterator iter = pool.iterator();
		while ( iter.hasNext() ) {
			try {
				( (Connection) iter.next() ).close();
			}
			catch (SQLException sqle) {
				log.warn("problem closing pooled connection", sqle);
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

}







