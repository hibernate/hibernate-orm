/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.AssertionFailure;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.mutation.jdbc.PreparableJdbcOperation;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public class BatchingPlanStepExecutor extends AbstractStepPlanner {
	private final int batchSize;

	private StatementShapeKey batchKey;
	private int currentBatchIndex;

	private String batchSql;
	private PreparedStatement batchStatement;

	private OperationResultChecker[] resultCheckers;

	public BatchingPlanStepExecutor(int batchSize, SharedSessionContractImplementor session) {
		super(session);
		this.batchSize = batchSize;
	}

	@Override
	protected void executePreparable(PreparableJdbcOperation preparable, PlannedOperation plannedOperation) {
		final StatementShapeKey operationShapeKey = plannedOperation.getShapeKey();
		if ( batchKey == null ) {
			newBatch( operationShapeKey, preparable );
		}
		else if ( !batchKey.equals( operationShapeKey ) || currentBatchIndex >= batchSize) {
			executeBatch();
			newBatch( operationShapeKey, preparable );
		}

		applyToBatch( preparable, plannedOperation );
	}

	private void newBatch(StatementShapeKey operationShapeKey, PreparableJdbcOperation preparable) {
		batchKey = operationShapeKey;
		currentBatchIndex = 0;
		batchSql = preparable.getSqlString();
		batchStatement = session.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( batchSql );
		resultCheckers = new OperationResultChecker[batchSize];

		try {
			preparable.getExpectation().prepare( batchStatement );
		}
		catch (SQLException e) {
			throw session.getJdbcServices()
					.getSqlExceptionHelper()
					.convert( e, "Error preparing result expectation", batchSql );
		}
	}

	private void applyToBatch(PreparableJdbcOperation preparable, PlannedOperation plannedOperation) {
		if ( currentBatchIndex > 0 ) {
			session.getJdbcServices().getSqlStatementLogger().logStatement( preparable.getSqlString() );
		}

		var valueBindings = new JdbcValueBindings( plannedOperation.getMutatingTableDescriptor(), preparable );
		plannedOperation.getBindPlan().bindValues( valueBindings, plannedOperation, session );
		valueBindings.beforeStatement( batchStatement, session );

		try {
			batchStatement.addBatch();
		}
		catch (SQLException e) {
			throw session.getJdbcServices()
					.getSqlExceptionHelper()
					.convert( e, "Error adding to batch statement", batchSql );
		}

		resultCheckers[currentBatchIndex] = plannedOperation.getBindPlan().getOperationResultChecker();
		currentBatchIndex++;
	}

	private void executeBatch() {
		try {
			final int[] affectedRowCounts = batchStatement.executeBatch();
			if ( affectedRowCounts.length != currentBatchIndex ) {
				throw new AssertionFailure( "Expecting " + currentBatchIndex + " result-counts; but got " + affectedRowCounts.length );
			}

			// See OPTIMISTIC_LOCKING_FIX_STATUS.md for details
			for ( int i = 0; i < affectedRowCounts.length; i++ ) {
				if ( resultCheckers[i] != null ) {
					resultCheckers[i].checkResult( affectedRowCounts[i], i, batchSql, session.getFactory() );
				}
			}

			batchKey = null;
			batchSql = null;
			batchStatement = null;
			resultCheckers = null;
			currentBatchIndex = 0;
		}
		catch (SQLException e) {
			throw session.getJdbcServices()
					.getSqlExceptionHelper()
					.convert( e, "Error executing batch statement" );
		}
	}

	@Override
	public void finishUp() {
		super.finishUp();

		if ( batchKey != null ) {
			assert batchStatement != null;
			executeBatch();
		}
	}
}
