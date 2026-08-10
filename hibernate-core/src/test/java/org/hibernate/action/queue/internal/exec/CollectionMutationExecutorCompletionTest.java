/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.exec;

import java.sql.Connection;
import java.util.List;

import org.hibernate.action.queue.internal.decompose.collection.CollectionMutationCompletion;
import org.hibernate.action.queue.spi.CollectionMutationId;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
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
}
