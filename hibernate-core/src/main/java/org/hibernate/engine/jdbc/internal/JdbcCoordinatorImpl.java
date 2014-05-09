/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.InvalidatableWrapper;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcWrapper;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionEnvironment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;

import org.jboss.logging.Logger;

/**
 * Standard Hibernate implementation of {@link JdbcCoordinator}
 * <p/>
 * IMPL NOTE : Custom serialization handling!
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 * @author Sanne Grinovero
 */
public class JdbcCoordinatorImpl implements JdbcCoordinator {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			JdbcCoordinatorImpl.class.getName()
	);

	private transient TransactionCoordinator transactionCoordinator;
	private final transient LogicalConnectionImpl logicalConnection;

	private transient Batch currentBatch;

	private transient long transactionTimeOutInstant = -1;

	/**
	 * This is a marker value to insert instead of null values for when a Statement gets registered in xref
	 * but has no associated ResultSets registered. This is useful to efficiently check against duplicate
	 * registration but you'll have to check against instance equality rather than null before attempting
	 * to add elements to this set.
	 */
	private static final Set<ResultSet> EMPTY_RESULTSET = Collections.emptySet();

	private final HashMap<Statement,Set<ResultSet>> xref = new HashMap<Statement,Set<ResultSet>>();
	private final Set<ResultSet> unassociatedResultSets = new HashSet<ResultSet>();
	private final transient SqlExceptionHelper exceptionHelper;

	private Statement lastQuery;

	/**
	 * If true, manually (and temporarily) circumvent aggressive release processing.
	 */
	private boolean releasesEnabled = true;

	/**
	 * Constructs a JdbcCoordinatorImpl
	 *
	 * @param userSuppliedConnection The user supplied connection (may be null)
	 * @param transactionCoordinator The transaction coordinator
	 */
	public JdbcCoordinatorImpl(
			Connection userSuppliedConnection,
			TransactionCoordinator transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
		this.logicalConnection = new LogicalConnectionImpl(
				userSuppliedConnection,
				transactionCoordinator.getTransactionContext().getConnectionReleaseMode(),
				transactionCoordinator.getTransactionContext().getTransactionEnvironment().getJdbcServices(),
				transactionCoordinator.getTransactionContext().getJdbcConnectionAccess()
		);
		this.exceptionHelper = logicalConnection.getJdbcServices().getSqlExceptionHelper();
	}

	/**
	 * Constructs a JdbcCoordinatorImpl
	 *
	 * @param logicalConnection The logical JDBC connection
	 * @param transactionCoordinator The transaction coordinator
	 */
	public JdbcCoordinatorImpl(
			LogicalConnectionImpl logicalConnection,
			TransactionCoordinator transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
		this.logicalConnection = logicalConnection;
		this.exceptionHelper = logicalConnection.getJdbcServices().getSqlExceptionHelper();
	}

	private JdbcCoordinatorImpl(LogicalConnectionImpl logicalConnection) {
		this.logicalConnection = logicalConnection;
		this.exceptionHelper = logicalConnection.getJdbcServices().getSqlExceptionHelper();
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return transactionCoordinator;
	}

	@Override
	public LogicalConnectionImplementor getLogicalConnection() {
		return logicalConnection;
	}

	protected TransactionEnvironment transactionEnvironment() {
		return getTransactionCoordinator().getTransactionContext().getTransactionEnvironment();
	}

	protected SessionFactoryImplementor sessionFactory() {
		return transactionEnvironment().getSessionFactory();
	}

	protected BatchBuilder batchBuilder() {
		return sessionFactory().getServiceRegistry().getService( BatchBuilder.class );
	}

	/**
	 * Access to the SqlExceptionHelper
	 *
	 * @return The SqlExceptionHelper
	 */
	public SqlExceptionHelper sqlExceptionHelper() {
		return exceptionHelper;
	}


	private int flushDepth;

	@Override
	public void flushBeginning() {
		if ( flushDepth == 0 ) {
			releasesEnabled = false;
		}
		flushDepth++;
	}

	@Override
	public void flushEnding() {
		flushDepth--;
		if ( flushDepth < 0 ) {
			throw new HibernateException( "Mismatched flush handling" );
		}
		if ( flushDepth == 0 ) {
			releasesEnabled = true;
		}
		
		afterStatementExecution();
	}

	@Override
	public Connection close() {
		LOG.tracev( "Closing JDBC container [{0}]", this );
		if ( currentBatch != null ) {
			LOG.closingUnreleasedBatch();
			currentBatch.release();
		}
		cleanup();
		return logicalConnection.close();
	}

	@Override
	public Batch getBatch(BatchKey key) {
		if ( currentBatch != null ) {
			if ( currentBatch.getKey().equals( key ) ) {
				return currentBatch;
			}
			else {
				currentBatch.execute();
				currentBatch.release();
			}
		}
		currentBatch = batchBuilder().buildBatch( key, this );
		return currentBatch;
	}

	@Override
	public void executeBatch() {
		if ( currentBatch != null ) {
			currentBatch.execute();
			// needed?
			currentBatch.release();
		}
	}

	@Override
	public void abortBatch() {
		if ( currentBatch != null ) {
			currentBatch.release();
		}
	}

	private transient StatementPreparer statementPreparer;

	@Override
	public StatementPreparer getStatementPreparer() {
		if ( statementPreparer == null ) {
			statementPreparer = new StatementPreparerImpl( this );
		}
		return statementPreparer;
	}

	private transient ResultSetReturn resultSetExtractor;

	@Override
	public ResultSetReturn getResultSetReturn() {
		if ( resultSetExtractor == null ) {
			resultSetExtractor = new ResultSetReturnImpl( this );
		}
		return resultSetExtractor;
	}

	@Override
	public void setTransactionTimeOut(int seconds) {
		transactionTimeOutInstant = System.currentTimeMillis() + ( seconds * 1000 );
	}

	@Override
	public int determineRemainingTransactionTimeOutPeriod() {
		if ( transactionTimeOutInstant < 0 ) {
			return -1;
		}
		final int secondsRemaining = (int) ((transactionTimeOutInstant - System.currentTimeMillis()) / 1000);
		if ( secondsRemaining <= 0 ) {
			throw new TransactionException( "transaction timeout expired" );
		}
		return secondsRemaining;
	}

	@Override
	public void afterStatementExecution() {
		LOG.tracev( "Starting after statement execution processing [{0}]", connectionReleaseMode() );
		if ( connectionReleaseMode() == ConnectionReleaseMode.AFTER_STATEMENT ) {
			if ( ! releasesEnabled ) {
				LOG.debug( "Skipping aggressive release due to manual disabling" );
				return;
			}
			if ( hasRegisteredResources() ) {
				LOG.debug( "Skipping aggressive release due to registered resources" );
				return;
			}
			getLogicalConnection().releaseConnection();
		}
	}

	@Override
	public void afterTransaction() {
		transactionTimeOutInstant = -1;
		if ( connectionReleaseMode() == ConnectionReleaseMode.AFTER_STATEMENT ||
				connectionReleaseMode() == ConnectionReleaseMode.AFTER_TRANSACTION ) {
			if ( hasRegisteredResources() ) {
				LOG.forcingContainerResourceCleanup();
				releaseResources();
			}
			getLogicalConnection().aggressiveRelease();
		}
	}
	
	private ConnectionReleaseMode connectionReleaseMode() {
		return getLogicalConnection().getConnectionReleaseMode();
	}

	@Override
	public <T> T coordinateWork(WorkExecutorVisitable<T> work) {
		final Connection connection = getLogicalConnection().getConnection();
		try {
			final T result = work.accept( new WorkExecutor<T>(), connection );
			afterStatementExecution();
			return result;
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "error executing work" );
		}
	}

	@Override
	public boolean isReadyForSerialization() {
		return getLogicalConnection().isUserSuppliedConnection()
				? ! getLogicalConnection().isPhysicallyConnected()
				: ! hasRegisteredResources();
	}

	/**
	 * JDK serialization hook
	 *
	 * @param oos The stream into which to write our state
	 *
	 * @throws IOException Trouble accessing the stream
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		if ( ! isReadyForSerialization() ) {
			throw new HibernateException( "Cannot serialize Session while connected" );
		}
		logicalConnection.serialize( oos );
	}

	/**
	 * JDK deserialization hook
	 *
	 * @param ois The stream into which to write our state
	 * @param transactionContext The transaction context which owns the JdbcCoordinatorImpl to be deserialized.
	 *
	 * @return The deserialized JdbcCoordinatorImpl
	 *
	 * @throws IOException Trouble accessing the stream
	 * @throws ClassNotFoundException Trouble reading the stream
	 */
	public static JdbcCoordinatorImpl deserialize(
			ObjectInputStream ois,
			TransactionContext transactionContext) throws IOException, ClassNotFoundException {
		return new JdbcCoordinatorImpl( LogicalConnectionImpl.deserialize( ois, transactionContext ) );
	}

	/**
	 * Callback after deserialization from Session is done
	 *
	 * @param transactionCoordinator The transaction coordinator
	 */
	public void afterDeserialize(TransactionCoordinatorImpl transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
	}

	@Override
	public void register(Statement statement) {
		LOG.tracev( "Registering statement [{0}]", statement );
		// Benchmarking has shown this to be a big hotspot.  Originally, most usages would call both
		// #containsKey and #put.  Instead, we optimize for the most common path (no previous Statement was
		// registered) by calling #put only once, but still handling the unlikely conflict and resulting exception.
		final Set<ResultSet> previousValue = xref.put( statement, EMPTY_RESULTSET );
		if ( previousValue != null ) {
			// Put the previous value back to undo the put
			xref.put( statement, previousValue );
			throw new HibernateException( "statement already registered with JDBCContainer" );
		}
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public void registerLastQuery(Statement statement) {
		LOG.tracev( "Registering last query statement [{0}]", statement );
		if ( statement instanceof JdbcWrapper ) {
			final JdbcWrapper<Statement> wrapper = (JdbcWrapper<Statement>) statement;
			registerLastQuery( wrapper.getWrappedObject() );
			return;
		}
		lastQuery = statement;
	}

	@Override
	public void cancelLastQuery() {
		try {
			if (lastQuery != null) {
				lastQuery.cancel();
			}
		}
		catch (SQLException sqle) {
			throw exceptionHelper.convert( sqle, "Cannot cancel query" );
		}
		finally {
			lastQuery = null;
		}
	}

	@Override
	public void release(Statement statement) {
		LOG.tracev( "Releasing statement [{0}]", statement );
		final Set<ResultSet> resultSets = xref.get( statement );
		if ( resultSets != null ) {
			for ( ResultSet resultSet : resultSets ) {
				close( resultSet );
			}
			resultSets.clear();
		}
		xref.remove( statement );
		close( statement );
		
		afterStatementExecution();
	}

	@Override
	public void register(ResultSet resultSet, Statement statement) {
		if ( statement == null ) {
			try {
				statement = resultSet.getStatement();
			}
			catch ( SQLException e ) {
				throw exceptionHelper.convert( e, "unable to access statement from resultset" );
			}
		}
		if ( statement != null ) {
			LOG.tracev( "Registering result set [{0}]", resultSet );
			Set<ResultSet> resultSets = xref.get( statement );
			if ( resultSets == null ) {
				LOG.unregisteredStatement();
			}
			if ( resultSets == null || resultSets == EMPTY_RESULTSET ) {
				resultSets = new HashSet<ResultSet>();
				xref.put( statement, resultSets );
			}
			resultSets.add( resultSet );
		}
		else {
			unassociatedResultSets.add( resultSet );
		}
	}

	@Override
	public void release(ResultSet resultSet, Statement statement) {
		LOG.tracev( "Releasing result set [{0}]", resultSet );
		if ( statement == null ) {
			try {
				statement = resultSet.getStatement();
			}
			catch ( SQLException e ) {
				throw exceptionHelper.convert( e, "unable to access statement from resultset" );
			}
		}
		if ( statement != null ) {
			final Set<ResultSet> resultSets = xref.get( statement );
			if ( resultSets == null ) {
				LOG.unregisteredStatement();
			}
			else {
				resultSets.remove( resultSet );
				if ( resultSets.isEmpty() ) {
					xref.remove( statement );
				}
			}
		}
		else {
			final boolean removed = unassociatedResultSets.remove( resultSet );
			if ( !removed ) {
				LOG.unregisteredResultSetWithoutStatement();
			}
		}
		close( resultSet );
	}

	@Override
	public boolean hasRegisteredResources() {
		return ! xref.isEmpty() || ! unassociatedResultSets.isEmpty();
	}

	@Override
	public void releaseResources() {
		LOG.tracev( "Releasing JDBC container resources [{0}]", this );
		cleanup();
	}
	
	@Override
	public void enableReleases() {
		releasesEnabled = true;
	}
	
	@Override
	public void disableReleases() {
		releasesEnabled = false;
	}

	private void cleanup() {
		for ( Map.Entry<Statement,Set<ResultSet>> entry : xref.entrySet() ) {
			closeAll( entry.getValue() );
			close( entry.getKey() );
		}
		xref.clear();

		closeAll( unassociatedResultSets );
	}

	protected void closeAll(Set<ResultSet> resultSets) {
		for ( ResultSet resultSet : resultSets ) {
			close( resultSet );
		}
		resultSets.clear();
	}

	@SuppressWarnings({ "unchecked" })
	protected void close(Statement statement) {
		LOG.tracev( "Closing prepared statement [{0}]", statement );
		
		// Important for Statement caching -- some DBs (especially Sybase) log warnings on every Statement under
		// certain situations.
		sqlExceptionHelper().logAndClearWarnings( statement );

		if ( statement instanceof InvalidatableWrapper ) {
			final InvalidatableWrapper<Statement> wrapper = (InvalidatableWrapper<Statement>) statement;
			close( wrapper.getWrappedObject() );
			wrapper.invalidate();
			return;
		}

		try {
			// if we are unable to "clean" the prepared statement,
			// we do not close it
			try {
				if ( statement.getMaxRows() != 0 ) {
					statement.setMaxRows( 0 );
				}
				if ( statement.getQueryTimeout() != 0 ) {
					statement.setQueryTimeout( 0 );
				}
			}
			catch( SQLException sqle ) {
				// there was a problem "cleaning" the prepared statement
				if ( LOG.isDebugEnabled() ) {
					LOG.debugf( "Exception clearing maxRows/queryTimeout [%s]", sqle.getMessage() );
				}
				// EARLY EXIT!!!
				return;
			}
			statement.close();
			if ( lastQuery == statement ) {
				lastQuery = null;
			}
		}
		catch( SQLException e ) {
			LOG.debugf( "Unable to release JDBC statement [%s]", e.getMessage() );
		}
		catch ( Exception e ) {
			// try to handle general errors more elegantly
			LOG.debugf( "Unable to release JDBC statement [%s]", e.getMessage() );
		}
	}

	@SuppressWarnings({ "unchecked" })
	protected void close(ResultSet resultSet) {
		LOG.tracev( "Closing result set [{0}]", resultSet );

		if ( resultSet instanceof InvalidatableWrapper ) {
			final InvalidatableWrapper<ResultSet> wrapper = (InvalidatableWrapper<ResultSet>) resultSet;
			close( wrapper.getWrappedObject() );
			wrapper.invalidate();
			return;
		}

		try {
			resultSet.close();
		}
		catch( SQLException e ) {
			LOG.debugf( "Unable to release JDBC result set [%s]", e.getMessage() );
		}
		catch ( Exception e ) {
			// try to handle general errors more elegantly
			LOG.debugf( "Unable to release JDBC result set [%s]", e.getMessage() );
		}
	}
}
