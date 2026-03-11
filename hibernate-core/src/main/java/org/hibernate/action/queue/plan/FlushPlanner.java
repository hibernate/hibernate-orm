/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.graph.Graph;

/// Creates an executable plan from the dependency graph.
/// Applies topological sort with cycle breaking.
/// The resulting FlushPlan defines ordered steps and cycle-break fixups.
///
/// @see CycleBreaker
/// @see TopographicalSorter
///
/// @author Steve Ebersole
public interface FlushPlanner {
	FlushPlan plan(Graph graph);
}
