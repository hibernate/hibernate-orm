/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.plan;

import org.hibernate.action.queue.internal.graph.Graph;
import org.hibernate.action.queue.internal.graph.GraphEdge;
import org.hibernate.action.queue.internal.graph.GroupNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/// Performs topological sorting on a dependency graph to determine the
/// correct execution order for mutation operations using Kahn's Algorithm
/// for breadth-first searching.
///
/// @author Steve Ebersole
public class TopographicalSorter {
	public List<GroupNode> sort(Graph graph) {
		// Initialize all nodes with in-degree 0
		final int[] indegree = new int[graph.nodes().size()];

		// Count incoming edges for each node
		for ( GroupNode u : graph.nodes() ) {
			for ( GraphEdge e : graph.outgoing().getOrDefault( u, List.of() ) ) {
				if ( e.isBroken() ) {
					// Skip broken edges!
					// See CycleBreaker
					continue;
				}
				indegree[nodeIndex( e.getTo() )]++;
			}
		}

		final PriorityQueue<GroupNode> q = new PriorityQueue<>( Comparator.comparingLong( GroupNode::stableId ) );
		for ( GroupNode node : graph.nodes() ) {
			if ( indegree[nodeIndex( node )] == 0 ) {
				q.add( node );
			}
		}

		final ArrayList<GroupNode> order = new ArrayList<>( graph.nodes().size() );
		while ( !q.isEmpty() ) {
			final GroupNode u = q.remove();
			order.add( u );

			for ( GraphEdge e : graph.outgoing().getOrDefault( u, List.of() ) ) {
				if ( e.isBroken() ) {
					continue;
				}
				final GroupNode to = e.getTo();
				final int toIndex = nodeIndex( to );
				if ( --indegree[toIndex] == 0 ) {
					q.add( to );
				}
			}
		}

		if ( order.size() != graph.nodes().size() ) {
			throw new IllegalStateException( "Graph still cyclic after breaks (bug)." );
		}

		return order;
	}

	private static int nodeIndex(GroupNode node) {
		return Math.toIntExact( node.stableId() - 1 );
	}
}
