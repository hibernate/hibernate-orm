/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Tracks invalidation of "query spaces" (tables) for the purpose of
 * determining if a cached query result set is stale. Implementations
 * use a {@linkplain TimestampsRegion special region} the second-level
 * cache to store invalidation timestamps.
 * <ul>
 * <li>A query space is {@linkplain #invalidate invalidated} in the
 *     {@code TimestampsCache} when a SQL DML statement executed by
 *     Hibernate affects the corresponding table.
 * <li>A cached query result set is {@linkplain #isUpToDate checked for
 *     staleness} against the {@code TimestampsCache} when it is read
 *     from a {@link QueryResultsRegion} by a {@link QueryResultsCache}.
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface TimestampsCache {
	/**
	 * The region used to store all timestamp data, or null of the timestamp
	 * cache is not enabled.
	 *
	 * @deprecated This method is never called.
	 */
	@Nullable
	TimestampsRegion getRegion();

	/**
	 * Perform pre-invalidation of the passed spaces (table names)
	 * against the timestamp region data.
	 */
	void preInvalidate(
			@Nonnull String[] spaces,
			@Nonnull SharedSessionContractImplementor session);

	/**
	 * Perform invalidation of the passed spaces (table names)
	 * against the timestamp region data.
	 */
	void invalidate(
			@Nonnull String[] spaces,
			@Nonnull SharedSessionContractImplementor session);

	/**
	 * Perform an up-to-date check for the given set of query spaces as
	 * part of verifying the validity of cached query results.
	 */
	boolean isUpToDate(
			@Nonnull String[] spaces,
			@Nonnull Long timestamp,
			@Nonnull SharedSessionContractImplementor session);

	/**
	 * Perform an up-to-date check for the given set of query spaces as
	 * part of verifying the validity of cached query results.
	 */
	boolean isUpToDate(
			@Nonnull Collection<String> spaces,
			@Nonnull Long timestamp,
			@Nonnull SharedSessionContractImplementor session);

	void clear() throws CacheException;

	/**
	 * @deprecated This noop method is never called
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	default void destroy() {
		// nothing to do - the region itself is destroyed
	}

}
