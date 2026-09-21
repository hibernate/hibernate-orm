/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashSet;

import jakarta.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;
import org.hibernate.StatementObserver;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.batch.spi.BatchedResultChecker;
import org.hibernate.engine.jdbc.batch.spi.SingleStatementBatch;
import org.hibernate.engine.jdbc.batch.spi.StatementBinder;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;

import static java.util.Objects.requireNonNull;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_MESSAGE_LOGGER;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Batch implementation for a single JDBC statement shape.
 *
 * @author Steve Ebersole
 */
public class SingleStatementBatchImpl implements SingleStatementBatch {
	private final BatchKey key;
	private final int batchSizeToUse;
	private final PreparableMutationOperation operation;
	private final JdbcCoordinator jdbcCoordinator;
	private final SharedSessionContractImplementor session;
	private final String sqlString;
	private final BatchedResultChecker[] resultCheckers;

	private final LinkedHashSet<BatchObserver> observers = new LinkedHashSet<>();
	private final @Nullable StatementBinder[] statementBinders;

	private PreparedStatement statement;
	private int batchPosition;
	private boolean batchExecuted;

	public SingleStatementBatchImpl(
			BatchKey key,
			PreparableMutationOperation operation,
			int batchSizeToUse,
			JdbcCoordinator jdbcCoordinator) {
		requireNonNull( key, "Batch key cannot be null" );
		requireNonNull( operation, "Mutation operation cannot be null" );
		requireNonNull( jdbcCoordinator, "JDBC coordinator cannot be null" );

		this.key = key;
		this.operation = operation;
		this.batchSizeToUse = batchSizeToUse;
		this.jdbcCoordinator = jdbcCoordinator;
		this.session = (SharedSessionContractImplementor) jdbcCoordinator.getJdbcSessionOwner();
		this.sqlString = operation.getSqlString();
		this.resultCheckers = new BatchedResultChecker[batchSizeToUse];

		if ( BATCH_MESSAGE_LOGGER.isTraceEnabled() ) {
			BATCH_MESSAGE_LOGGER.createBatch(
					batchSizeToUse,
					key.toLoggableString()
			);
		}
		statementBinders = operation.canRetry() ? new StatementBinder[batchSizeToUse] : null;
	}

	@Override
	public BatchKey getKey() {
		return key;
	}

	@Override
	public void addObserver(BatchObserver observer) {
		observers.add( observer );
	}

	@Override
	public void addToBatch(StatementBinder statementBinder, BatchedResultChecker resultChecker) {
		final int currentBatchPosition = batchPosition;
		if ( BATCH_MESSAGE_LOGGER.isTraceEnabled() ) {
			BATCH_MESSAGE_LOGGER.addToBatch(
					currentBatchPosition + 1,
					batchSizeToUse,
					key.toLoggableString()
			);
		}

		try {
			MODEL_MUTATION_LOGGER.addBatchForTable(
					operation.getTableDetails().getTableName(),
					currentBatchPosition + 1
			);

			final PreparedStatement statement = resolveStatement();
			session.getJdbcServices().getSqlStatementLogger().logStatement( sqlString );
			session.getJdbcSessionContext().getStatementObserver().performingSql( sqlString, currentBatchPosition + 1 );
			statementBinder.bind( statement, session );
			statement.addBatch();
			resultCheckers[currentBatchPosition] = resultChecker;
			if ( statementBinders != null ) {
				statementBinders[currentBatchPosition] = statementBinder;
			}
		}
		catch (SQLException exception) {
			abortBatch( exception );
			throw session.getJdbcServices()
					.getSqlExceptionHelper()
					.convert( exception, "Could not perform addBatch", sqlString );
		}
		catch (RuntimeException exception) {
			abortBatch( exception );
			throw exception;
		}

		batchPosition++;
		if ( batchPosition == batchSizeToUse ) {
			notifyObserversImplicitExecution();
			performExecution();
		}
	}

	private PreparedStatement resolveStatement() throws SQLException {
		if ( statement == null ) {
			statement = jdbcCoordinator.getMutationStatementPreparer()
					.prepareStatement( sqlString, operation.isCallable() );
			operation.getExpectation().prepare( statement );
		}
		return statement;
	}

	private void notifyObserversExplicitExecution() {
		for ( var observer : observers ) {
			observer.batchExplicitlyExecuted();
		}
	}

	private void notifyObserversImplicitExecution() {
		for ( var observer : observers ) {
			observer.batchImplicitlyExecuted();
		}
	}

