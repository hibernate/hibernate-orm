package org.hibernate.action.queue.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.action.queue.internal.constraint.ConstraintModel;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.PlanningOptions;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.spi.SessionImplementor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests collection-mutation isolation during graph operation grouping.
///
/// @author Steve Ebersole
class FlushCoordinatorCollectionGroupingTest {
	@Test
	void noOpCompletionCarriersRemainMutationLocal() {
		final var tableDescriptor = mock( TableDescriptor.class );
		when( tableDescriptor.name() ).thenReturn( "collection_table" );

		final var firstCarrier = new FlushOperation(
				tableDescriptor,
				MutationKind.NO_OP,
				null,
				null,
				100,
				"first"
		);
		final var secondCarrier = new FlushOperation(
				tableDescriptor,
				MutationKind.NO_OP,
				null,
				null,
				1_100,
				"second"
		);

		final var constraintModel = new ConstraintModel(
				List.of(),
				List.of(),
				Map.of(),
				Map.of(),
				Map.of(),
				Set.of(),
				Set.of()
		);

		final var coordinator = new FlushCoordinator(
				constraintModel,
				new PlanningOptions( false, false, false, false, PlanningOptions.UniqueCycleStrategy.FAIL ),
				mock( SessionImplementor.class )
		);
		final var groups = coordinator.groupOperations( List.of( firstCarrier, secondCarrier ) );

		assertEquals( 2, groups.size() );
		assertEquals( List.of( firstCarrier ), groups.get( 0 ).operations() );
		assertEquals( List.of( secondCarrier ), groups.get( 1 ).operations() );
	}
}
