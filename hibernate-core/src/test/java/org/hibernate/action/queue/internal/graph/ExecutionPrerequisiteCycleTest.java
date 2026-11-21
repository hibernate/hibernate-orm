package org.hibernate.action.queue.internal.graph;

import java.util.List;
import java.util.Map;

import org.hibernate.action.queue.internal.constraint.DeferrableConstraintMode;
import org.hibernate.action.queue.internal.plan.CycleBreaker;
import org.hibernate.action.queue.internal.plan.FlushOperationGroup;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.PlanningOptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutionPrerequisiteCycleTest {
	@Test
	void contradictoryPrerequisitesCannotBeRelaxed() {
		final var end = node( MutationKind.UPDATE, 1 );
		final var insert = node( MutationKind.INSERT, 2 );
		final var first = GraphEdge.executionPrerequisite( end, insert, 0 );
		final var second = GraphEdge.executionPrerequisite( insert, end, 1 );
		final var graph = new Graph( List.of( end, insert ), Map.of( end, List.of( first ), insert, List.of( second ) ) );
		final var options = new PlanningOptions( true, true, true, false, PlanningOptions.UniqueCycleStrategy.FAIL );
		assertThrows( IllegalStateException.class,
				() -> new CycleBreaker().applyCycleBreaks( graph, options, DeferrableConstraintMode.DEFAULT ) );
		assertFalse( first.isBroken() );
		assertFalse( second.isBroken() );
	}

	private static GroupNode node(MutationKind kind, int ordinal) {
		return new GroupNode( new FlushOperationGroup( "item", kind, null, List.of(), false, false, ordinal, "test" ), ordinal );
	}
}
