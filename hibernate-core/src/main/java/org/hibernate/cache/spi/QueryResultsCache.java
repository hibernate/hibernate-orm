/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Responsible for managing query result list caching in a specific
 * {@linkplain QueryResultsRegion query cache region}. There may be
 * multiple instances of {@code QueryResultsCache}, corresponding to
 * second-level cache regions with distinct policies.
 * <p>
 * A {@code QueryResultsCache} depends on the {@link TimestampsCache}
 * to track invalidation of the query spaces (tables) which affect the
 * cached queries. A cached query result list is considered <em>stale</em>
 * if any one of the query spaces which affect the query results was
 * {@linkplain TimestampsCache#invalidate invalidated} since the result
 * list was read from the database and {@linkplain #put stored} in the
 * query result cache.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface QueryResultsCache {
	/**
	 * The underlying cache region being used.
	 */
	QueryResultsRegion getRegion();

	/**
	 * Store a result list of a query with the given {@link QueryKey}
	 * in the query result cache.
	 *
	 * @param key The cache key uniquely identifying the query and its
	 *            bound parameter arguments
	 * @param result The result list to cache
	 * @param session The originating session
	 *
	 * @return Whether the put actually happened.
	 *
	 * @throws HibernateException Indicates a problem delegating to the underlying cache.
	 */
	boolean put(
			QueryKey key,
			List<?> result,
			SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Attempt to retrieve a cached query result list for the given
	 * {@link QueryKey} from the {@linkplain QueryResultsRegion cache
	 * region}, and then {@linkplain TimestampsCache#isUpToDate check}
	 * if the cached results, if any, are stale. If there is no cached
	 * result list for the given key, or if the cached results are stale,
	 * return {@code null}.
	 *
	 * @param key The cache key uniquely identifying the query and its
	 *            bound parameter arguments
	 * @param spaces The query spaces which affect the results of the
	 *               query (used to check if cached results are stale)
	 * @param session The originating session
	 *
	 * @return The cached results; may be null if there are no cached
	 *         results for the given key, or if the results are stale.
	 *
	 * @throws HibernateException Indicates a problem delegating to the underlying cache.
	 */
	List<?> get(
			QueryKey key,
			Set<String> spaces,
			SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Attempt to retrieve a cached query result list for the given
	 * {@link QueryKey} from the {@linkplain QueryResultsRegion cache
	 * region}, and then {@linkplain TimestampsCache#isUpToDate check}
	 * if the cached results, if any, are stale. If there is no cached
	 * result list for the given key, or if the cached results are stale,
	 * return {@code null}.
	 *
	 * @param key The cache key uniquely identifying the query and its
	 *            bound parameter arguments
	 * @param spaces The query spaces which affect the results of the
	 *               query (used to check if cached results are stale)
	 * @param session The originating session
	 *
	 * @return The cached results; may be null.
	 *
	 * @throws HibernateException Indicates a problem delegating to the underlying cache.
	 */
	List<?> get(
			QueryKey key,
			String[] spaces,
			SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Clear all items from this query result cache.
	 *
	 * @throws CacheException Indicates a problem delegating to the underlying cache.
	 */
	default void clear() throws CacheException {
		getRegion().clear();
	}

	default void destroy() {
		// nothing to do, the region itself gets destroyed
	}
}
