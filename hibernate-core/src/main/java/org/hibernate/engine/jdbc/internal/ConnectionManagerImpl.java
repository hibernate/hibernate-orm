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
package org.hibernate.engine.jdbc.internal;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.hibernate.AssertionFailure;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.HibernateLogger;
import org.hibernate.Interceptor;
import org.hibernate.ScrollMode;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.spi.ConnectionManager;
import org.hibernate.engine.jdbc.spi.ConnectionObserver;
import org.hibernate.jdbc.Expectation;
import org.jboss.logging.Logger;

/**
 * Encapsulates JDBC Connection management logic needed by Hibernate.
 * <p/>
 * The lifecycle is intended to span a logical series of interactions with the
 * database.  Internally, this means the the lifecycle of the Session.
 *
 * @author Steve Ebersole
 */
public class ConnectionManagerImpl implements ConnectionManager {

    private static final HibernateLogger LOG = Logger.getMessageLogger(HibernateLogger.class, ConnectionManagerImpl.class.getName());

	public static interface Callback extends ConnectionObserver {
		public boolean isTransactionInProgress();
	}

	// TODO: check if it's ok to change the method names in Callback

	private transient Interceptor interceptor;

	private final Callback callback;
	private transient LogicalConnectionImpl logicalConnection;
	private transient StatementPreparer statementPreparer;
	private final transient BatchBuilder batchBuilder;
	private Batch batch;

	/**
	 * Constructs a ConnectionManager.
	 * <p/>
	 * This is the form used internally.
	 *
	 * @param callback An observer for internal state change.
	 * @param releaseMode The mode by which to release JDBC connections.
	 * @param suppliedConnection An externally supplied connection.
	 */
	public ConnectionManagerImpl(
	        SessionFactoryImplementor factory,
	        Callback callback,
	        ConnectionReleaseMode releaseMode,
	        Connection suppliedConnection,
	        Interceptor interceptor) {
		this( factory,
				callback,
				interceptor,
				new LogicalConnectionImpl(
						suppliedConnection,
						releaseMode,
						factory.getJdbcServices(),
						factory.getStatistics() != null ? factory.getStatisticsImplementor() : null
				)
		);
	}

