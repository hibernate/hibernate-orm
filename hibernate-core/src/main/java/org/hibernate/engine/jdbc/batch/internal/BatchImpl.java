/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.internal;

import jakarta.annotation.Nullable;

import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.sql.model.TableMapping;

import static java.util.Objects.requireNonNull;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_MESSAGE_LOGGER;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Standard implementation of {@link Batch}
 *
 * @author Steve Ebersole
 */
public class BatchImpl implements Batch {
	private final BatchKey key;
	private final int batchSizeToUse;
	private final PreparedStatementGroup statementGroup;

	private final JdbcCoordinator jdbcCoordinator;
	private final SqlStatementLogger sqlStatementLogger;
	private final SqlExceptionHelper sqlExceptionHelper;
	private final JdbcServices jdbcServices;

	private final LinkedHashSet<BatchObserver> observers = new LinkedHashSet<>();
	private final @Nullable Binding[] valueBindings;

	private int batchPosition;
	private boolean batchExecuted;
	private StaleStateMapper[] staleStateMappers;

	private record Binding(JdbcValueBindings jdbcValueBindings, ArrayList<TableMapping> tableMappings) {}

	public BatchImpl(
			BatchKey key,
			PreparedStatementGroup statementGroup,
			int batchSizeToUse,
			JdbcCoordinator jdbcCoordinator) {
		requireNonNull( key, "Batch key cannot be null" );
		requireNonNull( jdbcCoordinator, "JDBC coordinator cannot be null" );

		this.batchSizeToUse = batchSizeToUse;
		this.key = key;
		this.jdbcCoordinator = jdbcCoordinator;
		this.statementGroup = statementGroup;

		this.jdbcServices =
				jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getJdbcServices();
		sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();

		if ( BATCH_MESSAGE_LOGGER.isTraceEnabled() ) {
			BATCH_MESSAGE_LOGGER.createBatch(
					batchSizeToUse,
					key.toLoggableString()
			);
		}
		valueBindings = statementGroup.canRetry() ? new Binding[batchSizeToUse] : null;
	}

	@Override
	public final BatchKey getKey() {
		return key;
	}

	@Override
	public PreparedStatementGroup getStatementGroup() {
		return statementGroup;
	}

	@Override
	public void addObserver(BatchObserver observer) {
		observers.add( observer );
	}

	@Override
	public void addToBatch(
			JdbcValueBindings jdbcValueBindings, TableInclusionChecker inclusionChecker,
			StaleStateMapper staleStateMapper) {
		if ( staleStateMapper != null ) {
			if ( staleStateMappers == null ) {
				staleStateMappers = new StaleStateMapper[batchSizeToUse];
			}
			staleStateMappers[batchPosition] = staleStateMapper;
		}
		addToBatch( jdbcValueBindings, inclusionChecker );
	}

	@Override
	public void addToBatch(JdbcValueBindings jdbcValueBindings, TableInclusionChecker inclusionChecker) {
		final boolean loggerTraceEnabled = BATCH_MESSAGE_LOGGER.isTraceEnabled();
		if ( loggerTraceEnabled ) {
			BATCH_MESSAGE_LOGGER.addToBatch(
					batchPosition + 1,
					batchSizeToUse,
					getKey().toLoggableString()
			);
		}

		try {
			getStatementGroup().forEachStatement( (tableName, statementDetails) -> {
				if ( inclusionChecker != null
						&& !inclusionChecker.include( statementDetails.getMutatingTableDetails() ) ) {
					if ( loggerTraceEnabled ) {
						MODEL_MUTATION_LOGGER.skippingAddBatchForTable(
								statementDetails.getMutatingTableDetails().getTableName(),
								batchPosition+1
						);
					}
				}
				else {
					MODEL_MUTATION_LOGGER.addBatchForTable(
							statementDetails.getMutatingTableDetails().getTableName(),
							batchPosition+1
					);
					//noinspection resource
					final var statement = statementDetails.resolveStatement();
					final String sqlString = statementDetails.getSqlString();
					sqlStatementLogger.logStatement( sqlString );
					jdbcValueBindings.beforeStatement( statementDetails );
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
					finally {
						if ( valueBindings != null ) {
							if ( valueBindings[batchPosition] == null ) {
								valueBindings[batchPosition] = new Binding( jdbcValueBindings,
										new ArrayList<>( getStatementGroup().getNumberOfStatements() ) );
							}
							valueBindings[batchPosition].tableMappings.add(
									statementDetails.getMutatingTableDetails() );
						}
						else {
							jdbcValueBindings.afterStatement( statementDetails.getMutatingTableDetails() );
						}
					}
				}
			} );
		}
		catch (RuntimeException e) {
			abortBatch( e );
			throw e;
		}

		batchPosition++;
		if ( batchPosition == batchSizeToUse ) {
			notifyObserversImplicitExecution();
			performExecution();
		}
	}

