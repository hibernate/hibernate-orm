/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.plan;

import org.hibernate.action.queue.spi.plan.FlushOperation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/// Output of [FlushPlanner] containing
///
/// @author Steve Ebersole
public class FlushPlan {
	private final List<PlanStep> steps;

	private final ArrayDeque<FlushOperation> fixups = new ArrayDeque<>();

	public FlushPlan(List<PlanStep> steps) {
		this.steps = List.copyOf(steps);
	}

	public List<PlanStep> steps() { return steps; }

	public void enqueueFixup(FlushOperation fixup) {
		fixups.addLast(fixup);
	}

	public List<FlushOperation> drainFixupsInOrder() {
		final ArrayList<FlushOperation> out = new ArrayList<>(fixups.size());
		while (!fixups.isEmpty()) {
			out.add(fixups.removeFirst());
		}
		return out;
	}

}
