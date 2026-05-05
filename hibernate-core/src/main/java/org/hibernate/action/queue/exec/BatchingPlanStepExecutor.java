/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.StaleStateMapper;
import org.hibernate.engine.jdbc.mutation.internal.JdbcValueBindingsImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public class BatchingPlanStepExecutor extends AbstractStepExecutor implements ExecutionContext {
	private final int batchSize;

	private StatementShapeKey batchKey;
	private int currentBatchIndex;

	private Batch batch;
	private PlannedOperation[] batchOperations;

	private Consumer<Object> newlyManagedEntityConsumer;
	private Consumer<PlannedOperation> fixupOperationConsumer;

	public BatchingPlanStepExecutor(int batchSize, SharedSessionContractImplementor session) {
		super(session);
		this.batchSize = batchSize;
	}

	@Override
	public void execute(
			java.util.List<PlannedOperation> plannedOperations,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<PlannedOperation> fixupOperationConsumer) {
		this.newlyManagedEntityConsumer = newlyManagedEntityConsumer;
		this.fixupOperationConsumer = fixupOperationConsumer;
		try {
			super.execute( plannedOperations, newlyManagedEntityConsumer, fixupOperationConsumer );
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
	protected void executePreparable(PreparableMutationOperation preparable, PlannedOperation plannedOperation) {
		plannedOperation.getBindPlan().execute( this, plannedOperation, session );
	}

	@Override
	protected boolean beforeOperationExecution(PlannedOperation plannedOperation) {
		if ( plannedOperation.getPreExecutionCallback() != null && batchKey != null ) {
			executeBatch();
		}
		return super.beforeOperationExecution( plannedOperation );
	}

	@Override
	protected void afterOperationExecution(
			PlannedOperation plannedOperation,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<PlannedOperation> fixupOperationConsumer) {
		if ( plannedOperation.getKind() == org.hibernate.action.queue.MutationKind.NO_OP && batchKey != null ) {
			executeBatch();
		}
		if ( plannedOperation.getKind() != org.hibernate.action.queue.MutationKind.NO_OP
				&& !plannedOperation.isExecutionSkipped()
				&& plannedOperation.getBindPlan().getGeneratedValuesCollector() == null
				&& plannedOperation.getJdbcOperation() instanceof PreparableMutationOperation ) {
			return;
		}
		super.afterOperationExecution( plannedOperation, newlyManagedEntityConsumer, fixupOperationConsumer );
	}

	@Override
	public void executeRow(
			PlannedOperation plannedOperation,
			BiConsumer<JdbcValueBindings, SharedSessionContractImplementor> binder,
			OperationResultChecker resultChecker) {
		assert plannedOperation.getJdbcOperation() instanceof PreparableMutationOperation;

		var preparable = (PreparableMutationOperation) plannedOperation.getJdbcOperation();
		final StatementShapeKey operationShapeKey = plannedOperation.getShapeKey();
		if ( batchKey == null ) {
			newBatch( operationShapeKey, preparable );
		}
		else if ( !batchKey.equals( operationShapeKey ) || currentBatchIndex >= batchSize) {
			executeBatch();
			newBatch( operationShapeKey, preparable );
		}

		applyToBatch( preparable, plannedOperation, binder, resultChecker );
	}

	private void newBatch(StatementShapeKey operationShapeKey, PreparableMutationOperation preparable) {
		batchKey = operationShapeKey;
		currentBatchIndex = 0;
		batch = session.getJdbcCoordinator().getBatch(
				operationShapeKey,
				batchSize,
				preparable
		);
		batchOperations = new PlannedOperation[batchSize];
	}

	private void applyToBatch(
			PreparableMutationOperation preparable,
			PlannedOperation plannedOperation,
			BiConsumer<JdbcValueBindings, SharedSessionContractImplementor> binder,
			OperationResultChecker resultChecker) {
		if ( currentBatchIndex > 0 ) {
			session.getJdbcServices().getSqlStatementLogger().logStatement( preparable.getSqlString() );
		}

		var valueBindings = new JdbcValueBindings( plannedOperation.getMutatingTableDescriptor(), preparable );
		binder.accept( valueBindings, session );

		batchOperations[currentBatchIndex] = plannedOperation;

		final var jdbcValueBindings = new JdbcValueBindingsImpl(
				preparable.getMutationType(),
				preparable.getMutationTarget(),
				preparable,
				session
		);
		valueBindings.getBindingGroup().forEachBinding( binding ->
				jdbcValueBindings.bindValue(
						binding.getValue(),
						valueBindings.getBindingGroup().getTableName(),
						binding.getColumnName(),
						binding.getValueDescriptor().getUsage()
				)
		);

		batch.addToBatch(
				jdbcValueBindings,
				null,
				buildStaleStateMapper( resultChecker, currentBatchIndex, preparable.getSqlString() )
		);
		currentBatchIndex++;

		if ( currentBatchIndex == batchSize ) {
			runPostBatchCallbacks( currentBatchIndex );
			Arrays.fill( batchOperations, null );
			currentBatchIndex = 0;
		}
	}

	private StaleStateMapper buildStaleStateMapper(
			OperationResultChecker resultChecker,
			int batchPosition,
			String sqlString) {
		if ( resultChecker == null ) {
			return null;
		}
		return staleStateException -> {
			try {
				return resultChecker.checkResult( 0, batchPosition, sqlString, session.getFactory() )
						? staleStateException
						: null;
			}
			catch (HibernateException e) {
				return e;
			}
			catch (SQLException e) {
				return session.getJdbcServices()
						.getSqlExceptionHelper()
						.convert( e, "Unable to check batched mutation result - " + sqlString );
			}
		};
	}

	private void executeBatch() {
		final int batchCount = currentBatchIndex;
		final PlannedOperation[] operations = batchOperations;
		try {
			session.getJdbcCoordinator().executeBatch();
			runPostBatchCallbacks( batchCount );
		}
		finally {
			batchKey = null;
			batch = null;
			batchOperations = null;
			currentBatchIndex = 0;
		}
	}

	private void runPostBatchCallbacks(int batchCount) {
		final PlannedOperation[] operations = batchOperations;
		if ( batchCount > operations.length ) {
			throw new AssertionFailure( "Expecting at most " + operations.length + " batched operations; but got " + batchCount );
		}
		for ( int i = 0; i < batchCount; i++ ) {
			super.afterOperationExecution( operations[i], newlyManagedEntityConsumer, fixupOperationConsumer );
		}
	}

	@Override
	protected void executeWithGeneratedValues(PlannedOperation plannedOperation) {
		if ( batchKey != null ) {
			executeBatch();
		}
		super.executeWithGeneratedValues( plannedOperation );
	}

	@Override
	protected void executeSelfExecuting(SelfExecutingUpdateOperation selfExecuting, PlannedOperation plannedOperation) {
		if ( batchKey != null ) {
			executeBatch();
		}
		super.executeSelfExecuting( selfExecuting, plannedOperation );
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