	protected void releaseStatements() {
		statementGroup.release();
	}

	protected void clearBatch(PreparedStatementDetails statementDetails) {
		final var statement = statementDetails.getStatement();
		assert statement != null;

		try {
			// This code can be called after the connection is released
			// and the statement is closed. If the statement is closed,
			// then SQLException will be thrown when PreparedStatement#clearBatch
			// is called.
			// Ensure the statement is not closed before
			// calling PreparedStatement#clearBatch.
			if ( !statement.isClosed() ) {
				statement.clearBatch();
			}
		}
		catch ( SQLException e ) {
			BATCH_MESSAGE_LOGGER.unableToReleaseBatchStatement();
		}
	}

	/**
	 * Convenience method to notify registered observers of an explicit execution of this batch.
	 */
	protected final void notifyObserversExplicitExecution() {
		for ( var observer : observers ) {
			observer.batchExplicitlyExecuted();
		}
	}

	/**
	 * Convenience method to notify registered observers of an implicit execution of this batch.
	 */
	protected final void notifyObserversImplicitExecution() {
		for ( var observer : observers ) {
			observer.batchImplicitlyExecuted();
		}
	}

	protected void abortBatch(Exception cause) {
		try {
			close();
			jdbcCoordinator.abortBatch();
		}
		catch (RuntimeException e) {
			cause.addSuppressed( e );
		}
	}

