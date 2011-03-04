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
package org.hibernate.engine.jdbc.internal;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.internal.proxy.ProxyBuilder;
import org.hibernate.engine.jdbc.spi.ConnectionObserver;
import org.hibernate.engine.jdbc.spi.JdbcResourceRegistry;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.jdbc.spi.NonDurableConnectionObserver;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.util.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Standard Hibernate {@link org.hibernate.engine.jdbc.spi.LogicalConnection} implementation
 * <p/>
 * IMPL NOTE : Custom serialization handling!
 *
 * @author Steve Ebersole
 */
public class LogicalConnectionImpl implements LogicalConnectionImplementor {
	private static final Logger log = LoggerFactory.getLogger( LogicalConnectionImpl.class );

	private transient Connection physicalConnection;
	private transient Connection shareableConnectionProxy;

	private final transient ConnectionReleaseMode connectionReleaseMode;
	private final transient JdbcServices jdbcServices;
	private final transient JdbcResourceRegistry jdbcResourceRegistry;
	private final transient List<ConnectionObserver> observers;

	private boolean releasesEnabled = true;

	private final boolean isUserSuppliedConnection;

	private boolean isClosed;

	public LogicalConnectionImpl(
			Connection userSuppliedConnection,
			ConnectionReleaseMode connectionReleaseMode,
			JdbcServices jdbcServices) {
		this(
				connectionReleaseMode,
				jdbcServices,
				(userSuppliedConnection != null),
				false,
				new ArrayList<ConnectionObserver>()
		);
		this.physicalConnection = userSuppliedConnection;
	}

	private LogicalConnectionImpl(
			ConnectionReleaseMode connectionReleaseMode,
			JdbcServices jdbcServices,
			boolean isUserSuppliedConnection,
			boolean isClosed,
			List<ConnectionObserver> observers) {
		this.connectionReleaseMode = determineConnectionReleaseMode(
				jdbcServices, isUserSuppliedConnection, connectionReleaseMode
		);
		this.jdbcServices = jdbcServices;
		this.jdbcResourceRegistry = new JdbcResourceRegistryImpl( getJdbcServices().getSqlExceptionHelper() );
		this.observers = observers;

		this.isUserSuppliedConnection = isUserSuppliedConnection;
		this.isClosed = isClosed;
	}

