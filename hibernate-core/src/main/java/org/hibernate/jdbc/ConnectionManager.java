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
package org.hibernate.jdbc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.jdbc.internal.LogicalConnectionImpl;
import org.hibernate.engine.jdbc.spi.ConnectionObserver;

/**
 * Encapsulates JDBC Connection management logic needed by Hibernate.
 * <p/>
 * The lifecycle is intended to span a logical series of interactions with the
 * database.  Internally, this means the the lifecycle of the Session.
 *
 * @author Steve Ebersole
 */
public class ConnectionManager implements Serializable {

	private static final Logger log = LoggerFactory.getLogger( ConnectionManager.class );

	// TODO: check if it's ok to change the method names in Callback
	public static interface Callback extends ConnectionObserver {
		public boolean isTransactionInProgress();
	}

	private transient SessionFactoryImplementor factory;
	private final Callback callback;

	private transient LogicalConnectionImpl connection;

	private transient Batcher batcher;
	private transient Interceptor interceptor;

	/**
	 * Constructs a ConnectionManager.
	 * <p/>
	 * This is the form used internally.
	 * 
	 * @param factory The SessionFactory.
	 * @param callback An observer for internal state change.
	 * @param releaseMode The mode by which to release JDBC connections.
	 * @param connection An externally supplied connection.
	 */ 
	public ConnectionManager(
	        SessionFactoryImplementor factory,
	        Callback callback,
	        ConnectionReleaseMode releaseMode,
	        Connection connection,
	        Interceptor interceptor) {
		this.factory = factory;
		this.callback = callback;

		this.interceptor = interceptor;
		this.batcher = factory.getSettings().getBatcherFactory().createBatcher( this, interceptor );

		setupConnection( connection, releaseMode );
	}

	/**
	 * Private constructor used exclusively from custom serialization
	 */
	private ConnectionManager(
			SessionFactoryImplementor factory,
			Callback callback,
			ConnectionReleaseMode releaseMode,
			Interceptor interceptor
	) {
		this.factory = factory;
		this.callback = callback;

		this.interceptor = interceptor;
		this.batcher = factory.getSettings().getBatcherFactory().createBatcher( this, interceptor );
	}

	private void setupConnection(Connection suppliedConnection,
								 ConnectionReleaseMode releaseMode
	) {
		connection =
				new LogicalConnectionImpl(
						suppliedConnection,
						releaseMode,
						factory.getJdbcServices()
				);
		connection.addObserver( callback );
	}

	/**
	 * The session factory.
	 *
	 * @return the session factory.
	 */
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	/**
	 * The batcher managed by this ConnectionManager.
	 *
	 * @return The batcher.
	 */
	public Batcher getBatcher() {
		return batcher;
	}

	/**
	 * Retrieves the connection currently managed by this ConnectionManager.
	 * <p/>
	 * Note, that we may need to obtain a connection to return here if a
	 * connection has either not yet been obtained (non-UserSuppliedConnectionProvider)
	 * or has previously been aggressively released (if supported in this environment).
	 *
	 * @return The current Connection.
	 *
	 * @throws HibernateException Indicates a connection is currently not
	 * available (we are currently manually disconnected).
	 */
	public Connection getConnection() throws HibernateException {
		return connection.getConnection();
	}

	public boolean hasBorrowedConnection() {
		// used from testsuite
		return connection.hasBorrowedConnection();
	}

	public Connection borrowConnection() {
		return connection.borrowConnection();
	}

	public void releaseBorrowedConnection() {
		connection.releaseBorrowedConnection();
	}

	/**
	 * Is the connection considered "auto-commit"?
	 *
	 * @return True if we either do not have a connection, or the connection
	 * really is in auto-commit mode.
	 *
	 * @throws SQLException Can be thrown by the Connection.isAutoCommit() check.
	 */
	public boolean isAutoCommit() throws SQLException {
		return connection == null ||
				! connection.isOpen() ||
				! connection.isPhysicallyConnected() ||
				connection.getConnection().getAutoCommit();
	}