	@Override
	public void execute() {
		notifyObserversExplicitExecution();
		if ( getStatementGroup().getNumberOfStatements() > 0 ) {
			try {
				if ( batchPosition == 0 ) {
					if ( !batchExecuted && BATCH_MESSAGE_LOGGER.isTraceEnabled() ) {
						BATCH_MESSAGE_LOGGER.emptyBatch( getKey().toLoggableString() );
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
	}

	protected void performExecution() {
		if ( BATCH_MESSAGE_LOGGER.isTraceEnabled() ) {
			BATCH_MESSAGE_LOGGER.executeBatch(
					batchPosition,
					batchSizeToUse,
					getKey().toLoggableString()
			);
		}

		final var jdbcSessionOwner = jdbcCoordinator.getJdbcSessionOwner();
		final var eventHandler = jdbcSessionOwner.getJdbcSessionContext().getEventHandler();
		try {
			getStatementGroup().forEachStatement( (tableName, statementDetails) -> {
				final String sql = statementDetails.getSqlString();
				final var statement = statementDetails.getStatement();
				if ( statement != null ) {
					try {
						if ( statementDetails.getMutatingTableDetails().isIdentifierTable() ) {
							final var eventMonitor = jdbcSessionOwner.getEventMonitor();
							final var executionEvent = eventMonitor.beginJdbcBatchExecutionEvent();
							eventHandler.jdbcExecuteBatchStart();
							int[] rowCounts;
							try {
								try {
									rowCounts = statement.executeBatch();
								}
								catch (BatchUpdateException batchUpdateException) {
									rowCounts = maybeRetryBatch( statementDetails, statement, batchUpdateException );
								}
							}
							catch (SQLException sqle) {
								jdbcCoordinator.afterFailedStatementExecution( sqle );
								throw sqle;
							}
							finally {
								eventMonitor.completeJdbcBatchExecutionEvent( executionEvent, sql );
								eventHandler.jdbcExecuteBatchEnd();
							}
							checkRowCounts( rowCounts, statementDetails );
						}
						else {
							try {
								statement.executeBatch();
							}
							catch (BatchUpdateException batchUpdateException) {
								maybeRetryBatch( statementDetails, statement, batchUpdateException );
							}
						}
					}
					catch (SQLException e) {
						abortBatch( e );
						throw sqlExceptionHelper.convert( e, "could not execute batch", sql );
					}
					catch (RuntimeException re) {
						abortBatch( re );
						throw re;
					}
				}
			} );
			batchExecuted = true;
		}
		finally {
			jdbcCoordinator.afterStatementExecution();
			clearBatchStateUntil( batchPosition );
			batchPosition = 0;
		}
	}

	private int[] maybeRetryBatch(
			PreparedStatementDetails statementDetails,
			PreparedStatement statement,
			BatchUpdateException batchUpdateException) throws SQLException {
		if ( valueBindings == null ) {
			// When no value bindings are available, this statement is not retryable
			throw batchUpdateException;
		}
		int availableValueBindings = 0;
		for ( Binding valueBinding : valueBindings ) {
			if ( valueBinding == null ) {
				break;
			}
			availableValueBindings++;
		}
		int[] updateCounts = batchUpdateException.getUpdateCounts();
		int[] finalUpdateCounts;
		if ( updateCounts.length == availableValueBindings ) {
			finalUpdateCounts = updateCounts;
		}
		else {
			finalUpdateCounts = new int[availableValueBindings];
			// -4 is our empty constant
			Arrays.fill( finalUpdateCounts, -4 );
			System.arraycopy( updateCounts, 0, finalUpdateCounts, 0, updateCounts.length );
		}
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
						final String sqlString = statementDetails.getSqlString();
						sqlStatementLogger.logStatement( sqlString );
						valueBindings[valueBindingToRetry].jdbcValueBindings.beforeStatement( statementDetails );
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
				for ( int i = valueBindingToRetry; i < availableValueBindings; i++ ) {
					final String sqlString = statementDetails.getSqlString();
					sqlStatementLogger.logStatement( sqlString );
					valueBindings[i].jdbcValueBindings.beforeStatement( statementDetails );
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
					executedValueBindings = availableValueBindings;
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
			} while ( executedValueBindings != availableValueBindings );

			return finalUpdateCounts;
		}
		catch (RuntimeException e) {
			batchUpdateException.addSuppressed( e );
			throw batchUpdateException;
		}
	}

	private void clearBatchStateUntil(int batchCount) {
		if ( staleStateMappers != null ) {
			Arrays.fill( staleStateMappers, 0, batchCount, null );
		}
		if ( valueBindings != null ) {
			for ( int i = 0; i < batchCount; i++ ) {
				final Binding valueBinding = valueBindings[i];
				if ( valueBinding != null ) {
					for ( TableMapping tableMapping : valueBinding.tableMappings ) {
						valueBinding.jdbcValueBindings.afterStatement( tableMapping );
					}
				}
			}
			Arrays.fill( valueBindings, 0, batchCount, null );
		}
	}

	private void checkRowCounts(int[] rowCounts, PreparedStatementDetails statementDetails)
			throws SQLException, HibernateException {
		final int numberOfRowCounts = rowCounts.length;
		if ( batchPosition != 0 && numberOfRowCounts != batchPosition ) {
			JDBC_LOGGER.unexpectedRowCounts(
					statementDetails.getMutatingTableDetails().getTableName(),
					numberOfRowCounts,
					batchPosition
			);
		}

		final String sql = statementDetails.getSqlString();
		for ( int i = 0; i < numberOfRowCounts; i++ ) {
			try {
				statementDetails.getExpectation()
						.verifyOutcome( rowCounts[i], statementDetails.getStatement(), i, sql );
			}
			catch ( StaleStateException staleStateException ) {
				if ( staleStateMappers != null ) {
					throw staleStateMappers[i].map( staleStateException );
				}
			}
		}
	}

	@Override
	public void release() {
		if ( BATCH_MESSAGE_LOGGER.isInfoEnabled() ) {
			final var statementGroup = getStatementGroup();
			if ( statementGroup.getNumberOfStatements() > 0
					&& statementGroup.hasMatching( statementDetails -> statementDetails.getStatement() != null ) ) {
				BATCH_MESSAGE_LOGGER.batchContainedStatementsOnRelease();
			}
		}
		close();
	}

	private void close(){
		releaseStatements();
		clearBatchStateUntil( batchSizeToUse );
		staleStateMappers = null;
		observers.clear();
	}

	@Override
	public String toString() {
		return "BatchImpl(" + getKey().toLoggableString() + ")";
	}
}
