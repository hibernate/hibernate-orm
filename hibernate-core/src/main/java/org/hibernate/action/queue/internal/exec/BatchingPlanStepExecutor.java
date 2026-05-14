/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.exec;

import org.hibernate.action.queue.spi.plan.FlushOperation;

import org.hibernate.AssertionFailure;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.engine.jdbc.batch.spi.SingleStatementBatch;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;

import java.util.function.Consumer;

/// PlanStepExecutor with support for JDBC batching.
///
/// @author Steve Ebersole
public class BatchingPlanStepExecutor extends AbstractStepExecutor {
	private final int batchSize;

	private StatementShapeKey batchKey;
	private int currentBatchIndex;

	private SingleStatementBatch batch;
	private final FlushOperation[] batchOperations;
	private PreparableMutationOperation reusableValueBindingsOperation;
	private JdbcValueBindings reusableValueBindings;

	private Consumer<Object> newlyManagedEntityConsumer;
	private Consumer<FlushOperation> fixupOperationConsumer;

	public BatchingPlanStepExecutor(int batchSize, SharedSessionContractImplementor session) {
		super(session);
		this.batchSize = batchSize;
		this.batchOperations = new FlushOperation[batchSize];
	}

	@Override
	public void execute(
			java.util.List<FlushOperation> flushOperations,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<FlushOperation> fixupOperationConsumer) {
		this.newlyManagedEntityConsumer = newlyManagedEntityConsumer;
		this.fixupOperationConsumer = fixupOperationConsumer;
		try {
			super.execute( flushOperations, newlyManagedEntityConsumer, fixupOperationConsumer );
			if ( batchKey != null ) {
				assert batch != null;
				executeBatch();
			}
		}
		finally {
			this.newlyManagedEntityConsumer = null;
			this.fixupOperationConsumer = null;
		}
	}

	@Override
	protected void executePreparable(PreparableMutationOperation preparable, FlushOperation flushOperation) {
		final StatementShapeKey operationShapeKey = flushOperation.getShapeKey();
		if ( batchKey == null ) {
			newBatch( operationShapeKey, preparable );
		}
		else if ( !batchKey.equals( operationShapeKey ) || currentBatchIndex >= batchSize ) {
			executeBatch();
			newBatch( operationShapeKey, preparable );
		}

		applyToBatch( preparable, flushOperation );
	}

	@Override
	protected boolean beforeOperationExecution(FlushOperation flushOperation) {
		if ( flushOperation.getPreExecutionCallback() != null && batchKey != null ) {
			executeBatch();
		}
		return super.beforeOperationExecution( flushOperation );
	}

	@Override
	protected void afterOperationExecution(
			FlushOperation flushOperation,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<FlushOperation> fixupOperationConsumer) {
		if ( flushOperation.getKind() == org.hibernate.action.queue.spi.MutationKind.NO_OP && batchKey != null ) {
			executeBatch();
		}
		if ( flushOperation.getKind() != org.hibernate.action.queue.spi.MutationKind.NO_OP
				&& !flushOperation.isExecutionSkipped()
				&& flushOperation.getBindPlan().getGeneratedValuesCollector() == null
				&& flushOperation.getJdbcOperation() instanceof PreparableMutationOperation ) {
			return;
		}
		super.afterOperationExecution( flushOperation, newlyManagedEntityConsumer, fixupOperationConsumer );
	}

	private void newBatch(StatementShapeKey operationShapeKey, PreparableMutationOperation preparable) {
		batchKey = operationShapeKey;
		currentBatchIndex = 0;
		reusableValueBindingsOperation = null;
		reusableValueBindings = null;
		batch = session.getJdbcCoordinator().getSingleStatementBatch( operationShapeKey, batchSize, preparable );
	}

	private void applyToBatch(
			PreparableMutationOperation preparable,
			FlushOperation flushOperation) {
		var bindPlan = flushOperation.getBindPlan();
		var valueBindings = getReusableValueBindings( preparable, flushOperation );
		bindPlan.bindValues( valueBindings, flushOperation, session );

		batchOperations[currentBatchIndex] = flushOperation;

		final OperationResultChecker resultChecker = flushOperation.getOperationResultChecker();
		batch.addToBatch(
				valueBindings::beforeStatement,
				resultChecker == null ? null : resultChecker::checkResult
		);
		currentBatchIndex++;

		if ( currentBatchIndex == batchSize ) {
			try {
				runPostBatchCallbacks( currentBatchIndex );
			}
			finally {
				currentBatchIndex = 0;
			}
		}
	}

	private JdbcValueBindings getReusableValueBindings(
			PreparableMutationOperation preparable,
			FlushOperation flushOperation) {
		if ( reusableValueBindingsOperation != preparable ) {
			reusableValueBindingsOperation = preparable;
			reusableValueBindings = new JdbcValueBindings( flushOperation.getMutatingTableDescriptor(), preparable );
		}
		else {
			reusableValueBindings.clear();
		}
		return reusableValueBindings;
	}

	private void executeBatch() {
		final int batchCount = currentBatchIndex;
		try {
			batch.execute();
			runPostBatchCallbacks( batchCount );
		}
		finally {
			batch.release();
			batchKey = null;
			batch = null;
			currentBatchIndex = 0;
			reusableValueBindingsOperation = null;
			reusableValueBindings = null;
		}
	}

	private void runPostBatchCallbacks(int batchCount) {
		final FlushOperation[] operations = batchOperations;
		if ( batchCount > operations.length ) {
			throw new AssertionFailure( "Expecting at most " + operations.length + " batched operations; but got " + batchCount );
		}
		for ( int i = 0; i < batchCount; i++ ) {
			final FlushOperation operation = operations[i];
			operations[i] = null;
			super.afterOperationExecution( operation, newlyManagedEntityConsumer, fixupOperationConsumer );
		}
	}

	@Override
	protected void executeWithGeneratedValues(FlushOperation flushOperation) {
		if ( batchKey != null ) {
			executeBatch();
		}
		super.executeWithGeneratedValues( flushOperation );
	}

	@Override
	protected void executeSelfExecuting(SelfExecutingUpdateOperation selfExecuting, FlushOperation flushOperation) {
		if ( batchKey != null ) {
			executeBatch();
		}
		super.executeSelfExecuting( selfExecuting, flushOperation );
	}

	@Override
	public void finishUp() {
		super.finishUp();

		if ( batchKey != null ) {
			assert batch != null;
			executeBatch();
		}
	}
}
