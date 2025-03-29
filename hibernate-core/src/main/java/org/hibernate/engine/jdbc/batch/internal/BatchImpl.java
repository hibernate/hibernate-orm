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
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.resource.jdbc.spi.JdbcEventHandler;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;

import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_MESSAGE_LOGGER;
import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_LOGGER;
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

	private final LinkedHashSet<BatchObserver> observers = new LinkedHashSet<>();

	private int batchPosition;
	private boolean batchExecuted;
	private StaleStateMapper[] staleStateMappers;

	public BatchImpl(
			BatchKey key,
			PreparedStatementGroup statementGroup,
			int batchSizeToUse,
			JdbcCoordinator jdbcCoordinator) {
		if ( key == null ) {
			throw new IllegalArgumentException( "Batch key cannot be null" );
		}
		if ( jdbcCoordinator == null ) {
			throw new IllegalArgumentException( "JDBC coordinator cannot be null" );
		}

		this.batchSizeToUse = batchSizeToUse;
		this.key = key;
		this.jdbcCoordinator = jdbcCoordinator;
		this.statementGroup = statementGroup;

		final JdbcServices jdbcServices = jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getJdbcServices();
		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();

		if ( BATCH_LOGGER.isTraceEnabled() ) {
			BATCH_MESSAGE_LOGGER.createBatch(
					batchSizeToUse,
					key.toLoggableString()
			);
		}
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
		final boolean loggerTraceEnabled = BATCH_LOGGER.isTraceEnabled();
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
						MODEL_MUTATION_LOGGER.tracef(
								"Skipping addBatch for table : %s (batch-position=%s)",
								statementDetails.getMutatingTableDetails().getTableName(),
								batchPosition+1
						);
					}
				}
				else {
					//noinspection resource
					final PreparedStatement statement = statementDetails.resolveStatement();
					sqlStatementLogger.logStatement( statementDetails.getSqlString() );
					jdbcValueBindings.beforeStatement( statementDetails );
					try {
						statement.addBatch();
					}
					catch (SQLException e) {
						BATCH_LOGGER.debug( "SQLException escaped proxy", e );
						throw sqlExceptionHelper.convert(
								e,
								"Could not perform addBatch",
								statementDetails.getSqlString()
						);
					}
					finally {
						jdbcValueBindings.afterStatement( statementDetails.getMutatingTableDetails() );
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
		final PreparedStatement statement = statementDetails.getStatement();
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
		for ( BatchObserver observer : observers ) {
			observer.batchExplicitlyExecuted();
		}
	}

	/**
	 * Convenience method to notify registered observers of an implicit execution of this batch.
	 */
	protected final void notifyObserversImplicitExecution() {
		for ( BatchObserver observer : observers ) {
			observer.batchImplicitlyExecuted();
		}
	}

	protected void abortBatch(Exception cause) {
		try {
			jdbcCoordinator.abortBatch();
		}
		catch (RuntimeException e) {
			cause.addSuppressed( e );
		}
	}

	@Override
	public void execute() {
		notifyObserversExplicitExecution();
		if ( getStatementGroup().getNumberOfStatements() != 0 ) {
			try {
				if ( batchPosition == 0 ) {
					if ( !batchExecuted && BATCH_LOGGER.isDebugEnabled() ) {
						BATCH_LOGGER.debugf(
								"No batched statements to execute - %s",
								getKey().toLoggableString()
						);
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
		if ( BATCH_LOGGER.isTraceEnabled() ) {
			BATCH_MESSAGE_LOGGER.executeBatch(
					batchPosition,
					batchSizeToUse,
					getKey().toLoggableString()
			);
		}

		final JdbcSessionOwner jdbcSessionOwner = jdbcCoordinator.getJdbcSessionOwner();
		final JdbcEventHandler eventHandler = jdbcSessionOwner.getJdbcSessionContext().getEventHandler();
		try {
			getStatementGroup().forEachStatement( (tableName, statementDetails) -> {
				final String sql = statementDetails.getSqlString();
				final PreparedStatement statement = statementDetails.getStatement();
				if ( statement != null ) {
					try {
						if ( statementDetails.getMutatingTableDetails().isIdentifierTable() ) {
							final int[] rowCounts;
							final EventMonitor eventMonitor = jdbcSessionOwner.getEventMonitor();
							final DiagnosticEvent executionEvent = eventMonitor.beginJdbcBatchExecutionEvent();
							try {
								eventHandler.jdbcExecuteBatchStart();
								rowCounts = statement.executeBatch();
							}
							finally {
								eventMonitor.completeJdbcBatchExecutionEvent( executionEvent, sql );
								eventHandler.jdbcExecuteBatchEnd();
							}
							checkRowCounts( rowCounts, statementDetails );
						}
						else {
							statement.executeBatch();
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
			batchPosition = 0;
		}
	}

	private void checkRowCounts(int[] rowCounts, PreparedStatementDetails statementDetails)
			throws SQLException, HibernateException {
		final int numberOfRowCounts = rowCounts.length;
		if ( batchPosition != 0 && numberOfRowCounts != batchPosition ) {
			JDBC_MESSAGE_LOGGER.unexpectedRowCounts(
					statementDetails.getMutatingTableDetails().getTableName(),
					numberOfRowCounts,
					batchPosition
			);
		}

		for ( int i = 0; i < numberOfRowCounts; i++ ) {
			try {
				statementDetails.getExpectation()
						.verifyOutcome( rowCounts[i], statementDetails.getStatement(), i, statementDetails.getSqlString() );
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
			final PreparedStatementGroup statementGroup = getStatementGroup();
			if ( statementGroup.getNumberOfStatements() != 0
					&& statementGroup.hasMatching( statementDetails -> statementDetails.getStatement() != null ) ) {
				BATCH_MESSAGE_LOGGER.batchContainedStatementsOnRelease();
			}
		}
		releaseStatements();
		observers.clear();
	}

	@Override
	public String toString() {
		return "BatchImpl(" + getKey().toLoggableString() + ")";
	}
}
