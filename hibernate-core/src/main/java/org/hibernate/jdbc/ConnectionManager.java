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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.util.JDBCExceptionReporter;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Encapsulates JDBC Connection management logic needed by Hibernate.
 * <p/>
 * The lifecycle is intended to span a logical series of interactions with the
 * database.  Internally, this means the the lifecycle of the Session.
 *
 * @author Steve Ebersole
 */
public class ConnectionManager implements Serializable {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                ConnectionManager.class.getPackage().getName());

	public static interface Callback {
		public void connectionOpened();
		public void connectionCleanedUp();
		public boolean isTransactionInProgress();
	}

	private transient SessionFactoryImplementor factory;
	private final Callback callback;

	private final ConnectionReleaseMode releaseMode;
	private transient Connection connection;
	private transient Connection borrowedConnection;

	private final boolean wasConnectionSupplied;
	private transient Batcher batcher;
	private transient Interceptor interceptor;
	private boolean isClosed;
	private transient boolean isFlushing;

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

		this.connection = connection;
		wasConnectionSupplied = ( connection != null );

		this.releaseMode = wasConnectionSupplied ? ConnectionReleaseMode.ON_CLOSE : releaseMode;
	}

	/**
	 * Private constructor used exclusively from custom serialization
	 */
	private ConnectionManager(
	        SessionFactoryImplementor factory,
	        Callback callback,
	        ConnectionReleaseMode releaseMode,
	        Interceptor interceptor,
	        boolean wasConnectionSupplied,
	        boolean isClosed) {
		this.factory = factory;
		this.callback = callback;

		this.interceptor = interceptor;
		this.batcher = factory.getSettings().getBatcherFactory().createBatcher( this, interceptor );

		this.wasConnectionSupplied = wasConnectionSupplied;
		this.isClosed = isClosed;
		this.releaseMode = wasConnectionSupplied ? ConnectionReleaseMode.ON_CLOSE : releaseMode;
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
	 * Was the connection being used here supplied by the user?
	 *
	 * @return True if the user supplied the JDBC connection; false otherwise
	 */
	public boolean isSuppliedConnection() {
		return wasConnectionSupplied;
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
		if ( isClosed ) {
			throw new HibernateException( "connection manager has been closed" );
		}
		if ( connection == null  ) {
			openConnection();
		}
		return connection;
	}

	public boolean hasBorrowedConnection() {
		// used from testsuite
		return borrowedConnection != null;
	}

	public Connection borrowConnection() {
		if ( isClosed ) {
			throw new HibernateException( "connection manager has been closed" );
		}
		if ( isSuppliedConnection() ) {
			return connection;
		}
		else {
			if ( borrowedConnection == null ) {
				borrowedConnection = BorrowedConnectionProxy.generateProxy( this );
			}
			return borrowedConnection;
		}
	}

	public void releaseBorrowedConnection() {
		if ( borrowedConnection != null ) {
			try {
				BorrowedConnectionProxy.renderUnuseable( borrowedConnection );
			}
			finally {
				borrowedConnection = null;
			}
		}
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
		return connection == null
			|| connection.isClosed()
			|| connection.getAutoCommit();
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
		if ( releaseMode == ConnectionReleaseMode.AFTER_STATEMENT ) {
			return true;
		}
		else if ( releaseMode == ConnectionReleaseMode.AFTER_TRANSACTION ) {
			boolean inAutoCommitState;
			try {
				inAutoCommitState = isAutoCommit()&& !callback.isTransactionInProgress();
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
		if ( releaseMode == ConnectionReleaseMode.AFTER_STATEMENT ) {
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
			return releaseMode == ConnectionReleaseMode.AFTER_TRANSACTION && inAutoCommitState;
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
		return wasConnectionSupplied ? connection != null : !isClosed;
	}

	/**
	 * To be called after execution of each JDBC statement.  Used to
	 * conditionally release the JDBC connection aggressively if
	 * the configured release mode indicates.
	 */
	public void afterStatement() {
		if ( isAggressiveRelease() ) {
            if (isFlushing) LOG.skippingAggressiveReleaseDueToFlushCycle();
            else if (batcher.hasOpenResources()) LOG.skippingAggressiveReleaseDueToOpenResources();
            else if (borrowedConnection != null) LOG.skippingAggressiveReleaseDueToBorrowedConnection();
            else aggressiveRelease();
		}
	}

	/**
	 * To be called after local transaction completion.  Used to conditionally
	 * release the JDBC connection aggressively if the configured release mode
	 * indicates.
	 */
	public void afterTransaction() {
        if (isAfterTransactionRelease()) aggressiveRelease();
		else if ( isAggressiveReleaseNoTransactionCheck() && batcher.hasOpenResources() ) {
            LOG.forcingBatcherResourceCleanup();
			batcher.closeStatements();
			aggressiveRelease();
        } else if (isOnCloseRelease()) LOG.transactionCompletedWithOnCloseConnectionReleaseMode();
		batcher.unsetTransactionTimeout();
	}

	private boolean isAfterTransactionRelease() {
		return releaseMode == ConnectionReleaseMode.AFTER_TRANSACTION;
	}

	private boolean isOnCloseRelease() {
		return releaseMode == ConnectionReleaseMode.ON_CLOSE;
	}

	/**
	 * To be called after Session completion.  Used to release the JDBC
	 * connection.
	 *
	 * @return The connection mantained here at time of close.  Null if
	 * there was no connection cached internally.
	 */
	public Connection close() {
		try {
			return cleanup();
		}
		finally {
			isClosed = true;
		}
	}

	/**
	 * Manually disconnect the underlying JDBC Connection.  The assumption here
	 * is that the manager will be reconnected at a later point in time.
	 *
	 * @return The connection mantained here at time of disconnect.  Null if
	 * there was no connection cached internally.
	 */
	public Connection manualDisconnect() {
		return cleanup();
	}

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at
	 * some point after manualDisconnect().
	 * <p/>
	 * This form is used for ConnectionProvider-supplied connections.
	 */
	public void manualReconnect() {
	}

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at
	 * some point after manualDisconnect().
	 * <p/>
	 * This form is used for user-supplied connections.
	 */
	public void manualReconnect(Connection suppliedConnection) {
		this.connection = suppliedConnection;
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
		releaseBorrowedConnection();

		if ( connection == null ) {
            LOG.connectionAlreadyNullInCleanup();
			return null;
		}

		try {
            LOG.performingCleanup();

			batcher.closeStatements();
			Connection c = null;
			if ( !wasConnectionSupplied ) {
				closeConnection();
			}
			else {
				c = connection;
			}
			connection = null;
			return c;
		}
		finally {
			callback.connectionCleanedUp();
		}
	}

	/**
	 * Performs actions required to perform an aggressive release of the
	 * JDBC Connection.
	 */
	private void aggressiveRelease() {
		if ( !wasConnectionSupplied ) {
            LOG.aggressivelyReleasingJdbcConnection();
            if (connection != null) closeConnection();
		}
	}

	/**
	 * Pysically opens a JDBC Connection.
	 *
	 * @throws HibernateException
	 */
	private void openConnection() throws HibernateException {
        if (connection != null) return;

		LOG.openingJdbcConnection();
		try {
			connection = factory.getConnectionProvider().getConnection();
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					factory.getSQLExceptionConverter(),
					sqle,
					"Cannot open connection"
				);
		}

		callback.connectionOpened(); // register synch; stats.connect()
	}

	/**
	 * Physically closes the JDBC Connection.
	 */
	private void closeConnection() {
        if (LOG.isDebugEnabled()) LOG.releasingJdbcConnection(batcher.openResourceStatsAsString());

		try {
			if ( !connection.isClosed() ) {
				JDBCExceptionReporter.logAndClearWarnings( connection );
			}
			factory.getConnectionProvider().closeConnection( connection );
			connection = null;
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					factory.getSQLExceptionConverter(),
					sqle,
					"Cannot release connection"
				);
		}
	}

	/**
	 * Callback to let us know that a flush is beginning.  We use this fact
	 * to temporarily circumvent aggressive connection releasing until after
	 * the flush cycle is complete {@link #flushEnding()}
	 */
	public void flushBeginning() {
        LOG.registeringFlushBegin();
		isFlushing = true;
	}

	/**
	 * Callback to let us know that a flush is ending.  We use this fact to
	 * stop circumventing aggressive releasing connections.
	 */
	public void flushEnding() {
        LOG.registeringFlushEnd();
		isFlushing = false;
		afterStatement();
	}

	public boolean isReadyForSerialization() {
		return wasConnectionSupplied ? connection == null : !batcher.hasOpenResources();
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

		this.batcher = factory.getSettings().getBatcherFactory().createBatcher( this, interceptor );
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeBoolean( wasConnectionSupplied );
		oos.writeBoolean( isClosed );
	}

	public static ConnectionManager deserialize(
			ObjectInputStream ois,
	        SessionFactoryImplementor factory,
	        Interceptor interceptor,
	        ConnectionReleaseMode connectionReleaseMode,
	        JDBCContext jdbcContext) throws IOException {
		return new ConnectionManager(
				factory,
		        jdbcContext,
		        connectionReleaseMode,
		        interceptor,
		        ois.readBoolean(),
		        ois.readBoolean()
		);
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = DEBUG )
        @Message( value = "Aggressively releasing JDBC connection" )
        void aggressivelyReleasingJdbcConnection();

        @LogMessage( level = TRACE )
        @Message( value = "Connection already null in cleanup : no action" )
        void connectionAlreadyNullInCleanup();

        @LogMessage( level = INFO )
        @Message( value = "Forcing batcher resource cleanup on transaction completion; forgot to close ScrollableResults/Iterator?" )
        void forcingBatcherResourceCleanup();

        @LogMessage( level = DEBUG )
        @Message( value = "Opening JDBC connection" )
        void openingJdbcConnection();

        @LogMessage( level = TRACE )
        @Message( value = "Performing cleanup" )
        void performingCleanup();

        @LogMessage( level = TRACE )
        @Message( value = "Registering flush begin" )
        void registeringFlushBegin();

        @LogMessage( level = TRACE )
        @Message( value = "Registering flush end" )
        void registeringFlushEnd();

        @LogMessage( level = DEBUG )
        @Message( value = "Releasing JDBC connection [%s]" )
        void releasingJdbcConnection( String openResourceStatsAsString );

        @LogMessage( level = DEBUG )
        @Message( value = "Skipping aggresive-release due to borrowed connection" )
        void skippingAggressiveReleaseDueToBorrowedConnection();

        @LogMessage( level = DEBUG )
        @Message( value = "Skipping aggressive-release due to flush cycle" )
        void skippingAggressiveReleaseDueToFlushCycle();

        @LogMessage( level = DEBUG )
        @Message( value = "Skipping aggresive-release due to open resources on batcher" )
        void skippingAggressiveReleaseDueToOpenResources();

        @LogMessage( level = DEBUG )
        @Message( value = "Transaction completed on session with on_close connection release mode; be sure to close the session to release JDBC resources!" )
        void transactionCompletedWithOnCloseConnectionReleaseMode();
    }
}
