/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;

import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.batch.spi.BatchedResultChecker;
import org.hibernate.engine.jdbc.batch.spi.SingleStatementBatch;
import org.hibernate.engine.jdbc.batch.spi.StatementBinder;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;

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
		final int[] rowCounts;
		try {
			eventHandler.jdbcExecuteBatchStart();
			rowCounts = statement.executeBatch();
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
			jdbcCoordinator.afterStatementExecution();
		}

		try {
			batchExecuted = true;
			checkRowCounts( rowCounts );
		}
		finally {
			clearResultCheckers( batchPosition );
			batchPosition = 0;
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
			if ( !resultChecker.checkResult( 0, batchPosition, sqlString, session.getFactory() ) ) {
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

	private void clearResultCheckers(int batchCount) {
		for ( int i = 0; i < batchCount; i++ ) {
			resultCheckers[i] = null;
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
		observers.clear();
	}

	@Override
	public String toString() {
		return "SingleStatementBatchImpl(" + getKey().toLoggableString() + ")";
	}
}
