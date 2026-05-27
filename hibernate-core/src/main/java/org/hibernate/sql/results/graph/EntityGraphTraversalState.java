/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchTiming;
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
		private final FetchStrategy fetchStrategy;
		private final CacheStoreMode cacheStoreMode;
		private final CacheRetrieveMode cacheRetrieveMode;

		public TraversalResult(GraphImplementor<?> previousContext, FetchStrategy fetchStrategy) {
			this( previousContext, fetchStrategy, null, null );
		}

		public TraversalResult(
				GraphImplementor<?> previousContext,
				FetchStrategy fetchStrategy,
				CacheStoreMode cacheStoreMode,
				CacheRetrieveMode cacheRetrieveMode) {
			this.previousContext = previousContext;
			this.fetchStrategy = fetchStrategy;
			this.cacheStoreMode = cacheStoreMode;
			this.cacheRetrieveMode = cacheRetrieveMode;
		}

		public GraphImplementor<?> getGraph() {
			return previousContext;
		}

		public FetchStrategy getFetchStrategy() {
			return fetchStrategy;
		}

		public CacheStoreMode getCacheStoreMode() {
			return cacheStoreMode;
		}

		public CacheRetrieveMode getCacheRetrieveMode() {
			return cacheRetrieveMode;
		}
	}

	class FetchStrategy {
		private final FetchTiming fetchTiming;
		private final boolean joined;

		public FetchStrategy(FetchTiming fetchTiming, boolean joined) {
			assert fetchTiming != null;
			this.fetchTiming = fetchTiming;
			this.joined = joined;
		}

		public FetchTiming getFetchTiming() {
			return fetchTiming;
		}

		public boolean isJoined() {
			return joined;
		}
	}

	/**
	 * Traverses to the next part of the Jakarta Persistence entity graph relating to
	 * the given {@link Fetchable}.
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
