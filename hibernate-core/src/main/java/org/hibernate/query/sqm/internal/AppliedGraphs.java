/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.query.spi.QueryOptions;

/**
 * @author Réda Housni Alaoui
 */
public class AppliedGraphs {

	private AppliedGraphs() {
	}

	public static boolean containsCollectionFetches(QueryOptions queryOptions) {
		final AppliedGraph appliedGraph = queryOptions.getAppliedGraph();
		return appliedGraph != null
			&& appliedGraph.getGraph() != null
			&& containsCollectionFetches( appliedGraph.getGraph() );
	}

	private static boolean containsCollectionFetches(GraphImplementor<?> graph) {
		for ( AttributeNodeImplementor<?,?,?> node : graph.getNodes().values() ) {
			if ( node.getAttributeDescriptor().isCollection() ) {
				return true;
			}
			for ( SubGraphImplementor<?> subgraph : node.getSubGraphs().values() ) {
				if ( containsCollectionFetches( subgraph ) ) {
					return true;
				}
			}
		}
		return false;
	}
}
