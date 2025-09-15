/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import org.hibernate.HibernateException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.hibernate.internal.log.ConnectionInfoLogger.CONNECTION_INFO_LOGGER;

class PooledConnections {

	// Thanks to Oleg Varaksin and his article on object pooling using the {@link java.util.concurrent}
	// package, from which the original pooling code here is was derived.
	// See http://ovaraksin.blogspot.com/2013/08/simple-and-lightweight-pool.html

	private final ConcurrentLinkedQueue<Connection> allConnections = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<Connection> availableConnections = new ConcurrentLinkedQueue<>();

	private final ConnectionCreator connectionCreator;
	private final ConnectionValidator connectionValidator;
	private final boolean autoCommit;
	private final int minSize;
	private final int maxSize;

	private volatile boolean primed;

	private PooledConnections(
			Builder builder) {
		CONNECTION_INFO_LOGGER.initializingConnectionPool( builder.initialSize );
		connectionCreator = builder.connectionCreator;
		connectionValidator = builder.connectionValidator == null
				? ConnectionValidator.ALWAYS_VALID
				: builder.connectionValidator;
		autoCommit = builder.autoCommit;
		maxSize = builder.maxSize;
		minSize = builder.minSize;
		addConnections( builder.initialSize );
	}

	void validate() {
		final int size = size();

		if ( !primed && size >= minSize ) {
			// IMPL NOTE: the purpose of primed is to allow the pool to lazily reach its
			// defined min-size.
			CONNECTION_INFO_LOGGER.connectionPoolPrimed();
			primed = true;
		}

		if ( size < minSize && primed ) {
			int numberToBeAdded = minSize - size;
			CONNECTION_INFO_LOGGER.addingConnectionsToPool( numberToBeAdded );
			addConnections( numberToBeAdded );
		}
		else if ( size > maxSize ) {
			int numberToBeRemoved = size - maxSize;
			CONNECTION_INFO_LOGGER.removingConnectionsFromPool( numberToBeRemoved );
			removeConnections( numberToBeRemoved );
		}
	}

	void add(Connection conn) {
		final Connection connection = releaseConnection( conn );
		if ( connection != null ) {
			availableConnections.offer( connection );
		}
	}

	private Connection releaseConnection(Connection conn) {
		Exception t = null;
		try {
			conn.setAutoCommit( true );
			conn.clearWarnings();
			if ( connectionValidator.isValid( conn ) ) {
				return conn;
			}
		}
		catch (SQLException ex) {
			t = ex;
		}
		closeConnection( conn, t );
		CONNECTION_INFO_LOGGER.connectionReleaseFailedClosingPooledConnection( t );
		return null;
	}

	Connection poll() {
		Connection conn;
		do {
			conn = availableConnections.poll();
			if ( conn == null ) {
				synchronized (allConnections) {
					if ( allConnections.size() < maxSize ) {
						addConnections( 1 );
						return poll();
					}
				}
				throw new HibernateException(
						"The internal connection pool has reached its maximum size and no connection is currently available" );
			}
			conn = prepareConnection( conn );
		}
		while ( conn == null );
		return conn;
	}

	protected Connection prepareConnection(Connection conn) {
		Exception t = null;
		try {
			conn.setAutoCommit( autoCommit );
			if ( connectionValidator.isValid( conn ) ) {
				return conn;
			}
		}
		catch (SQLException ex) {
			t = ex;
		}
		closeConnection( conn, t );
		CONNECTION_INFO_LOGGER.connectionPreparationFailedClosingPooledConnection( t );
		return null;
	}

	protected void closeConnection(Connection conn, Throwable t) {
		try {
			conn.close();
		}
		catch (SQLException ex) {
			CONNECTION_INFO_LOGGER.unableToClosePooledConnection( ex );
			if ( t != null ) {
				t.addSuppressed( ex );
			}
		}
		finally {
			if ( !allConnections.remove( conn ) ) {
				CONNECTION_INFO_LOGGER.connectionRemoveFailed();
			}
		}
	}

	public void close() throws SQLException {
		try {
			final int allocationCount = allConnections.size() - availableConnections.size();
			if ( allocationCount > 0 ) {
				CONNECTION_INFO_LOGGER.error(
						"Connection leak detected: there are " + allocationCount + " unclosed connections upon shutting down pool " + getUrl() );
			}
		}
		finally {
			removeConnections( Integer.MAX_VALUE );
		}
	}

	public int size() {
		return allConnections.size();
	}

	protected void removeConnections(int numberToBeRemoved) {
		for ( int i = 0; i < numberToBeRemoved; i++ ) {
			final Connection connection = availableConnections.poll();
			if ( connection == null ) {
				break;
			}
			closeConnection( connection, null );
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

	int getOpenConnectionCount() {
		return allConnections.size() - availableConnections.size();
	}

	public Iterable<Connection> getAllConnections() {
		return allConnections;
	}

	static class Builder {
		private final ConnectionCreator connectionCreator;
		private ConnectionValidator connectionValidator;
		private boolean autoCommit;
		private int initialSize = 1;
		private int minSize = 1;
		private int maxSize = 20;

		Builder(ConnectionCreator connectionCreator) {
			this.connectionCreator = connectionCreator;
		}

		Builder autoCommit(boolean autoCommit) {
			this.autoCommit = autoCommit;
			return this;
		}

		Builder initialSize(int initialSize) {
			this.initialSize = initialSize;
			return this;
		}

		Builder minSize(int minSize) {
			this.minSize = minSize;
			return this;
		}

		Builder maxSize(int maxSize) {
			this.maxSize = maxSize;
			return this;
		}

		Builder validator(ConnectionValidator connectionValidator) {
			this.connectionValidator = connectionValidator;
			return this;
		}

		PooledConnections build() {
			return new PooledConnections( this );
		}
	}
}
