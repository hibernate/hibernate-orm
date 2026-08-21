/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.exec;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.GroupedRowBindPlan;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.bind.OperationExecutionMonitor;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.spi.CollectionMutationId;
import org.hibernate.action.queue.internal.decompose.collection.CollectionMutationCompletion;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.batch.spi.BatchedResultChecker;
import org.hibernate.engine.jdbc.batch.spi.SingleStatementBatch;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.PreparableMutationOperation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests physical execution monitoring through the batching executor.
///
/// @author Steve Ebersole
class BatchingExecutionMonitorTest {
	@Test
	void monitoringSurroundsBatchExecutionRatherThanBatchPopulation() {
		final var session = mock( SessionImplementor.class );
		final var jdbcCoordinator = mock( JdbcCoordinator.class );
		final var logicalConnection = mock( LogicalConnectionImplementor.class );
		when( session.getJdbcCoordinator() ).thenReturn( jdbcCoordinator );
		when( jdbcCoordinator.getLogicalConnection() ).thenReturn( logicalConnection );
		when( logicalConnection.getPhysicalConnection() ).thenReturn( mock( Connection.class ) );

		final var tableDescriptor = mock( TableDescriptor.class );
		final var shapeKey = mock( StatementShapeKey.class );
		final var jdbcOperation = mock( PreparableMutationOperation.class );
		final var bindPlan = mock( BindPlan.class );
		doReturn( List.of( mock( JdbcParameterBinder.class ) ) ).when( jdbcOperation ).getParameterBinders();
		final var operation = new FlushOperation(
				tableDescriptor,
				MutationKind.UPDATE,
				jdbcOperation,
				bindPlan,
				0,
				"collection update",
				shapeKey
		);
		final var executionMonitor = mock( OperationExecutionMonitor.class );
		operation.setExecutionMonitor( executionMonitor );
		final var completionHandler = mock( PostExecutionCallback.class );
		final var mutationCompletion = new CollectionMutationCompletion(
				new CollectionMutationId( 1 ),
				null
		);
		mutationCompletion.registerOperation( operation );
		mutationCompletion.registerCompletionHandler( completionHandler );
		mutationCompletion.seal( session );

		final var batch = mock( SingleStatementBatch.class );
		when( jdbcCoordinator.getSingleStatementBatch( shapeKey, 5, jdbcOperation ) ).thenReturn( batch );
		final var observer = new AtomicReference<BatchObserver>();
		doAnswer( invocation -> {
			observer.set( invocation.getArgument( 0 ) );
			return null;
		} ).when( batch ).addObserver( org.mockito.ArgumentMatchers.any() );
		doAnswer( invocation -> {
			verify( executionMonitor, never() ).beforeExecution( session );
			verify( completionHandler, never() ).handle( session );
			observer.get().batchExplicitlyExecuted();
			verify( executionMonitor ).beforeExecution( session );
			return null;
		} ).when( batch ).execute();

		new BatchingPlanStepExecutor( 5, session ).execute( List.of( operation ), null, null );

		verify( executionMonitor ).afterSuccessfulExecution( session );
		verify( executionMonitor, never() ).afterFailedExecution( session );
		verify( completionHandler ).handle( session );
	}

	@Test
	void failedBatchSuppressesSemanticCompletion() {
		final var session = mock( SessionImplementor.class );
		final var jdbcCoordinator = mock( JdbcCoordinator.class );
		final var logicalConnection = mock( LogicalConnectionImplementor.class );
		when( session.getJdbcCoordinator() ).thenReturn( jdbcCoordinator );
		when( jdbcCoordinator.getLogicalConnection() ).thenReturn( logicalConnection );
		when( logicalConnection.getPhysicalConnection() ).thenReturn( mock( Connection.class ) );

		final var shapeKey = mock( StatementShapeKey.class );
		final var jdbcOperation = mock( PreparableMutationOperation.class );
		doReturn( List.of( mock( JdbcParameterBinder.class ) ) ).when( jdbcOperation ).getParameterBinders();
		final var operation = new FlushOperation(
				mock( TableDescriptor.class ),
				MutationKind.UPDATE,
				jdbcOperation,
				mock( BindPlan.class ),
				0,
				"collection update",
				shapeKey
		);
		final var completionHandler = mock( PostExecutionCallback.class );
		final var mutationCompletion = new CollectionMutationCompletion(
				new CollectionMutationId( 1 ),
				null
		);
		mutationCompletion.registerOperation( operation );
		mutationCompletion.registerCompletionHandler( completionHandler );
		mutationCompletion.seal( session );

		final var batch = mock( SingleStatementBatch.class );
		when( jdbcCoordinator.getSingleStatementBatch( shapeKey, 5, jdbcOperation ) ).thenReturn( batch );
		final var observer = new AtomicReference<BatchObserver>();
		doAnswer( invocation -> {
			observer.set( invocation.getArgument( 0 ) );
			return null;
		} ).when( batch ).addObserver( org.mockito.ArgumentMatchers.any() );
		doAnswer( invocation -> {
			observer.get().batchExplicitlyExecuted();
			throw new IllegalStateException( "expected batch failure" );
		} ).when( batch ).execute();

		assertThatThrownBy(
				() -> new BatchingPlanStepExecutor( 5, session ).execute( List.of( operation ), null, null )
		).isInstanceOf( IllegalStateException.class );

		verify( completionHandler, never() ).handle( session );
		assertThat( mutationCompletion.getState() ).isEqualTo( CollectionMutationCompletion.State.FAILED );
	}