	/**
	 * Private constructor used exclusively from custom serialization
	 */
	private ConnectionManagerImpl(
			SessionFactoryImplementor factory,
			Callback callback,
			Interceptor interceptor,
			LogicalConnectionImpl logicalConnection
	) {
		this.callback = callback;
		this.interceptor = interceptor;
		this.logicalConnection = logicalConnection;
		this.logicalConnection.addObserver( callback );
		this.statementPreparer = new StatementPreparer( logicalConnection, factory.getSettings() );
		this.batchBuilder = factory.getSettings().getBatchBuilder();
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
	@Override
	public Connection getConnection() throws HibernateException {
		return logicalConnection.getConnection();
	}

	@Override
	public boolean hasBorrowedConnection() {
		// used from testsuite
		return logicalConnection.hasBorrowedConnection();
	}

	public Connection borrowConnection() {
		return logicalConnection.borrowConnection();
	}

	@Override
	public void releaseBorrowedConnection() {
		logicalConnection.releaseBorrowedConnection();
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
		return logicalConnection == null ||
				! logicalConnection.isOpen() ||
				! logicalConnection.isPhysicallyConnected() ||
				logicalConnection.getConnection().getAutoCommit();
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
		if ( logicalConnection.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_STATEMENT ) {
			return true;
		}
		else if ( logicalConnection.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_TRANSACTION ) {
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
		if ( logicalConnection.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_STATEMENT ) {
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
			return logicalConnection.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_TRANSACTION && inAutoCommitState;
		}
	}

	/**
	 * Is this ConnectionManager instance "logically" connected.  Meaning
	 * do we either have a cached connection available or do we have the
	 * ability to obtain a connection on demand.
	 *
	 * @return True if logically connected; false otherwise.
	 */
	@Override
	public boolean isCurrentlyConnected() {
		return logicalConnection != null && logicalConnection.isLogicallyConnected();
	}

	/**
	 * To be called after execution of each JDBC statement.  Used to
	 * conditionally release the JDBC connection aggressively if
	 * the configured release mode indicates.
	 */
	@Override
	public void afterStatement() {
		if ( isAggressiveRelease() ) {
			logicalConnection.afterStatementExecution();
		}
	}

	/**
	 * To be called after local transaction completion.  Used to conditionally
	 * release the JDBC connection aggressively if the configured release mode
	 * indicates.
	 */
	public void afterTransaction() {
		if ( logicalConnection != null ) {
            if (isAfterTransactionRelease() || isAggressiveReleaseNoTransactionCheck()) logicalConnection.afterTransaction();
            // log a message about potential connection leaks
            else if (isOnCloseRelease()) LOG.debugf("Transaction completed on session with on_close connection release mode; be sure to close the session to release JDBC resources!");
		}
        if (statementPreparer != null) statementPreparer.unsetTransactionTimeout();
	}

	private boolean isAfterTransactionRelease() {
		return logicalConnection.getConnectionReleaseMode() == ConnectionReleaseMode.AFTER_TRANSACTION;
	}

	private boolean isOnCloseRelease() {
		return logicalConnection.getConnectionReleaseMode() == ConnectionReleaseMode.ON_CLOSE;
	}

	private boolean isLogicallyConnected() {
		return logicalConnection != null && logicalConnection.isOpen();
	}

	@Override
	public void setTransactionTimeout(int seconds) {
		statementPreparer.setTransactionTimeout( seconds );
	}

	/**
	 * To be called after Session completion.  Used to release the JDBC
	 * connection.
	 *
	 * @return The connection mantained here at time of close.  Null if
	 * there was no connection cached internally.
	 */
	@Override
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
	@Override
	public Connection manualDisconnect() {
		if ( ! isLogicallyConnected() ) {
			throw new IllegalStateException( "cannot manually disconnect because not logically connected." );
		}
		releaseBatch();
		return logicalConnection.manualDisconnect();
	}

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at
	 * some point after manualDisconnect().
	 * <p/>
	 * This form is used for ConnectionProvider-supplied connections.
	 */
	@Override
	public void manualReconnect() {
		manualReconnect( null );
	}

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at
	 * some point after manualDisconnect().
	 * <p/>
	 * This form is used for user-supplied connections.
	 */
	@Override
	public void manualReconnect(Connection suppliedConnection) {
		if ( ! isLogicallyConnected() ) {
			throw new IllegalStateException( "cannot manually disconnect because not logically connected." );
		}
		logicalConnection.reconnect( suppliedConnection );
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
		if ( logicalConnection == null ) {
            LOG.trace("Connection already null in cleanup : no action");
			return null;
		}
		try {
            LOG.trace("Performing cleanup");
			releaseBatch();
			statementPreparer.close();
			Connection c = logicalConnection.close();
			return c;
		}
		finally {
			batch = null;
			statementPreparer = null;
			logicalConnection = null;
		}
	}

	/**
	 * Callback to let us know that a flush is beginning.  We use this fact
	 * to temporarily circumvent aggressive connection releasing until after
	 * the flush cycle is complete {@link #flushEnding()}
	 */
	@Override
	public void flushBeginning() {
        LOG.trace("Registering flush begin");
		logicalConnection.disableReleases();
	}

	/**
	 * Callback to let us know that a flush is ending.  We use this fact to
	 * stop circumventing aggressive releasing connections.
	 */
	@Override
	public void flushEnding() {
        LOG.trace("Registering flush end");
		logicalConnection.enableReleases();
		afterStatement();
	}

	/**
	 * Get a non-batchable prepared statement to use for inserting / deleting / updating,
	 * using JDBC3 getGeneratedKeys ({@link java.sql.Connection#prepareStatement(String, int)}).
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()}, it will be
	 * released when the session is closed or disconnected.
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, final int autoGeneratedKeys)
			throws HibernateException {
		executeBatch();
		return statementPreparer.prepareStatement( getSQL( sql ), autoGeneratedKeys );
	}

	/**
	 * Get a non-batchable prepared statement to use for inserting / deleting / updating.
	 * using JDBC3 getGeneratedKeys ({@link java.sql.Connection#prepareStatement(String, String[])}).
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()}, it will be
	 * released when the session is closed or disconnected.
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, final String[] columnNames) {
		executeBatch();
		return statementPreparer.prepareStatement( getSQL( sql ), columnNames );
	}

	/**
	 * Get a non-batchable prepared statement to use for selecting. Does not
	 * result in execution of the current batch.
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()},
	 * it will be released when the session is closed or disconnected.
	 */
	@Override
	public PreparedStatement prepareSelectStatement(String sql) {
		return statementPreparer.prepareStatement( getSQL( sql ), false );
	}

	/**
	 * Get a non-batchable prepared statement to use for inserting / deleting / updating.
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()}, it will be
	 * released when the session is closed or disconnected.
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, final boolean isCallable) {
		executeBatch();
		return statementPreparer.prepareStatement( getSQL( sql ), isCallable );
	}

	/**
	 * Get a non-batchable callable statement to use for inserting / deleting / updating.
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()}, it will be
	 * released when the session is closed or disconnected.
	 */
	@Override
	public CallableStatement prepareCallableStatement(String sql) {
		executeBatch();
        LOG.trace("Preparing callable statement");
		return CallableStatement.class.cast( statementPreparer.prepareStatement( getSQL( sql ), true ) );
	}

	/**
	 * Get a batchable prepared statement to use for inserting / deleting / updating
	 * (might be called many times before a single call to <tt>executeBatch()</tt>).
	 * After setting parameters, call <tt>addToBatch</tt> - do not execute the
	 * statement explicitly.
	 * @see org.hibernate.engine.jdbc.batch.spi.Batch#addToBatch
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()}, it will be
	 * released when the session is closed or disconnected.
	 */
	@Override
	public PreparedStatement prepareBatchStatement(Object key, String sql, boolean isCallable) {
		if ( key == null ) {
			throw new IllegalArgumentException( "batch key must be non-null." );
		}
		String actualSQL = getSQL( sql );
		PreparedStatement batchUpdate = null;
		if ( batch != null ) {
			if ( key.equals( batch.getKey() ) ) {
				batchUpdate = batch.getBatchStatement( key, actualSQL );
			}
			else {
				batch.execute();
				batch = null;
			}
		}
		if ( batch == null ) {
			batch =  batchBuilder.buildBatch(
					key,
					logicalConnection.getJdbcServices().getSqlStatementLogger(),
					logicalConnection.getJdbcServices().getSqlExceptionHelper()
			);
		}
		if ( batchUpdate == null ) {
			batchUpdate = statementPreparer.prepareStatement( actualSQL, isCallable );
			batch.addBatchStatement( key, actualSQL, batchUpdate );
		}
		return batchUpdate;
	}

	/**
	 * Get a prepared statement for use in loading / querying. Does not
	 * result in execution of the current batch.
	 * <p/>
	 * If not explicitly closed via {@link java.sql.PreparedStatement#close()},
	 * it will be released when the session is closed or disconnected.
	 */
	@Override
	public PreparedStatement prepareQueryStatement(
			String sql,
			final boolean isScrollable,
			final ScrollMode scrollMode,
			final boolean isCallable) {
		PreparedStatement ps = (
				isScrollable ?
						statementPreparer.prepareScrollableQueryStatement(
								getSQL( sql ), scrollMode, isCallable
						) :
						statementPreparer.prepareQueryStatement(
								getSQL( sql ), isCallable
						)
		);
		logicalConnection.getResourceRegistry().registerLastQuery( ps );
		return ps;
	}

	/**
	 * Cancel the current query statement
	 */
	@Override
	public void cancelLastQuery() throws HibernateException {
		logicalConnection.getResourceRegistry().cancelLastQuery();
	}

	@Override
	public void addToBatch(Object batchKey, String sql, Expectation expectation) {
		batch.addToBatch( batchKey, sql, expectation );
	}

	@Override
	public void executeBatch() throws HibernateException {
		if ( batch != null ) {
			batch.execute();
			batch.release(); // needed?
		}
	}

	@Override
	public void abortBatch() {
		releaseBatch();
	}

	private void releaseBatch() {
		if ( batch != null ) {
			batch.release();
		}
	}

	private String getSQL(String sql) {
		sql = interceptor.onPrepareStatement( sql );
		if ( sql==null || sql.length() == 0 ) {
			throw new AssertionFailure( "Interceptor.onPrepareStatement() returned null or empty string." );
		}
		return sql;
	}

	public boolean isReadyForSerialization() {
		return logicalConnection == null ? true : logicalConnection.isReadyForSerialization();
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
		ois.defaultReadObject();
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		logicalConnection.serialize( oos );
	}

	public static ConnectionManagerImpl deserialize(
			ObjectInputStream ois,
	        SessionFactoryImplementor factory,
	        Interceptor interceptor,
	        ConnectionReleaseMode connectionReleaseMode,
	        Callback callback) throws IOException {
		return new ConnectionManagerImpl(
				factory,
		        callback,
				interceptor,
				LogicalConnectionImpl.deserialize(
						ois,
						factory.getJdbcServices(),
						factory.getStatistics() != null ? factory.getStatisticsImplementor() : null,
						connectionReleaseMode
				)
		);
	}
}
