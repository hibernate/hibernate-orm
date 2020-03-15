/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import org.hibernate.engine.FetchTiming;
import org.hibernate.graph.spi.GraphImplementor;

/**
 * @author Nathan Xu
 */
public interface EntityGraphSemanticTraverser {

	/**
	 * POJO class to store the result of applied entity graph traversal, including
	 * <ul>
	 *     <li>previous entity graph node so later on traverser can backtrack to it</li>
	 *     <li>whether the new graph node should be eagerly loaded or not</li>
	 *     <li>whether the new graph node fetching is joined</li>
	 * </ul>
	 */
	class Result {

		private GraphImplementor previousContext;
		private FetchTiming fetchTiming;
		private boolean joined;

		public Result(GraphImplementor previousContext, FetchTiming fetchTiming, boolean joined) {
			this.previousContext = previousContext;
			this.fetchTiming = fetchTiming;
			this.joined = joined;
		}

		public GraphImplementor getPreviousContext() {
			return previousContext;
		}

		public FetchTiming getFetchStrategy() {
			return fetchTiming;
		}

		public boolean isJoined() {
			return joined;
		}
	}

	/**
	 * Backtrack to previous entity graph status before last travrsal.
	 * Mainly reset the current context entity graph node to the passed method parameter.
	 *
	 * @param previousContext The previous entity graph context node; should not be null
	 * @see #traverse(FetchParent, Fetchable, boolean)
	 */
	void backtrack(GraphImplementor previousContext);

	/**
	 * Tries to traverse from parent to child node within entity graph and returns non-null {@code Result}.
	 *
	 * @param parent The FetchParent or traversal source node
	 * @param fetchable The Fetchable or traversal destination node
	 * @param exploreKeySubgraph true if only key sub graph is explored; false if key sub graph is excluded
	 * @return traversal result; never be null
	 */
	Result traverse(FetchParent parent, Fetchable fetchable, boolean exploreKeySubgraph);
}
