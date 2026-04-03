/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;


import org.hibernate.action.queue.constraint.Constraint;
import org.hibernate.action.queue.constraint.ForeignKey;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;

/**
 * Test utilities for creating graph structures
 *
 * @author Steve Ebersole
 */
public class GraphTestUtils {

	// Empty SelectableMappings for testing
	private static final SelectableMappings EMPTY_SELECTABLES = new SelectableMappings() {
		@Override
		public int getJdbcTypeCount() {
			return 0;
		}

		@Override
		public SelectableMapping getSelectable(int columnIndex) {
			throw new IndexOutOfBoundsException("No selectables in empty instance");
		}

		@Override
		public int forEachSelectable(int offset, SelectableConsumer consumer) {
			return 0;
		}
	};

	/**
	 * Create a GraphEdge for testing purposes.
	 * This is in the same package as GraphEdge, so it can access the package-private constructor.
	 */
	public static GraphEdge createEdge(
			GroupNode targetNode,
			GroupNode keyNode,
			GroupNode from,
			GroupNode to,
			boolean breakable,
			int breakCost,
			SelectableMappings childColumnsToNull,
			Constraint constraint,
			long stableId) {
		return new GraphEdge(targetNode, keyNode, from, to, breakable, breakCost, childColumnsToNull, constraint, stableId);
	}

	/**
	 * Create a simple breakable edge with default parameters
	 */
	public static GraphEdge createBreakableEdge(GroupNode from, GroupNode to, int breakCost) {
		// Create a dummy ForeignKey for testing
		ForeignKey fk = new ForeignKey("key_table", "target_table", EMPTY_SELECTABLES, EMPTY_SELECTABLES,
				ForeignKey.TargetType.NON_UNIQUE, true, true, false);
		return createEdge(from, to, from, to, true, breakCost, EMPTY_SELECTABLES, fk, System.nanoTime());
	}

	/**
	 * Create an unbreakable edge
	 */
	public static GraphEdge createUnbreakableEdge(GroupNode from, GroupNode to) {
		// Create a dummy ForeignKey for testing
		ForeignKey fk = new ForeignKey("key_table", "target_table", EMPTY_SELECTABLES, EMPTY_SELECTABLES,
				ForeignKey.TargetType.NON_UNIQUE, true, false, false);
		return createEdge(from, to, from, to, false, Integer.MAX_VALUE, EMPTY_SELECTABLES, fk, System.nanoTime());
	}
}
