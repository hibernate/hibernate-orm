/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.exec;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.OperationExecutionMonitor;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.batch.spi.SingleStatementBatch;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.PreparableMutationOperation;

import org.junit.jupiter.api.Test;

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

		final var batch = mock( SingleStatementBatch.class );
		when( jdbcCoordinator.getSingleStatementBatch( shapeKey, 5, jdbcOperation ) ).thenReturn( batch );
		final var observer = new AtomicReference<BatchObserver>();
		doAnswer( invocation -> {
			observer.set( invocation.getArgument( 0 ) );
			return null;
		} ).when( batch ).addObserver( org.mockito.ArgumentMatchers.any() );
		doAnswer( invocation -> {
			verify( executionMonitor, never() ).beforeExecution( session );
			observer.get().batchExplicitlyExecuted();
			verify( executionMonitor ).beforeExecution( session );
			return null;
		} ).when( batch ).execute();

		new BatchingPlanStepExecutor( 5, session ).execute( List.of( operation ), null, null );

		verify( executionMonitor ).afterSuccessfulExecution( session );
		verify( executionMonitor, never() ).afterFailedExecution( session );
	}
}
