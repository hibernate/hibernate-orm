/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * @author Andrea Boriero
 */
public class PooledConnections {

	private final ConcurrentLinkedQueue<Connection> allConnections = new ConcurrentLinkedQueue<Connection>();
	private final ConcurrentLinkedQueue<Connection> availableConnections = new ConcurrentLinkedQueue<Connection>();

	private static final CoreMessageLogger log = CoreLogging.messageLogger( DriverManagerConnectionProviderImpl.class );

	private final ConnectionCreator connectionCreator;
	private final boolean autoCommit;
	private final int minSize;
	private final int maxSize;

	private boolean primed;

	private PooledConnections(
			Builder builder) {
		log.debugf( "Initializing Connection pool with %s Connections", builder.initialSize );
		connectionCreator = builder.connectionCreator;
		autoCommit = builder.autoCommit;
		maxSize = builder.maxSize;
		minSize = builder.minSize;
		log.hibernateConnectionPoolSize( maxSize, minSize );
		addConnections( builder.initialSize );
	}

	public void validate() {
		final int size = size();

		if ( !primed && size >= minSize ) {
			// IMPL NOTE : the purpose of primed is to allow the pool to lazily reach its
			// defined min-size.
			log.debug( "Connection pool now considered primed; min-size will be maintained" );
			primed = true;
		}

		if ( size < minSize && primed ) {
			int numberToBeAdded = minSize - size;
			log.debugf( "Adding %s Connections to the pool", numberToBeAdded );
			addConnections( numberToBeAdded );
		}
		else if ( size > maxSize ) {
			int numberToBeRemoved = size - maxSize;
			log.debugf( "Removing %s Connections from the pool", numberToBeRemoved );
			removeConnections( numberToBeRemoved );
		}
	}

	public void add(Connection conn) throws SQLException {
		conn.setAutoCommit( true );
		conn.clearWarnings();
		availableConnections.offer( conn );
	}

	public Connection poll() throws SQLException {
		Connection conn = availableConnections.poll();
		if ( conn == null ) {
			synchronized (allConnections) {
				if(allConnections.size() < maxSize) {
					addConnections( 1 );
					return poll();
				}
			}
			throw new HibernateException( "The internal connection pool has reached its maximum size and no connection is currently available!" );
		}
		conn.setAutoCommit( autoCommit );
		return conn;
	}

	public void close() throws SQLException {
		try {
			int allocationCount = allConnections.size() - availableConnections.size();
			if(allocationCount > 0) {
				log.error( "Collection leak detected: there are " + allocationCount + " unclosed connections upon shutting down pool " + getUrl());
			}
		}
		finally {
			for ( Connection connection : allConnections ) {
				connection.close();
			}
		}
	}

	public int size() {
		return availableConnections.size();
	}

	protected void removeConnections(int numberToBeRemoved) {
		for ( int i = 0; i < numberToBeRemoved; i++ ) {
			Connection connection = availableConnections.poll();
			try {
				if ( connection != null ) {
					connection.close();
				}
				allConnections.remove( connection );
			}
			catch (SQLException e) {
				log.unableToCloseConnection( e );
			}
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

	public static class Builder {
		private final ConnectionCreator connectionCreator;
		private boolean autoCommit;
		private int initialSize = 1;
		private int minSize = 1;
		private int maxSize = 20;

		public Builder(ConnectionCreator connectionCreator, boolean autoCommit) {
			this.connectionCreator = connectionCreator;
			this.autoCommit = autoCommit;
		}

		public Builder initialSize(int initialSize) {
			this.initialSize = initialSize;
			return this;
		}

		public Builder minSize(int minSize) {
			this.minSize = minSize;
			return this;
		}

		public Builder maxSize(int maxSize) {
			this.maxSize = maxSize;
			return this;
		}

		public PooledConnections build() {
			return new PooledConnections( this );
		}
	}
}
