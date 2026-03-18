/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.planner;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.JdbcValueBindings;
import org.hibernate.action.queue.graph.Graph;
import org.hibernate.action.queue.graph.GraphEdge;
import org.hibernate.action.queue.graph.GraphTestUtils;
import org.hibernate.action.queue.graph.GroupNode;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.action.queue.plan.FlushPlan;
import org.hibernate.action.queue.plan.PlanStep;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.queue.plan.StandardFlushPlanner;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link StandardFlushPlanner}
 *
 * @author Steve Ebersole
 */
public class StandardFlushPlannerTest {

	// Default planning options for tests
	private static final PlanningOptions DEFAULT_PLANNING_OPTIONS = new PlanningOptions(
			true,  // orderByForeignKeys
			true,  // orderByUniqueKeySlots
			false, // avoidBreakingDeferrable
			true,  // ignoreDeferrableForOrdering
			PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
	);

	@Test
	public void testEmptyGraph() {
		// Test planning with an empty graph
		final Graph graph = new Graph(List.of(), Map.of());
		final StandardFlushPlanner planner = new StandardFlushPlanner(DEFAULT_PLANNING_OPTIONS);

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);
		assertTrue(plan.steps().isEmpty(), "Empty graph should produce empty plan");
	}

	@Test
	public void testSingleNodeGraph() {
		// Test planning with a single node (no dependencies)
		final PlannedOperationGroup group1 = createGroup("table1", MutationKind.INSERT, 1);
		final GroupNode node1 = new GroupNode(group1, 1L);

		final Graph graph = new Graph(List.of(node1), Map.of());
		final StandardFlushPlanner planner = new StandardFlushPlanner( DEFAULT_PLANNING_OPTIONS );

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);
		assertEquals(1, plan.steps().size(), "Single node should produce one step");
		assertEquals(1, plan.steps().get(0).operations().size(), "Step should contain one operation");
	}

	@Test
	public void testAcyclicGraph() {
		// Test planning with acyclic graph: A -> B -> C
		final PlannedOperationGroup groupA = createGroup("tableA", MutationKind.INSERT, 1);
		final PlannedOperationGroup groupB = createGroup("tableB", MutationKind.INSERT, 2);
		final PlannedOperationGroup groupC = createGroup("tableC", MutationKind.INSERT, 3);

		final GroupNode nodeA = new GroupNode(groupA, 1L);
		final GroupNode nodeB = new GroupNode(groupB, 2L);
		final GroupNode nodeC = new GroupNode(groupC, 3L);

		// A -> B -> C (A must execute before B, B before C)
		final GraphEdge edgeAB = createEdge(nodeA, nodeB, true, 1);
		final GraphEdge edgeBC = createEdge(nodeB, nodeC, true, 2);

		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();
		outgoing.put(nodeA, List.of(edgeAB));
		outgoing.put(nodeB, List.of(edgeBC));
		outgoing.put(nodeC, List.of());

		final Graph graph = new Graph(List.of(nodeA, nodeB, nodeC), outgoing);
		final StandardFlushPlanner planner = new StandardFlushPlanner( DEFAULT_PLANNING_OPTIONS );

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);
		assertFalse(plan.steps().isEmpty(), "Plan should have steps");

		// Verify topological order - A before B before C
		final List<PlannedOperation> allOps = new ArrayList<>();
		for ( PlanStep step : plan.steps()) {
			allOps.addAll(step.operations());
		}

		assertEquals(3, allOps.size(), "Should have 3 operations");
		// A should come before B
		int indexA = findOperationIndex(allOps, "tableA");
		int indexB = findOperationIndex(allOps, "tableB");
		int indexC = findOperationIndex(allOps, "tableC");

		assertTrue(indexA < indexB, "A should come before B");
		assertTrue(indexB < indexC, "B should come before C");
	}

	@Test
	public void testCyclicGraphBreaking() {
		// Test planning with cyclic graph: A -> B -> A (cycle)
		final PlannedOperationGroup groupA = createGroup("tableA", MutationKind.INSERT, 1);
		final PlannedOperationGroup groupB = createGroup("tableB", MutationKind.INSERT, 2);

		final GroupNode nodeA = new GroupNode(groupA, 1L);
		final GroupNode nodeB = new GroupNode(groupB, 2L);

		// A -> B, B -> A (cycle)
		final GraphEdge edgeAB = createEdge(nodeA, nodeB, true, 1);
		final GraphEdge edgeBA = createEdge(nodeB, nodeA, true, 2);

		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();
		outgoing.put(nodeA, List.of(edgeAB));
		outgoing.put(nodeB, List.of(edgeBA));

		final Graph graph = new Graph(List.of(nodeA, nodeB), outgoing);
		final StandardFlushPlanner planner = new StandardFlushPlanner( DEFAULT_PLANNING_OPTIONS );

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);
		assertFalse(plan.steps().isEmpty(), "Plan should have steps");

		// At least one edge should be broken
		assertTrue(edgeAB.isBroken() || edgeBA.isBroken(), "At least one edge should be broken");

		// Verify binding patch was installed on the child INSERT operations
		boolean patchFound = false;
		for ( PlanStep step : plan.steps()) {
			for (PlannedOperation op : step.operations()) {
				System.out.printf( "Operation : %s -> %s\n", op.getKind(), op.getTableExpression() );
				if (op.getBindingPatch() != null) {
					patchFound = true;
					break;
				}
			}
		}
		assertTrue(patchFound, "Binding patch should be installed for cycle breaking");


	}

	@Test
	public void testSelfLoopBreaking() {
		// Test planning with self-referencing node: A -> A
		final PlannedOperationGroup groupA = createGroup("tableA", MutationKind.INSERT, 1);
		final GroupNode nodeA = new GroupNode(groupA, 1L);

		// A -> A (self loop)
		final GraphEdge selfEdge = createEdge(nodeA, nodeA, true, 1);

		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();
		outgoing.put(nodeA, List.of(selfEdge));

		final Graph graph = new Graph(List.of(nodeA), outgoing);
		final StandardFlushPlanner planner = new StandardFlushPlanner( DEFAULT_PLANNING_OPTIONS );

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);
		assertTrue(selfEdge.isBroken(), "Self-loop edge should be broken");

		// Verify operation has binding patch
		final PlannedOperation op = groupA.operations().get(0);
		assertNotNull(op.getBindingPatch(), "Self-loop should install binding patch");
	}

	@Test
	public void testMultipleSCCs() {
		// Test graph with multiple strongly connected components
		// SCC1: A -> B -> A (cycle)
		// SCC2: C -> D -> C (cycle)
		// A -> C (dependency between SCCs)

		final PlannedOperationGroup groupA = createGroup("tableA", MutationKind.INSERT, 1);
		final PlannedOperationGroup groupB = createGroup("tableB", MutationKind.INSERT, 2);
		final PlannedOperationGroup groupC = createGroup("tableC", MutationKind.INSERT, 3);
		final PlannedOperationGroup groupD = createGroup("tableD", MutationKind.INSERT, 4);

		final GroupNode nodeA = new GroupNode(groupA, 1L);
		final GroupNode nodeB = new GroupNode(groupB, 2L);
		final GroupNode nodeC = new GroupNode(groupC, 3L);
		final GroupNode nodeD = new GroupNode(groupD, 4L);

		final GraphEdge edgeAB = createEdge(nodeA, nodeB, true, 1);
		final GraphEdge edgeBA = createEdge(nodeB, nodeA, true, 2);
		final GraphEdge edgeAC = createEdge(nodeA, nodeC, true, 3);
		final GraphEdge edgeCD = createEdge(nodeC, nodeD, true, 4);
		final GraphEdge edgeDC = createEdge(nodeD, nodeC, true, 5);

		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();
		outgoing.put(nodeA, List.of(edgeAB, edgeAC));
		outgoing.put(nodeB, List.of(edgeBA));
		outgoing.put(nodeC, List.of(edgeCD));
		outgoing.put(nodeD, List.of(edgeDC));

		final Graph graph = new Graph(List.of(nodeA, nodeB, nodeC, nodeD), outgoing);
		final StandardFlushPlanner planner = new StandardFlushPlanner(DEFAULT_PLANNING_OPTIONS);

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);
		assertFalse(plan.steps().isEmpty(), "Plan should have steps");

		// Both SCCs should have at least one broken edge
		int brokenCount = 0;
		if (edgeAB.isBroken()) brokenCount++;
		if (edgeBA.isBroken()) brokenCount++;
		if (edgeCD.isBroken()) brokenCount++;
		if (edgeDC.isBroken()) brokenCount++;

		assertTrue(brokenCount >= 2, "At least 2 edges should be broken (one per SCC)");
	}

	@Test
	public void testStatementShapeGrouping() {
		// Test that operations with same shape are grouped together
		final StatementShapeKey shapeKey1 = new StatementShapeKey("table1", MutationKind.INSERT, 100);

		final PlannedOperation op1 = createOperation("table1", MutationKind.INSERT, shapeKey1, 1);
		final PlannedOperation op2 = createOperation("table1", MutationKind.INSERT, shapeKey1, 2);
		final PlannedOperation op3 = createOperation("table1", MutationKind.INSERT, shapeKey1, 3);

		final PlannedOperationGroup group1 = new PlannedOperationGroup(
				"table1",
				MutationKind.INSERT,
				shapeKey1,
				List.of(op1, op2, op3),
				false,
				1,
				"test"
		);

		final GroupNode node1 = new GroupNode(group1, 1L);

		final Graph graph = new Graph(List.of(node1), Map.of());
		final StandardFlushPlanner planner = new StandardFlushPlanner(DEFAULT_PLANNING_OPTIONS);

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);
		assertEquals(1, plan.steps().size(), "Operations with same shape should be in one step");
		assertEquals(3, plan.steps().get(0).operations().size(), "Step should contain all 3 operations");
	}

	@Test
	public void testStatementShapeSeparation() {
		// Test that operations with different shapes are in different steps
		final StatementShapeKey shapeKey1 = new StatementShapeKey("table1", MutationKind.INSERT, 100);
		final StatementShapeKey shapeKey2 = new StatementShapeKey("table1", MutationKind.INSERT, 200); // different hash
		final StatementShapeKey shapeKey3 = new StatementShapeKey("table2", MutationKind.INSERT, 100); // different table

		final PlannedOperation op1 = createOperation("table1", MutationKind.INSERT, shapeKey1, 1);
		final PlannedOperation op2 = createOperation("table1", MutationKind.INSERT, shapeKey2, 2);
		final PlannedOperation op3 = createOperation("table2", MutationKind.INSERT, shapeKey3, 3);

		final PlannedOperationGroup group1 = new PlannedOperationGroup(
				"table1", MutationKind.INSERT, shapeKey1, List.of(op1), false, 1, "test"
		);
		final PlannedOperationGroup group2 = new PlannedOperationGroup(
				"table1", MutationKind.INSERT, shapeKey2, List.of(op2), false, 2, "test"
		);
		final PlannedOperationGroup group3 = new PlannedOperationGroup(
				"table2", MutationKind.INSERT, shapeKey3, List.of(op3), false, 3, "test"
		);

		final GroupNode node1 = new GroupNode(group1, 1L);
		final GroupNode node2 = new GroupNode(group2, 2L);
		final GroupNode node3 = new GroupNode(group3, 3L);

		final Graph graph = new Graph(List.of(node1, node2, node3), Map.of());
		final StandardFlushPlanner planner = new StandardFlushPlanner(DEFAULT_PLANNING_OPTIONS);

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);
		assertTrue(plan.steps().size() >= 2, "Operations with different shapes should be in separate steps");
	}

	@Test
	public void testDifferentMutationKinds() {
		// Test planning with different mutation kinds (INSERT, UPDATE, DELETE)
		final PlannedOperationGroup insertGroup = createGroup("table1", MutationKind.INSERT, 1);
		final PlannedOperationGroup updateGroup = createGroup("table1", MutationKind.UPDATE, 2);
		final PlannedOperationGroup deleteGroup = createGroup("table2", MutationKind.DELETE, 3);

		final GroupNode insertNode = new GroupNode(insertGroup, 1L);
		final GroupNode updateNode = new GroupNode(updateGroup, 2L);
		final GroupNode deleteNode = new GroupNode(deleteGroup, 3L);

		final Graph graph = new Graph(List.of(insertNode, updateNode, deleteNode), Map.of());
		final StandardFlushPlanner planner = new StandardFlushPlanner(DEFAULT_PLANNING_OPTIONS);

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);
		assertFalse(plan.steps().isEmpty(), "Plan should have steps");

		// Verify all operations are present
		int totalOps = 0;
		for ( PlanStep step : plan.steps()) {
			totalOps += step.operations().size();
		}
		assertEquals(3, totalOps, "All operations should be in the plan");
	}

	@Test
	public void testUnbreakableCycleThrowsException() {
		// Test that unbreakable cycle throws exception
		final PlannedOperationGroup groupA = createGroup("tableA", MutationKind.INSERT, 1);
		final PlannedOperationGroup groupB = createGroup("tableB", MutationKind.INSERT, 2);

		final GroupNode nodeA = new GroupNode(groupA, 1L);
		final GroupNode nodeB = new GroupNode(groupB, 2L);

		// A -> B, B -> A with both edges marked as unbreakable
		final GraphEdge edgeAB = createEdge(nodeA, nodeB, false, 1); // not breakable
		final GraphEdge edgeBA = createEdge(nodeB, nodeA, false, 2); // not breakable

		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();
		outgoing.put(nodeA, List.of(edgeAB));
		outgoing.put(nodeB, List.of(edgeBA));

		final Graph graph = new Graph(List.of(nodeA, nodeB), outgoing);
		final StandardFlushPlanner planner = new StandardFlushPlanner(DEFAULT_PLANNING_OPTIONS);

		// Should throw IllegalStateException for unbreakable cycle
		assertThrows(IllegalStateException.class, () -> planner.plan(graph));
	}

	@Test
	public void testBreakCostPreference() {
		// Test that edges with lower break cost are preferred when breaking cycles
		final PlannedOperationGroup groupA = createGroup("tableA", MutationKind.INSERT, 1);
		final PlannedOperationGroup groupB = createGroup("tableB", MutationKind.INSERT, 2);

		final GroupNode nodeA = new GroupNode(groupA, 1L);
		final GroupNode nodeB = new GroupNode(groupB, 2L);

		// A -> B (high cost), B -> A (low cost)
		final GraphEdge edgeAB = createEdge(nodeA, nodeB, true, 100); // high cost
		final GraphEdge edgeBA = createEdge(nodeB, nodeA, true, 1);   // low cost

		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();
		outgoing.put(nodeA, List.of(edgeAB));
		outgoing.put(nodeB, List.of(edgeBA));

		final Graph graph = new Graph(List.of(nodeA, nodeB), outgoing);
		final StandardFlushPlanner planner = new StandardFlushPlanner(DEFAULT_PLANNING_OPTIONS);

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);

		// The low-cost edge should be broken (prefer breaking BA over AB)
		assertTrue(edgeBA.isBroken(), "Lower cost edge should be broken");
		assertFalse(edgeAB.isBroken(), "Higher cost edge should not be broken");
	}

	@Test
	public void testComplexGraph() {
		// Test planning with a more complex graph
		// A -> B -> D
		// A -> C -> D
		// (diamond pattern)

		final PlannedOperationGroup groupA = createGroup("tableA", MutationKind.INSERT, 1);
		final PlannedOperationGroup groupB = createGroup("tableB", MutationKind.INSERT, 2);
		final PlannedOperationGroup groupC = createGroup("tableC", MutationKind.INSERT, 3);
		final PlannedOperationGroup groupD = createGroup("tableD", MutationKind.INSERT, 4);

		final GroupNode nodeA = new GroupNode(groupA, 1L);
		final GroupNode nodeB = new GroupNode(groupB, 2L);
		final GroupNode nodeC = new GroupNode(groupC, 3L);
		final GroupNode nodeD = new GroupNode(groupD, 4L);

		final GraphEdge edgeAB = createEdge(nodeA, nodeB, true, 1);
		final GraphEdge edgeAC = createEdge(nodeA, nodeC, true, 2);
		final GraphEdge edgeBD = createEdge(nodeB, nodeD, true, 3);
		final GraphEdge edgeCD = createEdge(nodeC, nodeD, true, 4);

		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();
		outgoing.put(nodeA, List.of(edgeAB, edgeAC));
		outgoing.put(nodeB, List.of(edgeBD));
		outgoing.put(nodeC, List.of(edgeCD));
		outgoing.put(nodeD, List.of());

		final Graph graph = new Graph(List.of(nodeA, nodeB, nodeC, nodeD), outgoing);
		final StandardFlushPlanner planner = new StandardFlushPlanner(DEFAULT_PLANNING_OPTIONS);

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);
		assertFalse(plan.steps().isEmpty(), "Plan should have steps");

		// Verify topological order - A before B and C, B and C before D
		final List<PlannedOperation> allOps = new ArrayList<>();
		for ( PlanStep step : plan.steps()) {
			allOps.addAll(step.operations());
		}

		int indexA = findOperationIndex(allOps, "tableA");
		int indexB = findOperationIndex(allOps, "tableB");
		int indexC = findOperationIndex(allOps, "tableC");
		int indexD = findOperationIndex(allOps, "tableD");

		assertTrue(indexA < indexB, "A should come before B");
		assertTrue(indexA < indexC, "A should come before C");
		assertTrue(indexB < indexD, "B should come before D");
		assertTrue(indexC < indexD, "C should come before D");
	}

	@Test
	public void testBindingPatchOnlyOnInserts() {
		// Test that binding patches are only installed on INSERT operations, not UPDATE or DELETE
		final PlannedOperationGroup groupA = createGroup("tableA", MutationKind.UPDATE, 1);
		final PlannedOperationGroup groupB = createGroup("tableB", MutationKind.UPDATE, 2);

		final GroupNode nodeA = new GroupNode(groupA, 1L);
		final GroupNode nodeB = new GroupNode(groupB, 2L);

		// A -> B, B -> A (cycle with UPDATE operations)
		final GraphEdge edgeAB = createEdge(nodeA, nodeB, true, 1);
		final GraphEdge edgeBA = createEdge(nodeB, nodeA, true, 2);

		final Map<GroupNode, List<GraphEdge>> outgoing = new HashMap<>();
		outgoing.put(nodeA, List.of(edgeAB));
		outgoing.put(nodeB, List.of(edgeBA));

		final Graph graph = new Graph(List.of(nodeA, nodeB), outgoing);
		final StandardFlushPlanner planner = new StandardFlushPlanner(DEFAULT_PLANNING_OPTIONS);

		final FlushPlan plan = planner.plan(graph);

		assertNotNull(plan);

		// Cycle should be broken, but no binding patch should be installed (not INSERT)
		assertTrue(edgeAB.isBroken() || edgeBA.isBroken(), "Cycle should be broken");

		// Verify no binding patch on UPDATE operations
		for ( PlanStep step : plan.steps()) {
			for (PlannedOperation op : step.operations()) {
				if (op.getKind() == MutationKind.UPDATE) {
					assertNull(op.getBindingPatch(), "UPDATE operations should not have binding patch");
				}
			}
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Helper methods
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private PlannedOperationGroup createGroup(String tableName, MutationKind kind, int ordinal) {
		return createGroup(
				makeTableDescriptor( tableName ),
				kind,
				ordinal
		);
	}

	private static TableDescriptor makeTableDescriptor(String name) {
		return new EntityTableDescriptor(
				name,
				0,
				true,
				false,
				false,
				false,
				null,
				null,
				null,
				List.of(),
				List.of(),
				Map.of(),
				null
		);
	}

	private PlannedOperationGroup createGroup(TableDescriptor tableDescriptor, MutationKind kind, int ordinal) {
		final StatementShapeKey shapeKey = new StatementShapeKey(tableDescriptor.name(), kind, ordinal);
		final PlannedOperation op = createOperation(tableDescriptor, kind, shapeKey, ordinal);

		return new PlannedOperationGroup(
				tableDescriptor.name(),
				kind,
				shapeKey,
				List.of(op),
				false,
				ordinal,
				"test-origin"
		);
	}

	private PlannedOperation createOperation(
			String tableName,
			MutationKind kind,
			StatementShapeKey shapeKey,
			int ordinal) {
		return createOperation( makeTableDescriptor( tableName ), kind, shapeKey, ordinal );
	}

	private PlannedOperation createOperation(
			TableDescriptor tableDescriptor,
			MutationKind kind,
			StatementShapeKey shapeKey,
			int ordinal) {
		return new PlannedOperation(
				tableDescriptor,
				kind,
				new MockMutationOperation(),
				new MockBindPlan(),
				ordinal,
				"test-operation"
		);
	}

	private GraphEdge createEdge(GroupNode from, GroupNode to, boolean breakable, long stableId) {
		if (breakable) {
			return GraphTestUtils.createBreakableEdge(from, to, 10);
		} else {
			return GraphTestUtils.createUnbreakableEdge(from, to);
		}
	}

	private GraphEdge createEdge(GroupNode from, GroupNode to, boolean breakable, int breakCost) {
		if (breakable) {
			return GraphTestUtils.createBreakableEdge(from, to, breakCost);
		} else {
			return GraphTestUtils.createUnbreakableEdge(from, to);
		}
	}

	private int findOperationIndex(List<PlannedOperation> ops, String tableName) {
		for (int i = 0; i < ops.size(); i++) {
			if (ops.get(i).getTableExpression().equals(tableName)) {
				return i;
			}
		}
		return -1;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mock classes
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static class MockMutationOperation implements JdbcOperation {
		@Override
		public MutationType getMutationType() {
			return MutationType.INSERT;
		}

		@Override
		public GraphMutationTarget<?> getMutationTarget() {
			return null;
		}

		@Override
		public Expectation getExpectation() {
			return null;
		}

		@Override
		public TableDescriptor getTableDescriptor() {
			return null;
		}

		@Override
		public JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage) {
			return null;
		}
	}

	private static class MockBindPlan implements BindPlan {
		@Override
		public void bindValues(
				JdbcValueBindings valueBindings,
				PlannedOperation plannedOperation,
				SharedSessionContractImplementor session) {
			// no-op for testing
		}
	}
}
