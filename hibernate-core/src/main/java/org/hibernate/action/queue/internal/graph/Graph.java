/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.graph;


import java.util.List;
import java.util.Map;

/// @author Steve Ebersole
public record Graph(List<GroupNode> nodes, Map<GroupNode, List<GraphEdge>> outgoing) {

	/// Check if the graph has no edges (all operations are independent).
	/// This enables optimization: skip cycle detection and topological sort.
	public boolean isEmpty() {
		for (List<GraphEdge> edges : outgoing.values()) {
			if (!edges.isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
