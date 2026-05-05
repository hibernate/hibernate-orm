/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.plan.PlannedOperationGroup;

/// Wraps a PlannedOperationGroup with a stable identifier for graph operations.
/// Allows for efficient lookups, comparisons, and deduplication in the graph structure.
/// Used as nodes (vertices) in the graph and as keys for [GraphEdge] lookups.
///
/// @param group The PlannedOperationGroup
/// @param stableId Provides a unique, stable identifier for each group that persists
/// 	throughout graph operations.
///
/// @author Steve Ebersole
public record GroupNode(PlannedOperationGroup group, long stableId) {
}
