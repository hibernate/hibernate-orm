/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.exec;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.action.queue.internal.decompose.collection.CollectionMutationCompletion;
import org.hibernate.action.queue.spi.CollectionMutationId;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.GroupedRowBindPlan;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests collection mutation completion reporting from direct operation execution.
///
/// @author Steve Ebersole
class CollectionMutationExecutorCompletionTest {
	@Test
	void directExecutionReportsSuccessAfterPhysicalExecution() {
		final var fixture = new Fixture();
		final var executor = new TestingExecutor( fixture.session, false, fixture.handler );

		executor.execute( List.of( fixture.operation ), null, null );

		verify( fixture.handler ).handle( fixture.session );
		assertThat( fixture.completion.getState() ).isEqualTo( CollectionMutationCompletion.State.COMPLETED );
	}

	@Test
	void directExecutionFailureSuppressesCompletion() {
		final var fixture = new Fixture();
		final var executor = new TestingExecutor( fixture.session, true, fixture.handler );

		assertThatThrownBy( () -> executor.execute( List.of( fixture.operation ), null, null ) )
				.isInstanceOf( IllegalStateException.class );

		verify( fixture.handler, never() ).handle( fixture.session );
		assertThat( fixture.completion.getState() ).isEqualTo( CollectionMutationCompletion.State.FAILED );
	}

	@Test
	void directExecutionVisitsEveryGroupedBindingAndCompletesOnce() {
		final var session = session();
		final var handler = mock( PostExecutionCallback.class );
		final var bindPlan = new TestingGroupedBindPlan( 4 );
		final var operation = operation( bindPlan );
		final var completion = new CollectionMutationCompletion( new CollectionMutationId( 2 ), null );
		completion.registerOperation( operation );
		completion.registerCompletionHandler( handler );
		completion.seal( session );
		final var executor = new GroupedTestingExecutor( session );

		executor.execute( List.of( operation ), null, null );

		assertThat( executor.bindingIndexes ).containsExactly( 0, 1, 2, 3 );
		verify( handler ).handle( session );
		assertThat( completion.getState() ).isEqualTo( CollectionMutationCompletion.State.COMPLETED );
	}

	private static SessionImplementor session() {
		final var session = mock( SessionImplementor.class );
		final var jdbcCoordinator = mock( JdbcCoordinator.class );
		final var logicalConnection = mock( LogicalConnectionImplementor.class );
		when( session.getJdbcCoordinator() ).thenReturn( jdbcCoordinator );
		when( jdbcCoordinator.getLogicalConnection() ).thenReturn( logicalConnection );
		when( logicalConnection.getPhysicalConnection() ).thenReturn( mock( Connection.class ) );
		return session;
	}

	private static FlushOperation operation(BindPlan bindPlan) {
		return new FlushOperation(
				mock( TableDescriptor.class ),
				MutationKind.UPDATE,
				mock( PreparableMutationOperation.class ),
				bindPlan,
				0,
				"collection update",
				mock( StatementShapeKey.class )
		);
	}

	private static class Fixture {
		private final SessionImplementor session = mock( SessionImplementor.class );
		private final PostExecutionCallback handler = mock( PostExecutionCallback.class );
		private final CollectionMutationCompletion completion = new CollectionMutationCompletion(
				new CollectionMutationId( 1 ),
				null
		);
		private final FlushOperation operation;

		private Fixture() {
			final var jdbcCoordinator = mock( JdbcCoordinator.class );
			final var logicalConnection = mock( LogicalConnectionImplementor.class );
			when( session.getJdbcCoordinator() ).thenReturn( jdbcCoordinator );
			when( jdbcCoordinator.getLogicalConnection() ).thenReturn( logicalConnection );
			when( logicalConnection.getPhysicalConnection() ).thenReturn( mock( Connection.class ) );

			operation = new FlushOperation(
					mock( TableDescriptor.class ),
					MutationKind.UPDATE,
					mock( PreparableMutationOperation.class ),
					mock( BindPlan.class ),
					0,
					"collection update",
					mock( StatementShapeKey.class )
			);
			completion.registerOperation( operation );
			completion.registerCompletionHandler( handler );
			completion.seal( session );
		}
	}

	private static class TestingExecutor extends AbstractStepExecutor {
		private final boolean fail;
		private final PostExecutionCallback handler;

		private TestingExecutor(
				SessionImplementor session,
				boolean fail,
				PostExecutionCallback handler) {
			super( session );
			this.fail = fail;
			this.handler = handler;
		}

		@Override
		protected void executePreparable(
				PreparableMutationOperation preparable,
				FlushOperation flushOperation) {
			verify( handler, never() ).handle( (SessionImplementor) session );
			if ( fail ) {
				throw new IllegalStateException( "expected failure" );
			}
		}
	}

	private static class GroupedTestingExecutor extends StandardPlanStepExecutor {
		private final java.util.ArrayList<Integer> bindingIndexes = new java.util.ArrayList<>();

		private GroupedTestingExecutor(SessionImplementor session) {
			super( session );
		}

		@Override
		protected void executePreparableDirectly(
				PreparableMutationOperation preparable,
				FlushOperation flushOperation,
				int bindingIndex) {
			bindingIndexes.add( bindingIndex );
		}
	}

	private static class TestingGroupedBindPlan implements GroupedRowBindPlan, OperationResultChecker {
		private final int bindingCount;

		private TestingGroupedBindPlan(int bindingCount) {
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
				org.hibernate.engine.spi.SharedSessionContractImplementor session) {
		}

		@Override
		public boolean checkResult(
				int affectedRowCount,
				int batchPosition,
				String sqlString,
				SessionFactoryImplementor sessionFactory) throws SQLException {
			return true;
		}
	}
}
