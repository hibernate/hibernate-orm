/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;

import org.hibernate.HibernateException;
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
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateMonitoringEvent;
import org.hibernate.resource.jdbc.spi.JdbcObserver;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;

import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_MESSAGE_LOGGER;
import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_LOGGER;
import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_MESSAGE_LOGGER;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Standard implementation of Batch
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

		this.key = key;
		this.jdbcCoordinator = jdbcCoordinator;
		this.statementGroup = statementGroup;

		final JdbcServices jdbcServices = jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getJdbcServices();
		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();

		this.batchSizeToUse = batchSizeToUse;

		if ( BATCH_LOGGER.isTraceEnabled() ) {
			BATCH_LOGGER.tracef(
					"Created Batch (%s) - `%s`",
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
	public void addToBatch(JdbcValueBindings jdbcValueBindings, TableInclusionChecker inclusionChecker) {
		final boolean loggerTraceEnabled = BATCH_LOGGER.isTraceEnabled();
		if ( loggerTraceEnabled ) {
			BATCH_LOGGER.tracef(
					"Adding to JDBC batch (%s) - `%s`",
					batchPosition + 1,
					getKey().toLoggableString()
			);
		}

		try {
			getStatementGroup().forEachStatement( (tableName, statementDetails) -> {
				if ( inclusionChecker != null && !inclusionChecker.include( statementDetails.getMutatingTableDetails() ) ) {
					if ( loggerTraceEnabled ) {
						MODEL_MUTATION_LOGGER.tracef(
								"Skipping addBatch for table : %s (batch-position=%s)",
								statementDetails.getMutatingTableDetails().getTableName(),
								batchPosition+1
						);
					}
					return;
				}

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
			batchPosition = 0;
			batchExecuted = true;
		}
	}

	protected void releaseStatements() {
		statementGroup.forEachStatement( (tableName, statementDetails) -> {
			if ( statementDetails.getStatement() == null ) {
				BATCH_LOGGER.debugf(
						"PreparedStatementDetails did not contain PreparedStatement on #releaseStatements : %s",
						statementDetails.getSqlString()
				);
				return;
			}

			clearBatch( statementDetails );
		} );

		statementGroup.release();
		jdbcCoordinator.afterStatementExecution();
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
		if ( getStatementGroup().getNumberOfStatements() == 0 ) {
			return;
		}

		try {
			if ( batchPosition == 0 ) {
				if( !batchExecuted) {
					if ( BATCH_LOGGER.isDebugEnabled() ) {
						BATCH_LOGGER.debugf(
								"No batched statements to execute - %s",
								getKey().toLoggableString()
						);
					}
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

	protected void performExecution() {
		if ( BATCH_LOGGER.isTraceEnabled() ) {
			BATCH_LOGGER.tracef(
					"Executing JDBC batch (%s / %s) - `%s`",
					batchPosition,
					batchSizeToUse,
					getKey().toLoggableString()
			);
		}

		//noinspection deprecation
		final JdbcSessionOwner jdbcSessionOwner = jdbcCoordinator.getJdbcSessionOwner();
		final JdbcObserver observer = jdbcSessionOwner.getJdbcSessionContext().getObserver();
		try {
			getStatementGroup().forEachStatement( (tableName, statementDetails) -> {
				final String sql = statementDetails.getSqlString();
				final PreparedStatement statement = statementDetails.getStatement();

				if ( statement == null ) {
					return;
				}

				try {
					if ( statementDetails.getMutatingTableDetails().isIdentifierTable() ) {
						final int[] rowCounts;
						final EventManager eventManager = jdbcSessionOwner.getEventManager();
						final HibernateMonitoringEvent jdbcBatchExecutionEvent = eventManager.beginJdbcBatchExecutionEvent();
						try {
							observer.jdbcExecuteBatchStart();
							rowCounts = statement.executeBatch();
						}
						finally {
							eventManager.completeJdbcBatchExecutionEvent( jdbcBatchExecutionEvent, sql );
							observer.jdbcExecuteBatchEnd();
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
			} );
		}
		finally {
			batchPosition = 0;
		}
	}

	private void checkRowCounts(int[] rowCounts, PreparedStatementDetails statementDetails) throws SQLException, HibernateException {
		final int numberOfRowCounts = rowCounts.length;
		if ( batchPosition != 0 ) {
			if ( numberOfRowCounts != batchPosition ) {
				JDBC_MESSAGE_LOGGER.unexpectedRowCounts(
						statementDetails.getMutatingTableDetails().getTableName(),
						numberOfRowCounts,
						batchPosition
				);
			}
		}

		for ( int i = 0; i < numberOfRowCounts; i++ ) {
			statementDetails.getExpectation().verifyOutcome( rowCounts[i], statementDetails.getStatement(), i, statementDetails.getSqlString() );
		}
	}

	@Override
	public void release() {
		if ( BATCH_MESSAGE_LOGGER.isInfoEnabled() ) {
			final PreparedStatementGroup statementGroup = getStatementGroup();
			if ( statementGroup.getNumberOfStatements() != 0 ) {
				if ( statementGroup.hasMatching( (statementDetails) -> statementDetails.getStatement() != null ) ) {
					BATCH_MESSAGE_LOGGER.batchContainedStatementsOnRelease();
				}
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