	@Override
	public void execute() {
		notifyObserversExplicitExecution();
		try {
			if ( statement == null || batchPosition == 0 ) {
				if ( !batchExecuted && BATCH_MESSAGE_LOGGER.isTraceEnabled() ) {
					BATCH_MESSAGE_LOGGER.emptyBatch( key.toLoggableString() );
				}
			}
			else {
				performExecution();
			}
		}
		finally {
			releaseStatements();
		}
	}

	private void performExecution() {
		if ( BATCH_MESSAGE_LOGGER.isTraceEnabled() ) {
			BATCH_MESSAGE_LOGGER.executeBatch(
					batchPosition,
					batchSizeToUse,
					key.toLoggableString()
			);
		}

		final var jdbcSessionOwner = jdbcCoordinator.getJdbcSessionOwner();
		final var eventHandler = jdbcSessionOwner.getJdbcSessionContext().getEventHandler();
		final var eventMonitor = jdbcSessionOwner.getEventMonitor();
		final var executionEvent = eventMonitor.beginJdbcBatchExecutionEvent();
		try {
			eventHandler.jdbcExecuteBatchStart();
			int[] rowCounts;
			try {
				try {
					rowCounts = statement.executeBatch();
				}
				catch (BatchUpdateException batchUpdateException) {
					rowCounts = maybeRetryBatch( statement, batchUpdateException );
				}
			}
			catch (SQLException sqle) {
				jdbcCoordinator.afterFailedStatementExecution( sqle );
				abortBatch( sqle );
				throw session.getJdbcServices()
						.getSqlExceptionHelper()
						.convert( sqle, "could not execute batch", sqlString );
			}
			catch (RuntimeException re) {
				abortBatch( re );
				throw re;
			}
			finally {
				eventMonitor.completeJdbcBatchExecutionEvent( executionEvent, sqlString );
				eventHandler.jdbcExecuteBatchEnd();
			}

			checkRowCounts( rowCounts );

			batchExecuted = true;
		}
		finally {
			jdbcCoordinator.afterStatementExecution();
			clearBatchStateUntil( batchPosition );
			batchPosition = 0;
		}
	}

	private int[] maybeRetryBatch(
			PreparedStatement statement,
			BatchUpdateException batchUpdateException) throws SQLException {
		if ( statementBinders == null ) {
			// When no value bindings are available, this statement is not retryable
			throw batchUpdateException;
		}
		int availableBinders = 0;
		for ( StatementBinder statementBinder : statementBinders ) {
			if ( statementBinder == null ) {
				break;
			}
			availableBinders++;
		}
		int[] updateCounts = batchUpdateException.getUpdateCounts();
		int[] finalUpdateCounts;
		if ( updateCounts.length == availableBinders ) {
			finalUpdateCounts = updateCounts;
		}
		else {
			finalUpdateCounts = new int[availableBinders];
			// -4 is our empty constant
			Arrays.fill( finalUpdateCounts, -4 );
			System.arraycopy( updateCounts, 0, finalUpdateCounts, 0, updateCounts.length );
		}
		final SqlStatementLogger statementLogger = session.getJdbcServices().getSqlStatementLogger();
		final StatementObserver statementObserver = session.getJdbcSessionContext().getStatementObserver();
		final SqlExceptionHelper sqlExceptionHelper = session.getJdbcServices().getSqlExceptionHelper();
		int valueBindingToRetry = 0;
		try {
			int executedValueBindings = updateCounts.length;

			do {
				// Even though a failure happened, we reach this line of code, which means to retry,
				// but this time, we don't allow another retry and let the exception bubble up
				final boolean loggerTraceEnabled = BATCH_MESSAGE_LOGGER.isTraceEnabled();
				if ( loggerTraceEnabled ) {
					BATCH_MESSAGE_LOGGER.retryingBatch( getKey().toLoggableString() );
				}
				statement.clearBatch();
				int retryBatchPosition = 1;
				int failedCount = 0;
				for ( ; valueBindingToRetry < updateCounts.length; valueBindingToRetry++ ) {
					if ( updateCounts[valueBindingToRetry] == Statement.EXECUTE_FAILED ) {
						statementLogger.logStatement( sqlString );
						statementObserver.performingSql( sqlString, retryBatchPosition++ );
						statementBinders[valueBindingToRetry].bind( statement, session );
						try {
							statement.addBatch();
						}
						catch (SQLException exception) {
							throw sqlExceptionHelper.convert(
									exception,
									"Could not perform addBatch",
									sqlString
							);
						}
						failedCount++;
					}
				}
				// Some databases don't continue executing statements if one fails in a batch, so we have to redo them here
				for ( int i = valueBindingToRetry; i < availableBinders; i++ ) {
					statementLogger.logStatement( sqlString );
					statementObserver.performingSql( sqlString, retryBatchPosition++ );
					statementBinders[i].bind( statement, session );
					try {
						statement.addBatch();
					}
					catch (SQLException exception) {
						throw sqlExceptionHelper.convert(
								exception,
								"Could not perform addBatch",
								sqlString
						);
					}
				}
				try {
					int[] retryUpdateCounts = statement.executeBatch();
					for ( int i = 0, j = 0; i < finalUpdateCounts.length && j < retryUpdateCounts.length; i++ ) {
						if ( finalUpdateCounts[i] == Statement.EXECUTE_FAILED ) {
							finalUpdateCounts[i] = retryUpdateCounts[j++];
						}
						else if ( finalUpdateCounts[i] == -4 ) {
							// -4 is our empty constant
							finalUpdateCounts[i] = retryUpdateCounts[j++];
						}
					}

					assert finalUpdateCounts[finalUpdateCounts.length - 1] != -4 : "Not all batch positions were retried";
					executedValueBindings = availableBinders;
				}
				catch (BatchUpdateException batchUpdateRetryException) {
					final int[] retryUpdateCounts = batchUpdateRetryException.getUpdateCounts();
					if ( retryUpdateCounts.length == 0 || failedCount != 0 ) {
						// The database stops executing the batch on error and retry failed on the same statement again
						// or the retry of a previous failure failed again, so give up
						throw batchUpdateRetryException;
					}
					else {
						// Made some progress, but another statement failed now
						System.arraycopy( retryUpdateCounts, 0, finalUpdateCounts, executedValueBindings, retryUpdateCounts.length );
						executedValueBindings += retryUpdateCounts.length;
						updateCounts = retryUpdateCounts;
						batchUpdateException.addSuppressed( batchUpdateRetryException );
					}
				}
			} while ( executedValueBindings != availableBinders );

			return finalUpdateCounts;
		}
		catch (RuntimeException e) {
			batchUpdateException.addSuppressed( e );
			throw batchUpdateException;
		}
	}

