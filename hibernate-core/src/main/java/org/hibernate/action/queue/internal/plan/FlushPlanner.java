/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.plan;


import org.hibernate.action.queue.internal.constraint.DeferrableConstraintMode;
import org.hibernate.action.queue.internal.graph.Graph;

/// Creates an executable plan from the dependency graph.
/// Applies topological sort with cycle breaking.
/// The resulting FlushPlan defines ordered steps and cycle-break fixups.
///
/// See [CycleBreaker].
/// See [TopographicalSorter].
///
/// @author Steve Ebersole
public interface FlushPlanner {
	default FlushPlan plan(Graph graph) {
		return plan( graph, DeferrableConstraintMode.DEFAULT );
	}

	FlushPlan plan(Graph graph, DeferrableConstraintMode deferrableConstraintMode);
}
