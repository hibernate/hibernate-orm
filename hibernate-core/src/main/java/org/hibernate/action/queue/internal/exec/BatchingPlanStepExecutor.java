/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.exec;

import jakarta.persistence.EntityExistsException;

import org.hibernate.action.queue.spi.plan.FlushOperation;

import org.hibernate.AssertionFailure;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.bind.GroupedRowBindPlan;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.engine.jdbc.batch.spi.SingleStatementBatch;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;
import org.hibernate.sql.spi.mutation.SelfExecutingUpdateOperation;

import java.util.function.Consumer;

/// PlanStepExecutor with support for JDBC batching.
///
/// @author Steve Ebersole
public class BatchingPlanStepExecutor extends AbstractStepExecutor implements BatchObserver {
	private final int batchSize;

	private StatementShapeKey batchKey;
	private int currentBatchIndex;

	private SingleStatementBatch batch;
	private final FlushOperation[] batchOperations;
	private final boolean[] batchOperationCompletions;
	private PreparableMutationOperation reusableValueBindingsOperation;
	private JdbcValueBindings reusableValueBindings;

	private Consumer<Object> newlyManagedEntityConsumer;
	private Consumer<FlushOperation> fixupOperationConsumer;

	public BatchingPlanStepExecutor(int batchSize, SharedSessionContractImplementor session) {
		super(session);
		this.batchSize = batchSize;
		this.batchOperations = new FlushOperation[batchSize];
		this.batchOperationCompletions = new boolean[batchSize];
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
		if ( flushOperation.getBindPlan() instanceof GroupedRowBindPlan groupedRowBindPlan ) {
			final int bindingCount = groupedRowBindPlan.getBindingCount();
			for ( int bindingIndex = 0; bindingIndex < bindingCount; bindingIndex++ ) {
				prepareBatch( flushOperation, preparable );
				applyToBatch(
						preparable,
						flushOperation,
						groupedRowBindPlan,
						bindingIndex,
						bindingIndex == bindingCount - 1
				);
			}
			return;
		}

		prepareBatch( flushOperation, preparable );
		applyToBatch( preparable, flushOperation, flushOperation.getBindPlan(), -1, true );
	}

	private void prepareBatch(FlushOperation flushOperation, PreparableMutationOperation preparable) {
		final StatementShapeKey operationShapeKey = flushOperation.getShapeKey();
		if ( batchKey == null ) {
			newBatch( operationShapeKey, preparable );
		}
		else if ( !batchKey.equals( operationShapeKey ) || currentBatchIndex >= batchSize ) {
			executeBatch();
			newBatch( operationShapeKey, preparable );
		}

	}

	@Override
	protected boolean beforeOperationExecution(FlushOperation flushOperation) {
		if ( flushOperation.getPreExecutionCallback() != null && batchKey != null ) {
			executeBatch();
		}
		return super.beforeOperationExecution( flushOperation );
	}

	@Override
	protected void beforePhysicalExecution(FlushOperation flushOperation) {
		// Preparable operations begin monitoring when their JDBC batch actually
		// executes. Generated-value and self-executing operations begin in their
		// specialized execution methods after any preceding batch is drained.
	}

	@Override
	protected void afterOperationExecution(
			FlushOperation flushOperation,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<FlushOperation> fixupOperationConsumer) {
		final boolean operationIsNoop = flushOperation.getKind() == MutationKind.NO_OP;
		if ( operationIsNoop && batchKey != null ) {
			executeBatch();
		}
		if ( operationIsNoop
				|| flushOperation.isExecutionSkipped()
				|| flushOperation.getBindPlan().getGeneratedValuesCollector() != null
				|| !(flushOperation.getJdbcOperation() instanceof PreparableMutationOperation) ) {
			super.afterOperationExecution( flushOperation, newlyManagedEntityConsumer, fixupOperationConsumer );
		}
	}

	private void newBatch(StatementShapeKey operationShapeKey, PreparableMutationOperation preparable) {
		batchKey = operationShapeKey;
		currentBatchIndex = 0;
		reusableValueBindingsOperation = null;
		reusableValueBindings = null;
		batch = session.getJdbcCoordinator().getSingleStatementBatch( operationShapeKey, batchSize, preparable );
		batch.addObserver( this );
	}

