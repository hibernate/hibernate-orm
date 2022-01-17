/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchTiming;
import org.hibernate.graph.AttributeNode;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;

/**
 * State used as part of applying entity graphs to
 * Hibernate {@link DomainResult} / {@link Fetch} load graphs.
 *
 * @author Nathan Xu
 */
@Incubating
public interface EntityGraphTraversalState {

	/**
	 * Details of a particular traversal within the entity graph
	 */
	class TraversalResult {
		private final GraphImplementor<?> previousContext;
		private final FetchTiming fetchTiming;
		private final boolean joined;

		public TraversalResult(GraphImplementor<?> previousContext, FetchTiming fetchTiming, boolean joined) {
			this.previousContext = previousContext;
			this.fetchTiming = fetchTiming;
			this.joined = joined;
		}

		public GraphImplementor<?> getGraph() {
			return previousContext;
		}

		public FetchTiming getFetchTiming() {
			return fetchTiming;
		}

		public boolean isJoined() {
			return joined;
		}
	}

	/**
	 * Traverses to the next part of the Jakarta Persistence entity graph relating to the
	 * given fetchable.
	 * <p/>
	 *
	 * The {@link AttributeNode} corresponding to the given `fetchable` is resolved.
	 *
	 * Depending on `exploreKeySubgraph`, either {@link AttributeNode#getSubGraphs()}
	 * or {@link AttributeNode#getKeySubGraphs()} will be used
	 */
	TraversalResult traverse(FetchParent parent, Fetchable fetchable, boolean exploreKeySubgraph);

	/**
	 * Backtrack to previous entity graph status before last traversal.
	 * Mainly reset the current context entity graph node to the passed method parameter.
	 *
	 * @param previousContext The previous entity graph context node; should not be null
	 * @see #traverse(FetchParent, Fetchable, boolean)
	 */
	void backtrack(TraversalResult previousContext);
}
