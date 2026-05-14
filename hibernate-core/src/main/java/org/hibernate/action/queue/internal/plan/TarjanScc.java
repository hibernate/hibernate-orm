/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.plan;

import org.hibernate.action.queue.internal.graph.Graph;
import org.hibernate.action.queue.internal.graph.GraphEdge;
import org.hibernate.action.queue.internal.graph.GroupNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/// Applies Uses Tarjan’s Algorithm for finding strongly connected components (SCCs).
///
/// @author Steve Ebersole
public class TarjanScc {
	static List<List<GroupNode>> compute(Graph graph) {
		final int nodeCount = graph.nodes().size();
		final int[] index = new int[nodeCount];
		final int[] lowlink = new int[nodeCount];
		final boolean[] onStack = new boolean[nodeCount];
		final GroupNode[] stack = new GroupNode[nodeCount];
		final List<List<GroupNode>> out = new ArrayList<>();
		final int[] idx = { 0 };
		final int[] stackSize = { 0 };
		Arrays.fill( index, -1 );

		for ( GroupNode v : graph.nodes() ) {
			if ( index[nodeIndex( v )] == -1 ) {
				strongConnect( graph, v, idx, index, lowlink, stack, stackSize, onStack, out );
			}
		}
		return out;
	}

	private static void strongConnect(
			Graph graph,
			GroupNode v,
			int[] idx,
			int[] index,
			int[] lowlink,
			GroupNode[] stack,
			int[] stackSize,
			boolean[] onStack,
			List<List<GroupNode>> out) {
		final int vIndex = nodeIndex( v );
		index[vIndex] = idx[0];
		lowlink[vIndex] = idx[0];
		idx[0]++;

		stack[stackSize[0]++] = v;
		onStack[vIndex] = true;

		for ( GraphEdge e : graph.outgoing().getOrDefault( v, List.of() ) ) {
			if ( e.isBroken() ) {
				continue;
			}

			final GroupNode w = e.getTo();
			final int wIndex = nodeIndex( w );
			if ( index[wIndex] == -1 ) {
				strongConnect( graph, w, idx, index, lowlink, stack, stackSize, onStack, out );
				lowlink[vIndex] = Math.min( lowlink[vIndex], lowlink[wIndex] );
			}
			else if ( onStack[wIndex] ) {
				lowlink[vIndex] = Math.min( lowlink[vIndex], index[wIndex] );
			}
		}

		if ( lowlink[vIndex] == index[vIndex] ) {
			final ArrayList<GroupNode> scc = new ArrayList<>();
			GroupNode w;
			do {
				w = stack[--stackSize[0]];
				stack[stackSize[0]] = null;
				onStack[nodeIndex( w )] = false;
				scc.add( w );
			} while ( w != v );

			scc.sort( Comparator.comparingLong( GroupNode::stableId ) );
			out.add( scc );
		}
	}

	private static int nodeIndex(GroupNode node) {
		return Math.toIntExact( node.stableId() - 1 );
	}
}
