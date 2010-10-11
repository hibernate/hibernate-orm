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
package org.hibernate.service.jdbc.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.service.jdbc.spi.JdbcResourceRegistry;
import org.hibernate.service.jdbc.spi.JdbcServices;
import org.hibernate.service.jdbc.spi.ConnectionObserver;
import org.hibernate.service.jdbc.spi.LogicalConnectionImplementor;

/**
 * LogicalConnectionImpl implementation
 *
 * @author Steve Ebersole
 */
public class LogicalConnectionImpl implements LogicalConnectionImplementor {
	private static final Logger log = LoggerFactory.getLogger( LogicalConnectionImpl.class );

	private transient Connection physicalConnection;
	private final ConnectionReleaseMode connectionReleaseMode;
	private final JdbcServices jdbcServices;
	private final JdbcResourceRegistry jdbcResourceRegistry;
	private final List<ConnectionObserver> observers = new ArrayList<ConnectionObserver>();
	private boolean releasesEnabled = true;

	private final boolean isUserSuppliedConnection;
	private boolean isClosed;

	public LogicalConnectionImpl(
	        Connection userSuppliedConnection,
	        ConnectionReleaseMode connectionReleaseMode,
	        JdbcServices jdbcServices) {
		if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT &&
				! jdbcServices.getConnectionProvider().supportsAggressiveRelease() ) {
			log.debug( "connection provider reports to not support aggressive release; overriding" );
			connectionReleaseMode = ConnectionReleaseMode.AFTER_TRANSACTION;
		}
		this.physicalConnection = userSuppliedConnection;
		this.connectionReleaseMode = connectionReleaseMode;
		this.jdbcServices = jdbcServices;
		this.jdbcResourceRegistry = new JdbcResourceRegistryImpl( jdbcServices.getSqlExceptionHelper() );

		this.isUserSuppliedConnection = ( userSuppliedConnection != null );
	}

	/**
	 * {@inheritDoc}
	 */
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	/**
	 * {@inheritDoc}
	 */
	public JdbcResourceRegistry getResourceRegistry() {
		return jdbcResourceRegistry;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addObserver(ConnectionObserver observer) {
		observers.add( observer );
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isOpen() {
		return !isClosed;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isPhysicallyConnected() {
		return physicalConnection != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Connection getConnection() throws HibernateException {
		if ( isClosed ) {
			throw new HibernateException( "Logical connection is closed" );
		}
		if ( physicalConnection == null ) {
			if ( isUserSuppliedConnection ) {
				// should never happen
				throw new HibernateException( "User-supplied connection was null" );
			}
			obtainConnection();
		}
		return physicalConnection;
	}

	/**
	 * {@inheritDoc}
	 */
	public Connection close() {
		log.trace( "closing logical connection" );
		Connection c = physicalConnection;
		if ( !isUserSuppliedConnection && physicalConnection != null ) {
			jdbcResourceRegistry.close();
			releaseConnection();
		}
		// not matter what
		physicalConnection = null;
		isClosed = true;
		for ( ConnectionObserver observer : observers ) {
			observer.logicalConnectionClosed();
		}
		log.trace( "logical connection closed" );
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	public ConnectionReleaseMode getConnectionReleaseMode() {
		return connectionReleaseMode;
	}

	public void afterStatementExecution() {
		log.trace( "starting after statement execution processing [{}]", connectionReleaseMode );
		if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT ) {
			if ( ! releasesEnabled ) {
				log.debug( "skipping aggressive release due to manual disabling" );
				return;
			}
			if ( jdbcResourceRegistry.hasRegisteredResources() ) {
				log.debug( "skipping aggressive release due to registered resources" );
				return;
			}
			releaseConnection();
		}
	}

	public void afterTransaction() {
		if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT ||
				connectionReleaseMode == ConnectionReleaseMode.AFTER_TRANSACTION ) {
			if ( jdbcResourceRegistry.hasRegisteredResources() ) {
				log.info( "forcing container resource cleanup on transaction completion" );
				jdbcResourceRegistry.releaseResources();
			}
			aggressiveRelease();
		}
	}

	public void disableReleases() {
		log.trace( "disabling releases" );
		releasesEnabled = false;
	}

	public void enableReleases() {
		log.trace( "(re)enabling releases" );
		releasesEnabled = true;
		afterStatementExecution();
	}

	/**
	 * Force aggresive release of the underlying connection.
	 */
	public void aggressiveRelease() {
		if ( isUserSuppliedConnection ) {
			log.debug( "cannot aggressively release user-supplied connection; skipping" );
		}
		else {
			log.debug( "aggressively releasing JDBC connection" );
			if ( physicalConnection != null ) {
				releaseConnection();
			}
		}
	}


	/**
	 * Pysically opens a JDBC Connection.
	 *
	 * @throws org.hibernate.JDBCException Indicates problem opening a connection
	 */
	private void obtainConnection() throws JDBCException {
		log.debug( "obtaining JDBC connection" );
		try {
			physicalConnection = getJdbcServices().getConnectionProvider().getConnection();
			for ( ConnectionObserver observer : observers ) {
				observer.physicalConnectionObtained( physicalConnection );
			}
			log.debug( "obtained JDBC connection" );
		}
		catch ( SQLException sqle) {
			throw getJdbcServices().getSqlExceptionHelper().convert( sqle, "Could not open connection" );
		}
	}

	/**
	 * Physically closes the JDBC Connection.
	 *
	 * @throws JDBCException Indicates problem closing a connection
	 */
	private void releaseConnection() throws JDBCException {
		log.debug( "releasing JDBC connection" );
		try {
			if ( !physicalConnection.isClosed() ) {
				getJdbcServices().getSqlExceptionHelper().logAndClearWarnings( physicalConnection );
			}
			for ( ConnectionObserver observer : observers ) {
				observer.physicalConnectionReleased();
			}
			getJdbcServices().getConnectionProvider().closeConnection( physicalConnection );
			physicalConnection = null;
			log.debug( "released JDBC connection" );
		}
		catch (SQLException sqle) {
			throw getJdbcServices().getSqlExceptionHelper().convert( sqle, "Could not close connection" );
		}
	}
}
