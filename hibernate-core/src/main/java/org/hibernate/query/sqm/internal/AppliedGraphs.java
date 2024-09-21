/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
		for ( AttributeNodeImplementor<?> attributeNodeImplementor : graph.getAttributeNodeImplementors() ) {
			if ( attributeNodeImplementor.getAttributeDescriptor().isCollection() ) {
				return true;
			}
			for ( SubGraphImplementor<?> subGraph : attributeNodeImplementor.getSubGraphMap().values() ) {
				if ( containsCollectionFetches(subGraph) ) {
					return true;
				}
			}
		}
		return false;
	}
}
