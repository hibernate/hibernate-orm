/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat;

import java.io.Serializable;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public interface CacheableDataStatistics extends Serializable {
	long NOT_CACHED_COUNT = Long.MIN_VALUE;

	/**
	 * The name of the region where this data is cached.
	 */
	@Nullable String getCacheRegionName();

	/**
	 * The number of times this data has been into its configured cache region
	 * since the last Statistics clearing
	 */
	long getCachePutCount();

	/**
	 * The number of successful cache look-ups for this data from its
	 * configured cache region since the last Statistics clearing
	 */
	long getCacheHitCount();

	/**
	 * The number of unsuccessful cache look-ups for this data from its
	 * configured cache region since the last Statistics clearing
	 */
	long getCacheMissCount();

	/**
	 * The number of evictions from the configured cache region since
	 * the last Statistics clearing
	 */
	long getCacheRemoveCount();
}
