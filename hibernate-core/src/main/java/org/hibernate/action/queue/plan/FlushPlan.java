/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/// Output of [FlushPlanner] containing
///
/// @author Steve Ebersole
public class FlushPlan {
	private final List<PlanStep> steps;
	private final ArrayDeque<PlannedOperation> fixups = new ArrayDeque<>();

	public FlushPlan(List<PlanStep> steps) {
		this.steps = List.copyOf(steps);
	}

	public List<PlanStep> steps() { return steps; }

	public void enqueueFixup(PlannedOperation fixup, PlanStep currentStep) {
		fixups.addLast(fixup);
	}

	public List<PlannedOperation> drainFixupsInOrder() {
		ArrayList<PlannedOperation> out = new ArrayList<>(fixups.size());
		while (!fixups.isEmpty()) out.add(fixups.removeFirst());
		return out;
	}

}
