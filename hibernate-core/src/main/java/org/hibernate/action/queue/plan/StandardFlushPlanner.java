/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.graph.Graph;
import org.hibernate.action.queue.graph.GroupNode;

import java.util.ArrayList;
import java.util.List;

/// Standard FlushPlanner
/// @author Steve Ebersole
public class StandardFlushPlanner implements FlushPlanner {
	public StandardFlushPlanner() {
	}

	@Override
	public FlushPlan plan(Graph graph, PlanningOptions planningOptions) {
		// detect cycles and choose edges to break.
		//		for the broken edge, apply a "binding path"
		//		which facilitates the pattern of inserting
		//		with null fk and later updating to set the
		//		actual fk value
		// DEBUG
		try {
			String msg = String.format("Calling CycleBreaker with graph containing %d nodes\\n", graph.nodes().size());
			java.nio.file.Files.write(
					java.nio.file.Paths.get("/tmp/cycle-breaker-debug.log"),
					msg.getBytes(),
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND
			);
		} catch (Exception e) {
			// ignore
		}
		new CycleBreaker().applyCycleBreaks(graph, planningOptions);
		// DEBUG
		try {
			String msg = "CycleBreaker completed\\n";
			java.nio.file.Files.write(
					java.nio.file.Paths.get("/tmp/cycle-breaker-debug.log"),
					msg.getBytes(),
					java.nio.file.StandardOpenOption.CREATE,
					java.nio.file.StandardOpenOption.APPEND
			);
		} catch (Exception e) {
			// ignore
		}

		// ---------------------------------------------------------------
		// The graph should now be acyclic.
		// Topologically sort it, ignoring any broken edges.
		final List<GroupNode> topoOrder = new TopographicalSorter().sort(graph);

		// build steps and return the overall plan
		return new FlushPlan(buildSteps(topoOrder));
	}

	private List<PlanStep> buildSteps(List<GroupNode> topoOrder) {
		final ArrayList<PlanStep> steps = new ArrayList<>();

		StatementShapeKey curKey = null;
		final ArrayList<PlannedOperation> bucket = new ArrayList<>();

		for (GroupNode n : topoOrder) {
			final StatementShapeKey key = n.group().shapeKey();

			for (PlannedOperation op : n.group().operations()) {
				if (curKey == null) {
					curKey = key;
					bucket.add(op);
				}
				else if (sameShape(curKey, key)) {
					bucket.add(op);
				}
				else {
					steps.add(new SimplePlanStep(new ArrayList<>(bucket)));
					bucket.clear();
					curKey = key;
					bucket.add(op);
				}
			}
		}

		if (!bucket.isEmpty()) {
			steps.add(new SimplePlanStep(new ArrayList<>(bucket)));
		}
		return steps;
	}

	private boolean sameShape(StatementShapeKey a, StatementShapeKey b) {
		return a != null && b != null
			&& a.tableExpression().equalsIgnoreCase(b.tableExpression())
			&& a.kind() == b.kind()
			&& a.shapeHash() == b.shapeHash();
	}
}
