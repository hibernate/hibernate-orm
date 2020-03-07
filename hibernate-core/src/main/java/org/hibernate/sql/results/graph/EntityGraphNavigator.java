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
public interface EntityGraphNavigator {

	/**
	 * Pojo class to store the result of applied entity graph navigation, including
	 * <ul>
	 *     <li>previous entity graph node so later on navigator can backtrack to it</li>
	 *     <li>whether the new graph node should be eagerly loaded or not</li>
	 *     <li>whether the new graph node fetching is joined</li>
	 * </ul>
	 */
	class NavigateResult {

		private GraphImplementor previousContext;
		private FetchTiming fetchTiming;
		private boolean joined;

		public NavigateResult(GraphImplementor previousContext, FetchTiming fetchTiming, boolean joined) {
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
	 * Backtrack to previous entity graph status after the current children navigating has been done.
	 * Mainly reset the current context entity graph node to the passed method parameter.
	 *
	 * @param previousContext The stored previous invocation result; should not be null
	 * @see #navigateIfApplicable(FetchParent, Fetchable, boolean)
	 */
	void backtrack(GraphImplementor previousContext);

	/**
	 * Tries to navigate from parent to child node within entity graph and returns non-null {@code NavigateResult}
	 * if applicable. Returns null value if not applicable.
	 *
	 * @apiNote If applicable, internal state will be mutated. Not thread safe and should be used within single thread.
	 * @param parent The FetchParent
	 * @param fetchable The Fetchable
	 * @param exploreKeySubgraph true if only key sub graph is explored; false if key sub graph is excluded
	 * @return {@link NavigateResult} if applicable; null otherwise
	 */
	NavigateResult navigateIfApplicable(FetchParent parent, Fetchable fetchable, boolean exploreKeySubgraph);
}