	private void applyToBatch(
			PreparableMutationOperation preparable,
			FlushOperation flushOperation,
			org.hibernate.action.queue.spi.bind.BindPlan bindPlan,
			int bindingIndex,
			boolean completesOperation) {
		var valueBindings = getReusableValueBindings( preparable, flushOperation );
		if ( bindingIndex >= 0 && bindPlan instanceof GroupedRowBindPlan groupedRowBindPlan ) {
			groupedRowBindPlan.bindValues( bindingIndex, valueBindings, flushOperation, session );
		}
		else {
			bindPlan.bindValues( valueBindings, flushOperation, session );
		}

		batchOperations[currentBatchIndex] = flushOperation;
		batchOperationCompletions[currentBatchIndex] = completesOperation;

		final var resultChecker = flushOperation.getOperationResultChecker();
		try {
			batch.addToBatch(
					valueBindings::beforeStatement,
					resultChecker == null ? null : resultChecker::checkResult
			);
		}
		catch (ConstraintViolationException cve) {
			runFailureCallbacks( currentBatchIndex + 1 );
			throw convertBatchException( cve, currentBatchIndex + 1 );
		}
		catch (RuntimeException | Error failure) {
			runFailureCallbacks( currentBatchIndex + 1 );
			throw failure;
		}
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
			try {
				batch.execute();
			}
			catch (ConstraintViolationException cve) {
				runFailureCallbacks( batchCount );
				throw convertBatchException( cve, batchCount );
			}
			catch (RuntimeException | Error failure) {
				runFailureCallbacks( batchCount );
				throw failure;
			}
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

	private void runFailureCallbacks(int batchCount) {
		for ( int i = 0; i < batchCount; i++ ) {
			final FlushOperation operation = batchOperations[i];
			if ( operation != null ) {
				afterFailedExecution( operation );
			}
		}
	}

	private void runBeforeBatchCallbacks(int batchCount) {
		for ( int i = 0; i < batchCount; i++ ) {
			final FlushOperation operation = batchOperations[i];
			if ( operation != null ) {
				super.beforePhysicalExecution( operation );
			}
		}
	}

	@Override
	public void batchExplicitlyExecuted() {
		runBeforeBatchCallbacks( currentBatchIndex );
	}

	@Override
	public void batchImplicitlyExecuted() {
		runBeforeBatchCallbacks( currentBatchIndex + 1 );
	}

	private RuntimeException convertBatchException(ConstraintViolationException cve, int batchCount) {
		return session.getFactory().getSessionFactoryOptions().isJpaBootstrap()
			&& cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE
			&& hasEntityInsert( batchCount )
				? new EntityExistsException( cve )
				: cve;
	}

	private boolean hasEntityInsert(int batchCount) {
		for ( int i = 0; i < batchCount; i++ ) {
			final var operation = batchOperations[i];
			if ( operation != null
					&& operation.getKind() == MutationKind.INSERT
					&& operation.getBindPlan().getEntityInstance() != null ) {
				return true;
			}
		}
		return false;
	}

	private void runPostBatchCallbacks(int batchCount) {
		final FlushOperation[] operations = batchOperations;
		if ( batchCount > operations.length ) {
			throw new AssertionFailure( "Expecting at most " + operations.length + " batched operations; but got " + batchCount );
		}
		// The whole JDBC batch has succeeded. Complete every physical monitor
		// before running any lifecycle callback, since a lifecycle failure must
		// not leave another successfully executed operation's span open.
		for ( int i = 0; i < batchCount; i++ ) {
			if ( batchOperationCompletions[i] ) {
				afterSuccessfulPhysicalExecution( operations[i] );
			}
		}
		for ( int i = 0; i < batchCount; i++ ) {
			final FlushOperation operation = operations[i];
			operations[i] = null;
			if ( batchOperationCompletions[i] ) {
				afterOperationCompletion( operation, newlyManagedEntityConsumer, fixupOperationConsumer );
				batchOperationCompletions[i] = false;
			}
		}
	}

	@Override
	protected void executeWithGeneratedValues(FlushOperation flushOperation) {
		if ( batchKey != null ) {
			executeBatch();
		}
		super.beforePhysicalExecution( flushOperation );
		super.executeWithGeneratedValues( flushOperation );
	}

	@Override
	protected void executeSelfExecuting(SelfExecutingUpdateOperation selfExecuting, FlushOperation flushOperation) {
		if ( batchKey != null ) {
			executeBatch();
		}
		super.beforePhysicalExecution( flushOperation );
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
