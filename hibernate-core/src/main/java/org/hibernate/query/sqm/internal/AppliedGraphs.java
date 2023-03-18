/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.spi.QueryOptions;

/**
 * @author Réda Housni Alaoui
 */
public class AppliedGraphs {

	private AppliedGraphs() {
	}

	public static boolean containsCollectionFetches(QueryOptions queryOptions) {
		final AppliedGraph appliedGraph = queryOptions.getAppliedGraph();
		return appliedGraph != null && appliedGraph.getGraph() != null && containsCollectionFetches(appliedGraph.getGraph());
	}

	private static boolean containsCollectionFetches(GraphImplementor<?> graph) {
		for (AttributeNodeImplementor<?> attributeNodeImplementor : graph.getAttributeNodeImplementors()) {
			PersistentAttribute<?, ?> attributeDescriptor = attributeNodeImplementor.getAttributeDescriptor();
			if (attributeDescriptor.isCollection()) {
				return true;
			}
			for (SubGraphImplementor<?> subGraph : attributeNodeImplementor.getSubGraphMap().values()) {
				if (containsCollectionFetches(subGraph)) {
					return true;
				}
			}
		}
		return false;
	}
}
