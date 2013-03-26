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
import org.jboss.logging.Logger.Level;

/**
 * Standard Hibernate implementation of {@link JdbcCoordinator}
 * <p/>
 * IMPL NOTE : Custom serialization handling!
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class JdbcCoordinatorImpl implements JdbcCoordinator {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class, JdbcCoordinatorImpl.class.getName()
	);

	private transient TransactionCoordinator transactionCoordinator;
	private final transient LogicalConnectionImpl logicalConnection;

	private transient Batch currentBatch;

	private transient long transactionTimeOutInstant = -1;

	private final HashMap<Statement,Set<ResultSet>> xref = new HashMap<Statement,Set<ResultSet>>();
	private final Set<ResultSet> unassociatedResultSets = new HashSet<ResultSet>();
	private final SqlExceptionHelper exceptionHelper;

	private Statement lastQuery;

	/**
	 * If true, manually (and temporarily) circumvent aggressive release processing.
	 */
	private boolean releasesEnabled = true;

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

	public SqlExceptionHelper sqlExceptionHelper() {
		return transactionEnvironment().getJdbcServices().getSqlExceptionHelper();
	}


	private int flushDepth = 0;

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
			currentBatch.release(); // needed?
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
		Connection connection = getLogicalConnection().getConnection();
		try {
			T result = work.accept( new WorkExecutor<T>(), connection );
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

	public void serialize(ObjectOutputStream oos) throws IOException {
		if ( ! isReadyForSerialization() ) {
			throw new HibernateException( "Cannot serialize Session while connected" );
		}
		logicalConnection.serialize( oos );
	}

	public static JdbcCoordinatorImpl deserialize(
			ObjectInputStream ois,
			TransactionContext transactionContext) throws IOException, ClassNotFoundException {
		return new JdbcCoordinatorImpl( LogicalConnectionImpl.deserialize( ois, transactionContext ) );
 	}

	public void afterDeserialize(TransactionCoordinatorImpl transactionCoordinator) {
		this.transactionCoordinator = transactionCoordinator;
	}

	@Override
	public void register(Statement statement) {
		LOG.tracev( "Registering statement [{0}]", statement );
		if ( xref.containsKey( statement ) ) {
			throw new HibernateException( "statement already registered with JDBCContainer" );
		}
		xref.put( statement, null );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public void registerLastQuery(Statement statement) {
		LOG.tracev( "Registering last query statement [{0}]", statement );
		if ( statement instanceof JdbcWrapper ) {
			JdbcWrapper<Statement> wrapper = ( JdbcWrapper<Statement> ) statement;
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
			throw exceptionHelper.convert(
			        sqle,
			        "Cannot cancel query"
				);
		}
		finally {
			lastQuery = null;
		}
	}

	@Override
	public void release(Statement statement) {
		LOG.tracev( "Releasing statement [{0}]", statement );
		Set<ResultSet> resultSets = xref.get( statement );
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
		LOG.tracev( "Registering result set [{0}]", resultSet );
		if ( statement == null ) {
			try {
				statement = resultSet.getStatement();
			}
			catch ( SQLException e ) {
				throw exceptionHelper.convert( e, "unable to access statement from resultset" );
			}
		}
		if ( statement != null ) {
			// Keep this at DEBUG level, rather than warn.  Numerous connection pool implementations can return a
			// proxy/wrapper around the JDBC Statement, causing excessive logging here.  See HHH-8210.
			if ( LOG.isEnabled( Level.DEBUG ) && !xref.containsKey( statement ) ) {
				LOG.unregisteredStatement();
			}
			Set<ResultSet> resultSets = xref.get( statement );
			if ( resultSets == null ) {
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
			// Keep this at DEBUG level, rather than warn.  Numerous connection pool implementations can return a
			// proxy/wrapper around the JDBC Statement, causing excessive logging here.  See HHH-8210.
			if ( LOG.isEnabled( Level.DEBUG ) && !xref.containsKey( statement ) ) {
				LOG.unregisteredStatement();
			}
			Set<ResultSet> resultSets = xref.get( statement );
			if ( resultSets != null ) {
				resultSets.remove( resultSet );
				if ( resultSets.isEmpty() ) {
					xref.remove( statement );
				}
			}
		}
		else {
			boolean removed = unassociatedResultSets.remove( resultSet );
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
			if ( entry.getValue() != null ) {
				closeAll( entry.getValue() );
			}
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

		if ( statement instanceof InvalidatableWrapper ) {
			InvalidatableWrapper<Statement> wrapper = ( InvalidatableWrapper<Statement> ) statement;
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
				return; // EARLY EXIT!!!
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
			InvalidatableWrapper<ResultSet> wrapper = (InvalidatableWrapper<ResultSet>) resultSet;
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
