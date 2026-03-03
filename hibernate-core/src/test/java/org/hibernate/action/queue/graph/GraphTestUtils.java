/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;


import java.util.Set;

/**
 * Test utilities for creating graph structures
 *
 * @author Steve Ebersole
 */
public class GraphTestUtils {

	/**
	 * Create a GraphEdge for testing purposes.
	 * This is in the same package as GraphEdge, so it can access the package-private constructor.
	 */
	public static GraphEdge createEdge(
			GroupNode from,
			GroupNode to,
			boolean breakable,
			int breakCost,
			Set<String> childColumnsToNull,
			boolean deferrable,
			long stableId) {
		return new GraphEdge(from, to, breakable, breakCost, childColumnsToNull, deferrable, stableId);
	}

	/**
	 * Create a simple breakable edge with default parameters
	 */
	public static GraphEdge createBreakableEdge(GroupNode from, GroupNode to, int breakCost) {
		return createEdge(from, to, true, breakCost, Set.of("fk_column"), false, System.nanoTime());
	}

	/**
	 * Create an unbreakable edge
	 */
	public static GraphEdge createUnbreakableEdge(GroupNode from, GroupNode to) {
		return createEdge(from, to, false, Integer.MAX_VALUE, Set.of(), false, System.nanoTime());
	}
}