	private void checkRowCounts(int[] rowCounts) {
		final int numberOfRowCounts = rowCounts.length;
		if ( batchPosition != 0 && numberOfRowCounts != batchPosition ) {
			JDBC_LOGGER.unexpectedRowCounts(
					operation.getTableDetails().getTableName(),
					numberOfRowCounts,
					batchPosition
			);
		}

		if ( !operation.getTableDetails().isIdentifierTable() ) {
			return;
		}

		for ( int i = 0; i < numberOfRowCounts; i++ ) {
			try {
				operation.getExpectation().verifyOutcome( rowCounts[i], statement, i, sqlString );
			}
			catch (StaleStateException staleStateException) {
				mapStaleStateException( staleStateException, i );
			}
			catch (SQLException e) {
				throw session.getJdbcServices()
						.getSqlExceptionHelper()
						.convert( e, "Unable to check batched mutation result - " + sqlString );
			}
		}
	}

	private void mapStaleStateException(StaleStateException staleStateException, int batchPosition) {
		final BatchedResultChecker resultChecker = resultCheckers[batchPosition];
		if ( resultChecker == null ) {
			return;
		}
		try {
			if ( !resultChecker.checkResult( 0, statement, batchPosition, sqlString, session.getFactory() ) ) {
				throw staleStateException;
			}
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (SQLException e) {
			throw session.getJdbcServices()
					.getSqlExceptionHelper()
					.convert( e, "Unable to check batched mutation result - " + sqlString );
		}
	}

	private void clearBatchStateUntil(int batchCount) {
		Arrays.fill( resultCheckers, 0, batchCount, null );
		if ( statementBinders != null ) {
			Arrays.fill( statementBinders, 0, batchCount, null );
		}
	}

	protected void releaseStatements() {
		if ( statement == null ) {
			return;
		}

		try {
			try {
				if ( !statement.isClosed() ) {
					statement.clearBatch();
				}
			}
			catch ( SQLException e ) {
				BATCH_MESSAGE_LOGGER.unableToReleaseBatchStatement();
			}
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( statement );
			jdbcCoordinator.afterStatementExecution();
		}
		finally {
			statement = null;
			batchPosition = 0;
		}
	}

	private void abortBatch(Exception cause) {
		try {
			jdbcCoordinator.abortBatch();
		}
		catch (RuntimeException e) {
			cause.addSuppressed( e );
		}
	}

	@Override
	public void release() {
		releaseStatements();
		clearBatchStateUntil( batchSizeToUse );
		observers.clear();
	}

	@Override
	public String toString() {
		return "SingleStatementBatchImpl(" + getKey().toLoggableString() + ")";
	}
}
