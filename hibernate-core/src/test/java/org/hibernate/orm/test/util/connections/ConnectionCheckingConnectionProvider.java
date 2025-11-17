/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util.connections;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;

/**
 * A {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider} implementation
 * meant to be configured via {@code AvailableSettings.CONNECTION_PROVIDER} to facilitate
 * testing of how many connections are being opened and closed.
 * This implementation is thread-safe, however state could be racy: make sure the scenarios
 * being tested have completed before assertions are verified.
 */
public final class ConnectionCheckingConnectionProvider extends UserSuppliedConnectionProviderImpl {

	private final DataSource dataSource = new BaseDataSource( Environment.getProperties() );

	/**
	 * Counts the "open" events. Does NOT hold the total number of open connections
	 * existing at a given time, just the amount of times a connection was opened.
	 */
	private final AtomicInteger connectionOpenEventCount = new AtomicInteger();

	//Using a Vector just to avoid synchronizing on a bag
	private final Vector<CheckedConnection> openedConnections = new Vector<>();

	@Override
	public Connection getConnection() throws SQLException {
		this.connectionOpenEventCount.incrementAndGet();
		final CheckedConnection opened = new CheckedConnection( dataSource.getConnection() );
		this.openedConnections.add( opened );
		return opened;
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connection.close();
		//cast is also meant to verify we're not being returned a different implementation
		CheckedConnection wrapper = (CheckedConnection) connection;
		boolean removed = this.openedConnections.remove( wrapper );
		if ( !removed ) {
			throw new IllegalStateException(
					"Closing a connection which wasn't registered in this ConnectionProviderDecorator" );
		}
	}

	/**
	 * Resets the counters to zero; it's useful to invoke this after Hibernate
	 * has booted to exclude connections being used during initialization.
	 * @throws IllegalStateException if any unclosed connection are being detected.
	 */
	public void clear() {
		this.connectionOpenEventCount.set( 0 );
		if ( !areAllConnectionClosed() ) {
			throw new IllegalStateException( "Resetting test helper while not all connections have been closed yet" );
		}
	}

	/**
	 * @return the count of connections which are currently open.
	 */
	public int getCurrentOpenConnections() {
		return this.openedConnections.size();
	}

	/**
	 * @return {@code true} iff all known connections that have been opened are now closed.
	 */
	public boolean areAllConnectionClosed() {
		return this.openedConnections.isEmpty();
	}

	/**
	 * @return This returns the count of connections that have been opened since
	 * construction, or since the last time method {@link #clear()} has
	 * been invoked. N.B. this count includes connections that have since been closed.
	 */
	public int getTotalOpenedConnectionCount() {
		return this.connectionOpenEventCount.get();
	}

	private static final class CheckedConnection extends ConnectionBaseDelegate {

		private final AtomicBoolean delegateWasClosed = new AtomicBoolean( false );

		private CheckedConnection(Connection delegate) {
			super( delegate );
		}

		/**
		 * Implementation Note: closing the connection by invoking this method directly will not
		 * unregister it from the #openedConnections vector above:
		 * this implies that there could be a mismatch, and we leverage this to spot connections
		 * that have been closed but not using the appropriate method:
		 * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider#closeConnection(Connection)}
		 * @throws SQLException on any exception during the close operation
		 */
		@Override
		public void close() throws SQLException {
			super.close();
			//Safeguard against closing multiple times:
			if ( !this.delegateWasClosed.compareAndSet( false, true ) ) {
				throw new IllegalStateException( "Was already closed?!" );
			}
		}

	}

}
