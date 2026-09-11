/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.internal;

import jakarta.persistence.EntityExistsException;

import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;
import org.hibernate.StatementObserver;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.batch.spi.GroupedBatch;
import org.hibernate.engine.jdbc.batch.spi.StaleStateMapper;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.sql.spi.mutation.TableMapping;

import static java.util.Objects.requireNonNull;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_MESSAGE_LOGGER;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Standard implementation of
 * {@link org.hibernate.engine.jdbc.batch.spi.Batch}.
 *
 * @author Steve Ebersole
 */
public class BatchImpl implements GroupedBatch {
	private final BatchKey key;
	private final int batchSizeToUse;
	private final PreparedStatementGroup statementGroup;

	private final JdbcCoordinator jdbcCoordinator;
	private final SqlStatementLogger sqlStatementLogger;
	private final StatementObserver statementObserver;
	private final SqlExceptionHelper sqlExceptionHelper;

	private final LinkedHashSet<BatchObserver> observers = new LinkedHashSet<>();

	private int batchPosition;
	private boolean batchExecuted;
	private StaleStateMapper[] staleStateMappers;
	private Binding[] valueBindings;

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

		var jdbcServices = jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getJdbcServices();
		sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		statementObserver = jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getStatementObserver();
		sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();

		if ( BATCH_MESSAGE_LOGGER.isTraceEnabled() ) {
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
			JdbcValueBindings jdbcValueBindings,
			TableInclusionChecker inclusionChecker,
			StaleStateMapper staleStateMapper) {
		if ( staleStateMapper != null ) {
			if ( staleStateMappers == null ) {
				staleStateMappers = new StaleStateMapper[batchSizeToUse];
			}
			staleStateMappers[batchPosition] = staleStateMapper;
		}
		else if ( staleStateMappers != null ) {
			staleStateMappers[batchPosition] = null;
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
					statementObserver.performingSql( statementDetails.getSqlString(), batchPosition+1 );
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
						if ( valueBindings == null ) {
							valueBindings = new Binding[batchSizeToUse];
						}
						if ( valueBindings[batchPosition] == null ) {
							valueBindings[batchPosition] = new Binding( jdbcValueBindings, new ArrayList<>( getStatementGroup().getNumberOfStatements() ) );
						}
						valueBindings[batchPosition].tableMappings.add( statementDetails.getMutatingTableDetails() );
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
							try {
								eventHandler.jdbcExecuteBatchStart();
								checkRowCounts( statement.executeBatch(), statementDetails );
							}
							catch (BatchUpdateException batchUpdateException) {
								maybeRetryBatch( statementDetails, statement, batchUpdateException );
							}
							catch (SQLException sqle) {
								jdbcCoordinator.afterFailedStatementExecution( sqle );
								throw sqle;
							}
							finally {
								eventMonitor.completeJdbcBatchExecutionEvent( executionEvent, sql );
								eventHandler.jdbcExecuteBatchEnd();
							}
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
						throw convertBatchException( sqlExceptionHelper.convert( e, "could not execute batch", sql ) );
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

	private void maybeRetryBatch(
			PreparedStatementDetails statementDetails,
			PreparedStatement statement,
			BatchUpdateException batchUpdateException) throws SQLException {
		// Let the Expectation do the update count checking and decide if a batch error
		// really is an error, since some statements may legitimately be allowed to fail
		final int[] updateCounts = batchUpdateException.getUpdateCounts();
		try {
			checkRowCounts( updateCounts, statementDetails );

			// Even though a failure happened, we reach this line of code, which means to retry,
			// but this time, we don't allow another retry and let the exception bubble up
			final boolean loggerTraceEnabled = BATCH_MESSAGE_LOGGER.isTraceEnabled();
			if ( loggerTraceEnabled ) {
				BATCH_MESSAGE_LOGGER.retryingBatch( getKey().toLoggableString() );
			}
			statement.clearBatch();
			int retryBatchPosition = 1;
			for ( int i = 0; i < updateCounts.length; i++ ) {
				if ( updateCounts[i] == Statement.EXECUTE_FAILED ) {
					final String sqlString = statementDetails.getSqlString();
					sqlStatementLogger.logStatement( sqlString );
					statementObserver.performingSql( statementDetails.getSqlString(), retryBatchPosition++ );
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
			}
			statement.executeBatch();
		}
		catch (RuntimeException e) {
			batchUpdateException.addSuppressed( e );
			throw batchUpdateException;
		}
	}

	private RuntimeException convertBatchException(RuntimeException exception) {
		return jdbcCoordinator.getJdbcSessionOwner() instanceof SharedSessionContractImplementor session
			&& session.getFactory().getSessionFactoryOptions().isJpaBootstrap()
			&& key instanceof EntityInsertBatchKey
			&& exception instanceof ConstraintViolationException cve
			&& cve.getKind() == ConstraintKind.UNIQUE
				? new EntityExistsException( cve )
				: exception;
	}

	private void clearBatchStateUntil(int batchCount) {
		if ( staleStateMappers != null ) {
			Arrays.fill( staleStateMappers, 0, batchCount, null );
		}
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
				if ( staleStateMappers != null && staleStateMappers[i] != null ) {
					final var mappedException = staleStateMappers[i].map( staleStateException );
					if ( mappedException != null ) {
						throw mappedException;
					}
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
		releaseStatements();
		clearBatchStateUntil( batchSizeToUse );
		staleStateMappers = null;
		valueBindings = null;
		observers.clear();
	}

	@Override
	public String toString() {
		return "BatchImpl(" + getKey().toLoggableString() + ")";
	}
}