	/**
	 * Will connections be released after each statement execution?
	 * <p/>
	 * Connections will be released after each statement if either:<ul>
	 * <li>the defined release-mode is {@link ConnectionReleaseMode#AFTER_STATEMENT}; or
	 * <li>the defined release-mode is {@link ConnectionReleaseMode#AFTER_TRANSACTION} but
	 * we are in auto-commit mode.
	 * <p/>
	 * release-mode = {@link ConnectionReleaseMode#ON_CLOSE} should [b]never[/b] release
	 * a connection.
	 *
	 * @return True if the connections will be released after each statement; false otherwise.
	 */
	public boolean isAggressiveRelease() {
		if ( connection.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_STATEMENT ) {
			return true;
		}
		else if ( connection.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_TRANSACTION ) {
			boolean inAutoCommitState;
			try {
				inAutoCommitState = isAutoCommit() && ! callback.isTransactionInProgress();
			}
			catch( SQLException e ) {
				// assume we are in an auto-commit state
				inAutoCommitState = true;
			}
			return inAutoCommitState;
		}
		return false;
	}

	/**
	 * Modified version of {@link #isAggressiveRelease} which does not force a
	 * transaction check.  This is solely used from our {@link #afterTransaction}
	 * callback, so no need to do the check; plus it seems to cause problems on
	 * websphere (god i love websphere ;)
	 * </p>
	 * It uses this information to decide if an aggressive release was skipped
	 * do to open resources, and if so forces a release.
	 *
	 * @return True if the connections will be released after each statement; false otherwise.
	 */
	private boolean isAggressiveReleaseNoTransactionCheck() {
		if ( connection.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_STATEMENT ) {
			return true;
		}
		else {
			boolean inAutoCommitState;
			try {
				inAutoCommitState = isAutoCommit();
			}
			catch( SQLException e ) {
				// assume we are in an auto-commit state
				inAutoCommitState = true;
			}
			return connection.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_TRANSACTION && inAutoCommitState;
		}
	}

	/**
	 * Is this ConnectionManager instance "logically" connected.  Meaning
	 * do we either have a cached connection available or do we have the
	 * ability to obtain a connection on demand.
	 *
	 * @return True if logically connected; false otherwise.
	 */
	public boolean isCurrentlyConnected() {
		if ( connection != null ) {
			if ( connection.isUserSuppliedConnection() ) {
				return connection.isPhysicallyConnected();
			}
			else {
				return connection.isOpen();
			}
		}
		else {
			return false;
		}
	}

	/**
	 * To be called after execution of each JDBC statement.  Used to
	 * conditionally release the JDBC connection aggressively if
	 * the configured release mode indicates.
	 */
	public void afterStatement() {
		if ( isAggressiveRelease() ) {
			if ( batcher.hasOpenResources() ) {
				log.debug( "skipping aggresive-release due to open resources on batcher" );
			}
			else {
				connection.afterStatementExecution();
			}
		}
	}

	/**
	 * To be called after local transaction completion.  Used to conditionally
	 * release the JDBC connection aggressively if the configured release mode
	 * indicates.
	 */
	public void afterTransaction() {
		if ( connection != null ) {
			if ( isAfterTransactionRelease() ) {
				connection.afterTransaction();
			}
			else if ( isAggressiveReleaseNoTransactionCheck() && batcher.hasOpenResources() ) {
				log.info( "forcing batcher resource cleanup on transaction completion; forgot to close ScrollableResults/Iterator?" );
				batcher.closeStatements();
				connection.afterTransaction();
			}
			else if ( isOnCloseRelease() ) {
				// log a message about potential connection leaks
				log.debug( "transaction completed on session with on_close connection release mode; be sure to close the session to release JDBC resources!" );
			}
		}
		batcher.unsetTransactionTimeout();
	}

	private boolean isAfterTransactionRelease() {
		return connection.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_TRANSACTION;
	}

	private boolean isOnCloseRelease() {
		return connection.getConnectionReleaseMode() == ConnectionReleaseMode.ON_CLOSE;
	}

	public boolean isLogicallyConnected() {
		return connection != null && connection.isOpen();
	}

	/**
	 * To be called after Session completion.  Used to release the JDBC
	 * connection.
	 *
	 * @return The connection mantained here at time of close.  Null if
	 * there was no connection cached internally.
	 */
	public Connection close() {
		return cleanup();
	}