	private static ConnectionReleaseMode determineConnectionReleaseMode(
			JdbcServices jdbcServices,
			boolean isUserSuppliedConnection,
			ConnectionReleaseMode connectionReleaseMode) {
		if ( isUserSuppliedConnection ) {
			return ConnectionReleaseMode.ON_CLOSE;
		}
		else if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT &&
				! jdbcServices.getConnectionProvider().supportsAggressiveRelease() ) {
			log.debug( "connection provider reports to not support aggressive release; overriding" );
			return ConnectionReleaseMode.AFTER_TRANSACTION;
		}
		else {
			return connectionReleaseMode;
		}
	}

	@Override
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	@Override
	public JdbcResourceRegistry getResourceRegistry() {
		return jdbcResourceRegistry;
	}

	@Override
	public void addObserver(ConnectionObserver observer) {
		observers.add( observer );
	}

	@Override
	public void removeObserver(ConnectionObserver connectionObserver) {
		observers.remove( connectionObserver );
	}

	@Override
	public boolean isOpen() {
		return !isClosed;
	}

	@Override
	public boolean isPhysicallyConnected() {
		return physicalConnection != null;
	}

	@Override
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

	@Override
	public Connection getShareableConnectionProxy() {
		if ( shareableConnectionProxy == null ) {
			shareableConnectionProxy = buildConnectionProxy();
		}
		return shareableConnectionProxy;
	}

	private Connection buildConnectionProxy() {
		return ProxyBuilder.buildConnection( this );
	}

	@Override
	public Connection getDistinctConnectionProxy() {
		return buildConnectionProxy();
	}

	/**
	 * {@inheritDoc}
	 */
	public Connection close() {
		log.trace( "closing logical connection" );
		Connection c = isUserSuppliedConnection ? physicalConnection : null;
		try {
			releaseProxies();
			jdbcResourceRegistry.close();
			if ( !isUserSuppliedConnection && physicalConnection != null ) {
				releaseConnection();
			}
			return c;
		}
		finally {
			// no matter what
			physicalConnection = null;
			isClosed = true;
			log.trace( "logical connection closed" );
			for ( ConnectionObserver observer : observers ) {
				observer.logicalConnectionClosed();
			}
			observers.clear();
		}			
	}

	private void releaseProxies() {
		if ( shareableConnectionProxy != null ) {
			try {
				shareableConnectionProxy.close();
			}
			catch (SQLException e) {
				log.debug( "Error releasing shared connection proxy", e );
			}
		}
		shareableConnectionProxy = null;
	}

	@Override
	public ConnectionReleaseMode getConnectionReleaseMode() {
		return connectionReleaseMode;
	}

	@Override
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

	@Override
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

	@Override
	public void disableReleases() {
		log.trace( "disabling releases" );
		releasesEnabled = false;
	}

	@Override
	public void enableReleases() {
		log.trace( "(re)enabling releases" );
		releasesEnabled = true;
		afterStatementExecution();
	}

	/**
	 * Force aggressive release of the underlying connection.
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
	 * Physically opens a JDBC Connection.
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
		if ( physicalConnection == null ) {
			return;
		}
		try {
			if ( ! physicalConnection.isClosed() ) {
				getJdbcServices().getSqlExceptionHelper().logAndClearWarnings( physicalConnection );
			}
			if ( !isUserSuppliedConnection ) {
				getJdbcServices().getConnectionProvider().closeConnection( physicalConnection );
			}
			log.debug( "released JDBC connection" );
		}
		catch (SQLException sqle) {
			throw getJdbcServices().getSqlExceptionHelper().convert( sqle, "Could not close connection" );
		}
		finally {
			physicalConnection = null;
		}
		log.debug( "released JDBC connection" );
		for ( ConnectionObserver observer : observers ) {
			observer.physicalConnectionReleased();
		}
		releaseNonDurableObservers();
	}

	private void releaseNonDurableObservers() {
		Iterator observers = this.observers.iterator();
		while ( observers.hasNext() ) {
			if ( NonDurableConnectionObserver.class.isInstance( observers.next() ) ) {
				observers.remove();
			}
		}
	}

	@Override
	public Connection manualDisconnect() {
		if ( isClosed ) {
			throw new IllegalStateException( "cannot manually disconnect because logical connection is already closed" );
		}
		releaseProxies();
		Connection c = physicalConnection;
		jdbcResourceRegistry.releaseResources();
		releaseConnection();
		return c;
	}

	@Override
	public void manualReconnect(Connection suppliedConnection) {
		if ( isClosed ) {
			throw new IllegalStateException( "cannot manually reconnect because logical connection is already closed" );
		}
		if ( !isUserSuppliedConnection ) {
			throw new IllegalStateException( "cannot manually reconnect unless Connection was originally supplied" );
		}
		else {
			if ( suppliedConnection == null ) {
				throw new IllegalArgumentException( "cannot reconnect a null user-supplied connection" );
			}
			else if ( suppliedConnection == physicalConnection ) {
				log.debug( "reconnecting the same connection that is already connected; should this connection have been disconnected?" );
			}
			else if ( physicalConnection != null ) {
				throw new IllegalArgumentException(
						"cannot reconnect to a new user-supplied connection because currently connected; must disconnect before reconnecting."
				);
			}
			physicalConnection = suppliedConnection;
			log.debug( "reconnected JDBC connection" );
		}
	}

	@Override
	public boolean isAutoCommit() {
		if ( !isOpen() || ! isPhysicallyConnected() ) {
			return true;
		}

		try {
			return getConnection().getAutoCommit();
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert( e, "could not inspect JDBC autocommit mode" );
		}
	}

	@Override
	public void notifyObserversStatementPrepared() {
		for ( ConnectionObserver observer : observers ) {
			observer.statementPrepared();
		}
	}

	@Override
	public boolean isReadyForSerialization() {
		return isUserSuppliedConnection
				? ! isPhysicallyConnected()
				: ! getResourceRegistry().hasRegisteredResources();
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeBoolean( isUserSuppliedConnection );
		oos.writeBoolean( isClosed );
		List<ConnectionObserver> durableConnectionObservers = new ArrayList<ConnectionObserver>();
		for ( ConnectionObserver observer : observers ) {
			if ( ! NonDurableConnectionObserver.class.isInstance( observer ) ) {
				durableConnectionObservers.add( observer );
			}
		}
		oos.writeInt( durableConnectionObservers.size() );
		for ( ConnectionObserver observer : durableConnectionObservers ) {
			oos.writeObject( observer );
		}
	}

	public static LogicalConnectionImpl deserialize(
			ObjectInputStream ois,
			TransactionContext transactionContext) throws IOException, ClassNotFoundException {
		boolean isUserSuppliedConnection = ois.readBoolean();
		boolean isClosed = ois.readBoolean();
		int observerCount = ois.readInt();
		List<ConnectionObserver> observers = CollectionHelper.arrayList( observerCount );
		for ( int i = 0; i < observerCount; i++ ) {
			observers.add( (ConnectionObserver) ois.readObject() );
		}
		return new LogicalConnectionImpl(
				transactionContext.getConnectionReleaseMode(),
				transactionContext.getTransactionEnvironment().getJdbcServices(),
				isUserSuppliedConnection,
				isClosed,
				observers
		);
 	}

}
