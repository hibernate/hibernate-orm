/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.jdbc.batch.JdbcBatchLogging;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.JdbcWrapper;
import org.hibernate.engine.jdbc.spi.MutationStatementPreparer;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.internal.LogicalConnectionManagedImpl;
import org.hibernate.resource.jdbc.internal.LogicalConnectionProvidedImpl;
import org.hibernate.resource.jdbc.internal.ResourceRegistryStandardImpl;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

import static org.hibernate.ConnectionReleaseMode.AFTER_STATEMENT;

/**
 * Standard implementation of {@link JdbcCoordinator}.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 * @author Sanne Grinovero
 */
public class JdbcCoordinatorImpl implements JdbcCoordinator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JdbcCoordinatorImpl.class );
	private static final boolean TRACE_ENABLED = LOG.isTraceEnabled();

	private transient final LogicalConnectionImplementor logicalConnection;
	private transient final JdbcSessionOwner owner;

	private transient final JdbcServices jdbcServices;

	private transient Batch currentBatch;

	private transient long transactionTimeOutInstant = -1;

	private Statement lastQuery;
	private final boolean isUserSuppliedConnection;

	/**
	 * If true, manually (and temporarily) circumvent aggressive release processing.
	 */
	private boolean releasesEnabled = true;

	/**
	 * Constructs a {@code JdbcCoordinatorImpl}
	 *
	 * @param userSuppliedConnection The user supplied connection (may be null)
	 */
	public JdbcCoordinatorImpl(
			Connection userSuppliedConnection,
			JdbcSessionOwner owner,
			JdbcServices jdbcServices) {
		this.owner = owner;
		this.jdbcServices = jdbcServices;
		this.isUserSuppliedConnection = userSuppliedConnection != null;
		this.logicalConnection = createLogicalConnection( userSuppliedConnection, owner );
	}

	private static LogicalConnectionImplementor createLogicalConnection(
			Connection userSuppliedConnection,
			JdbcSessionOwner owner) {
		final ResourceRegistry resourceRegistry =
				new ResourceRegistryStandardImpl( owner.getJdbcSessionContext().getEventHandler() );
		return userSuppliedConnection == null
				? new LogicalConnectionManagedImpl( owner, resourceRegistry )
				: new LogicalConnectionProvidedImpl( userSuppliedConnection, resourceRegistry );
	}

	private JdbcCoordinatorImpl(
			LogicalConnectionImplementor logicalConnection,
			boolean isUserSuppliedConnection,
			JdbcSessionOwner owner) {
		this.logicalConnection = logicalConnection;
		this.isUserSuppliedConnection = isUserSuppliedConnection;
		this.owner = owner;
		this.jdbcServices = owner.getJdbcSessionContext().getJdbcServices();
	}

	@Override
	public LogicalConnectionImplementor getLogicalConnection() {
		return logicalConnection;
	}

	/**
	 * Access to the {@link SqlExceptionHelper}
	 *
	 * @return The {@code SqlExceptionHelper}
	 */
	public SqlExceptionHelper sqlExceptionHelper() {
		return jdbcServices.getSqlExceptionHelper();
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
		if ( TRACE_ENABLED ) LOG.tracev( "Closing JDBC container [{0}]", this );
		Connection connection;
		try {
			if ( currentBatch != null ) {
				LOG.closingUnreleasedBatch();
				currentBatch.release();
			}
		}
		finally {
			connection = logicalConnection.close();
		}
		return connection;
	}

	@Override
	public Batch getBatch(BatchKey key, Integer batchSize, Supplier<PreparedStatementGroup> statementGroupSupplier) {
		if ( currentBatch != null ) {
			if ( currentBatch.getKey().equals( key ) ) {
				return currentBatch;
			}
			else {
				try {
					currentBatch.execute();
				}
				finally {
					currentBatch.release();
				}
			}
		}

		currentBatch = owner.getJdbcSessionContext().getBatchBuilder()
				.buildBatch( key, batchSize, statementGroupSupplier, this );

		return currentBatch;
	}

	@Override
	public void executeBatch() {
		if ( currentBatch != null ) {
			try {
				currentBatch.execute();
			}
			finally {
				currentBatch.release();
			}
		}
	}

	@Override
	public void conditionallyExecuteBatch(BatchKey key) {
		if ( currentBatch != null && !currentBatch.getKey().equals( key ) ) {
			JdbcBatchLogging.BATCH_LOGGER.debugf( "Conditionally executing batch - %s", currentBatch.getKey() );
			try {
				currentBatch.execute();
			}
			finally {
				currentBatch.release();
			}
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
			statementPreparer = new StatementPreparerImpl( this, jdbcServices );
		}
		return statementPreparer;
	}

	private transient MutationStatementPreparer mutationStatementPreparer;

	@Override
	public MutationStatementPreparer getMutationStatementPreparer() {
		if ( mutationStatementPreparer == null ) {
			mutationStatementPreparer = new MutationStatementPreparerImpl( this, jdbcServices );
		}
		return mutationStatementPreparer;
	}

	private transient ResultSetReturn resultSetExtractor;

	@Override
	public ResultSetReturn getResultSetReturn() {
		if ( resultSetExtractor == null ) {
			resultSetExtractor = new ResultSetReturnImpl( this, jdbcServices );
		}
		return resultSetExtractor;
	}

	@Override
	public void setTransactionTimeOut(int seconds) {
		transactionTimeOutInstant = System.currentTimeMillis() + ( seconds * 1000L );
	}

	@Override
	public void flushBeforeTransactionCompletion() {
		getJdbcSessionOwner().flushBeforeTransactionCompletion();
	}

	@Override
	public int determineRemainingTransactionTimeOutPeriod() {
		if ( transactionTimeOutInstant < 0 ) {
			return -1;
		}
		final long millisecondsRemaining = transactionTimeOutInstant - System.currentTimeMillis();
		if ( millisecondsRemaining <= 0L ) {
			throw new TransactionException( "transaction timeout expired" );
		}
		return Math.max( (int) (millisecondsRemaining / 1000), 1 );
	}

	@Override
	public void afterStatementExecution() {
		final ConnectionReleaseMode connectionReleaseMode = connectionReleaseMode();
		if ( TRACE_ENABLED ) LOG.tracev( "Starting after statement execution processing [{0}]", connectionReleaseMode );
		if ( connectionReleaseMode == AFTER_STATEMENT ) {
			if ( ! releasesEnabled ) {
				LOG.debug( "Skipping aggressive release due to manual disabling" );
			}
			else if ( hasRegisteredResources() ) {
				LOG.debug( "Skipping aggressive release due to registered resources" );
			}
			else {
				getLogicalConnection().afterStatement();
			}
		}
	}

	@Override
	public void afterTransaction() {
		transactionTimeOutInstant = -1;
		switch ( connectionReleaseMode() ) {
			case AFTER_STATEMENT:
			case AFTER_TRANSACTION:
			case BEFORE_TRANSACTION_COMPLETION:
				logicalConnection.afterTransaction();
		}
	}

	private ConnectionReleaseMode connectionReleaseMode() {
		return getLogicalConnection().getConnectionHandlingMode().getReleaseMode();
	}

	private boolean hasRegisteredResources() {
		return getLogicalConnection().getResourceRegistry().hasRegisteredResources();
	}

	@Override
	public <T> T coordinateWork(WorkExecutorVisitable<T> work) {
		final Connection connection = getLogicalConnection().getPhysicalConnection();
		try {
			final T result = work.accept( new WorkExecutor<>(), connection );
			afterStatementExecution();
			return result;
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "error executing work" );
		}
	}

	@Override
	public boolean isReadyForSerialization() {
		return isUserSuppliedConnection
				? ! getLogicalConnection().isPhysicallyConnected()
				: ! hasRegisteredResources();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void registerLastQuery(Statement statement) {
		if ( TRACE_ENABLED ) LOG.tracev( "Registering last query statement [{0}]", statement );
		if ( statement instanceof JdbcWrapper ) {
			final JdbcWrapper<Statement> wrapper = (JdbcWrapper<Statement>) statement;
			registerLastQuery( wrapper.getWrappedObject() );
		}
		else {
			lastQuery = statement;
		}
	}

	@Override
	public void cancelLastQuery() {
		try {
			if ( lastQuery != null ) {
				lastQuery.cancel();
			}
		}
		catch ( SQLException sqle ) {
			throw safeSqlExceptionHelper().convert( sqle, "Cannot cancel query" );
		}
		finally {
			lastQuery = null;
		}
	}

	private SqlExceptionHelper safeSqlExceptionHelper() {
		final SqlExceptionHelper sqlExceptionHelper = sqlExceptionHelper();
		//Should always be non-null, but to make sure as the implementation is lazy:
		return sqlExceptionHelper == null ? new SqlExceptionHelper( false ) : sqlExceptionHelper;
	}

	@Override
	public void enableReleases() {
		releasesEnabled = true;
	}

	@Override
	public void disableReleases() {
		releasesEnabled = false;
	}

	@Override
	public boolean isActive() {
		return owner.getJdbcSessionContext().isActive();
	}

	@Override
	public void afterTransactionBegin() {
		owner.afterTransactionBegin();
	}

	@Override
	public void beforeTransactionCompletion() {
		owner.beforeTransactionCompletion();
		logicalConnection.beforeTransactionCompletion();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		afterTransaction();
		owner.afterTransactionCompletion( successful, delayed );
	}

	@Override
	public JdbcSessionOwner getJdbcSessionOwner() {
		return owner;
	}

	@Override
	public JdbcResourceTransaction getResourceLocalTransaction() {
		return logicalConnection.getPhysicalJdbcTransaction();
	}

	/**
	 * JDK serialization hook
	 *
	 * @param oos The stream into which to write our state
	 *
	 * @throws IOException Trouble accessing the stream
	 */
	@Override
	public void serialize(ObjectOutputStream oos) throws IOException {
		if ( !isReadyForSerialization() ) {
			throw new HibernateException( "Cannot serialize Session while connected" );
		}
		oos.writeBoolean( isUserSuppliedConnection );
		logicalConnection.serialize( oos );
	}

	/**
	 * JDK deserialization hook
	 *
	 * @param ois The stream into which to write our state
	 * @param owner The Jdbc Session owner which owns the JdbcCoordinatorImpl to be deserialized.
	 *
	 * @return The deserialized {@code JdbcCoordinatorImpl}
	 *
	 * @throws IOException Trouble accessing the stream
	 * @throws ClassNotFoundException Trouble reading the stream
	 */
	public static JdbcCoordinatorImpl deserialize(ObjectInputStream ois, JdbcSessionOwner owner)
			throws IOException, ClassNotFoundException {
		final boolean isUserSuppliedConnection = ois.readBoolean();
		final var logicalConnection =
				isUserSuppliedConnection
						? LogicalConnectionProvidedImpl.deserialize( ois )
						: LogicalConnectionManagedImpl.deserialize( ois, owner );
		return new JdbcCoordinatorImpl( logicalConnection, isUserSuppliedConnection, owner );
	}
}