	/**
	 * Manually disconnect the underlying JDBC Connection.  The assumption here
	 * is that the manager will be reconnected at a later point in time.
	 *
	 * @return The connection mantained here at time of disconnect.  Null if
	 * there was no connection cached internally.
	 */
	public Connection manualDisconnect() {
		if ( ! isLogicallyConnected() ) {
			throw new IllegalStateException( "cannot manually disconnect because not logically connected." );
		}
		batcher.closeStatements();
		return connection.manualDisconnect();
	}

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at
	 * some point after manualDisconnect().
	 * <p/>
	 * This form is used for ConnectionProvider-supplied connections.
	 */
	public void manualReconnect() {
		manualReconnect( null );
	}

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at
	 * some point after manualDisconnect().
	 * <p/>
	 * This form is used for user-supplied connections.
	 */
	public void manualReconnect(Connection suppliedConnection) {
		if ( ! isLogicallyConnected() ) {
			throw new IllegalStateException( "cannot manually disconnect because not logically connected." );
		}
		connection.reconnect( suppliedConnection );
	}

	/**
	 * Releases the Connection and cleans up any resources associated with
	 * that Connection.  This is intended for use:
	 * 1) at the end of the session
	 * 2) on a manual disconnect of the session
	 * 3) from afterTransaction(), in the case of skipped aggressive releasing
	 *
	 * @return The released connection.
	 * @throws HibernateException
	 */
	private Connection cleanup() throws HibernateException {
		if ( connection == null ) {
			log.trace( "connection already null in cleanup : no action");
			return null;
		}
		try {
			log.trace( "performing cleanup" );

			if ( isLogicallyConnected() ) {
				batcher.closeStatements();
			}
			Connection c = connection.close();
			return c;
		}
		finally {
			connection = null;
		}
	}

	/**
	 * Callback to let us know that a flush is beginning.  We use this fact
	 * to temporarily circumvent aggressive connection releasing until after
	 * the flush cycle is complete {@link #flushEnding()}
	 */
	public void flushBeginning() {
		log.trace( "registering flush begin" );
		connection.disableReleases();
	}

	/**
	 * Callback to let us know that a flush is ending.  We use this fact to
	 * stop circumventing aggressive releasing connections.
	 */
	public void flushEnding() {
		log.trace( "registering flush end" );
		connection.enableReleases();
		afterStatement();
	}

	public boolean isReadyForSerialization() {
		return connection == null ? true : ! batcher.hasOpenResources() && connection.isReadyForSerialization();
	}

	/**
	 * Used during serialization.
	 *
	 * @param oos The stream to which we are being written.
	 * @throws IOException Indicates an I/O error writing to the stream
	 */
	private void writeObject(ObjectOutputStream oos) throws IOException {
		if ( !isReadyForSerialization() ) {
			throw new IllegalStateException( "Cannot serialize a ConnectionManager while connected" );
		}

		oos.writeObject( factory );
		oos.writeObject( interceptor );
		oos.defaultWriteObject();
	}

	/**
	 * Used during deserialization.
	 *
	 * @param ois The stream from which we are being read.
	 * @throws IOException Indicates an I/O error reading the stream
	 * @throws ClassNotFoundException Indicates resource class resolution.
	 */
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		factory = (SessionFactoryImplementor) ois.readObject();
		interceptor = (Interceptor) ois.readObject();
		ois.defaultReadObject();
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		connection.serialize( oos );
	}

	public static ConnectionManager deserialize(
			ObjectInputStream ois,
	        SessionFactoryImplementor factory,
	        Interceptor interceptor,
	        ConnectionReleaseMode connectionReleaseMode,
	        Callback callback) throws IOException {
		ConnectionManager connectionManager = new ConnectionManager(
				factory,
		        callback,
		        connectionReleaseMode,
		        interceptor
		);
		connectionManager.connection =
				LogicalConnectionImpl.deserialize(
						ois,
						factory.getJdbcServices( ),
						connectionReleaseMode
				);
		connectionManager.connection.addObserver( callback );
		return connectionManager;
	}
}