	@Test
	void groupedRowsRetainPerRowBindingAndResultChecksWithOneCompletion() throws SQLException {
		final var session = mock( SessionImplementor.class );
		final var sessionFactory = mock( SessionFactoryImplementor.class );
		when( session.getFactory() ).thenReturn( sessionFactory );
		final var jdbcCoordinator = mock( JdbcCoordinator.class );
		final var logicalConnection = mock( LogicalConnectionImplementor.class );
		when( session.getJdbcCoordinator() ).thenReturn( jdbcCoordinator );
		when( jdbcCoordinator.getLogicalConnection() ).thenReturn( logicalConnection );
		when( logicalConnection.getPhysicalConnection() ).thenReturn( mock( Connection.class ) );

		final var tableDescriptor = mock( TableDescriptor.class );
		final var shapeKey = mock( StatementShapeKey.class );
		final var jdbcOperation = mock( PreparableMutationOperation.class );
		doReturn( List.of( mock( JdbcParameterBinder.class ) ) ).when( jdbcOperation ).getParameterBinders();
		final var bindPlan = new RecordingGroupedBindPlan( 4 );
		final var operation = new FlushOperation(
				tableDescriptor,
				MutationKind.INSERT,
				jdbcOperation,
				bindPlan,
				0,
				"grouped collection inserts",
				shapeKey
		);
		final var completionHandler = mock( PostExecutionCallback.class );
		final var completion = new CollectionMutationCompletion( new CollectionMutationId( 2 ), null );
		completion.registerOperation( operation );
		completion.registerCompletionHandler( completionHandler );
		completion.seal( session );

		final var batch = mock( SingleStatementBatch.class );
		when( jdbcCoordinator.getSingleStatementBatch( shapeKey, 5, jdbcOperation ) ).thenReturn( batch );
		final var observer = new AtomicReference<BatchObserver>();
		final var resultCheckers = new ArrayList<BatchedResultChecker>();
		doAnswer( invocation -> {
			observer.set( invocation.getArgument( 0 ) );
			return null;
		} ).when( batch ).addObserver( org.mockito.ArgumentMatchers.any() );
		doAnswer( invocation -> {
			resultCheckers.add( invocation.getArgument( 1 ) );
			return null;
		} ).when( batch ).addToBatch(
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any()
		);
		doAnswer( invocation -> {
			observer.get().batchExplicitlyExecuted();
			for ( int i = 0; i < resultCheckers.size(); i++ ) {
				resultCheckers.get( i ).checkResult( 1, i, "insert", sessionFactory );
			}
			return null;
		} ).when( batch ).execute();

		new BatchingPlanStepExecutor( 5, session ).execute( List.of( operation ), null, null );

		assertThat( bindPlan.boundIndexes ).containsExactly( 0, 1, 2, 3 );
		assertThat( bindPlan.checkedIndexes ).containsExactly( 0, 1, 2, 3 );
		verify( completionHandler ).handle( session );
		assertThat( completion.getState() ).isEqualTo( CollectionMutationCompletion.State.COMPLETED );
	}

	private static class RecordingGroupedBindPlan implements GroupedRowBindPlan, OperationResultChecker {
		private final int bindingCount;
		private final List<Integer> boundIndexes = new ArrayList<>();
		private final List<Integer> checkedIndexes = new ArrayList<>();

		private RecordingGroupedBindPlan(int bindingCount) {
			this.bindingCount = bindingCount;
		}

		@Override
		public int getBindingCount() {
			return bindingCount;
		}

		@Override
		public void bindValues(
				int bindingIndex,
				JdbcValueBindings valueBindings,
				FlushOperation flushOperation,
				SharedSessionContractImplementor session) {
			boundIndexes.add( bindingIndex );
		}

		@Override
		public boolean checkResult(
				int affectedRowCount,
				int batchPosition,
				String sqlString,
				SessionFactoryImplementor sessionFactory) {
			checkedIndexes.add( batchPosition );
			return true;
		}
	}
}
