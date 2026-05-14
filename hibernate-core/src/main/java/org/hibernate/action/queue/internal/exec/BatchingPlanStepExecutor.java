/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.exec;

import org.hibernate.action.queue.spi.plan.FlushOperation;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.StaleStateMapper;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.function.Consumer;

/// @author Steve Ebersole
public class BatchingPlanStepExecutor extends AbstractStepExecutor {
	private final int batchSize;

	private StatementShapeKey batchKey;
	private int currentBatchIndex;

	private Batch batch;
	private FlushOperation[] batchOperations;

	private Consumer<Object> newlyManagedEntityConsumer;
	private Consumer<FlushOperation> fixupOperationConsumer;

	public BatchingPlanStepExecutor(int batchSize, SharedSessionContractImplementor session) {
		super(session);
		this.batchSize = batchSize;
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
		batch = session.getJdbcCoordinator().getBatch(
				operationShapeKey,
				batchSize,
				preparable
		);
		batchOperations = new FlushOperation[batchSize];
	}

	private void applyToBatch(
			PreparableMutationOperation preparable,
			FlushOperation flushOperation) {
		if ( currentBatchIndex > 0 ) {
			session.getJdbcServices().getSqlStatementLogger().logStatement( preparable.getSqlString() );
		}

		var valueBindings = new JdbcValueBindings( flushOperation.getMutatingTableDescriptor(), preparable );
		flushOperation.getBindPlan().bindValues( valueBindings, flushOperation, session );

		batchOperations[currentBatchIndex] = flushOperation;

		batch.addToBatch(
				new BatchJdbcValueBindings( valueBindings, session ),
				null,
				buildStaleStateMapper(
						flushOperation.getBindPlan().getOperationResultChecker(),
						currentBatchIndex,
						preparable.getSqlString()
				)
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
						? null
						: staleStateException;
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
		final FlushOperation[] operations = batchOperations;
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
		final FlushOperation[] operations = batchOperations;
		if ( batchCount > operations.length ) {
			throw new AssertionFailure( "Expecting at most " + operations.length + " batched operations; but got " + batchCount );
		}
		for ( int i = 0; i < batchCount; i++ ) {
			super.afterOperationExecution( operations[i], newlyManagedEntityConsumer, fixupOperationConsumer );
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

	private static class BatchJdbcValueBindings implements org.hibernate.engine.jdbc.mutation.JdbcValueBindings {
		private final JdbcValueBindings valueBindings;
		private final SharedSessionContractImplementor session;

		private BatchJdbcValueBindings(
				JdbcValueBindings valueBindings,
				SharedSessionContractImplementor session) {
			this.valueBindings = valueBindings;
			this.session = session;
		}

		@Override
		public BindingGroup getBindingGroup(String tableName) {
			final BindingGroup bindingGroup = valueBindings.getBindingGroup();
			return bindingGroup.getTableName().equals( tableName ) ? bindingGroup : null;
		}

		@Override
		public void bindValue(Object value, String tableName, String columnName, ParameterUsage usage) {
			if ( !valueBindings.getBindingGroup().getTableName().equals( tableName ) ) {
				throw new IllegalArgumentException( "Unexpected table binding `" + tableName + "`" );
			}
			valueBindings.bindValue( value, columnName, usage );
		}

		@Override
		public void beforeStatement(PreparedStatementDetails statementDetails) {
			valueBindings.beforeStatement( statementDetails.resolveStatement(), session );
		}

		@Override
		public void afterStatement(TableMapping mutatingTable) {
		}
	}
}
