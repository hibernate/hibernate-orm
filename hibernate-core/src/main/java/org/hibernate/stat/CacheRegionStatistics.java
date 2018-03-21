/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Second level cache statistics of a specific region
 *
 * @author Gavin King
 */
public interface CacheRegionStatistics extends Serializable {
	/**
	 * The value returned from {@link #getElementCountInMemory},
	 * {@link #getElementCountOnDisk()} and {@link #getSizeInMemory()}
	 * for cache providers that do not support such "extended" statistics.
	 */
	long NO_EXTENDED_STAT_SUPPORT_RETURN = Long.MIN_VALUE;

	String getRegionName();

	/**
	 * The number of cache puts into the region since the last Statistics
	 * clearing
	 */
	long getPutCount();

	/**
	 * The number of successful cache look-ups against the region since the
	 * last Statistics clearing
	 */
	long getHitCount();

	/**
	 * The number of unsuccessful cache look-ups against the region since the
	 * last Statistics clearing
	 */
	long getMissCount();

	/**
	 * The number of elements currently in memory within the cache provider.
	 *
	 * This is an optional value contingent upon the underlying cache provider
	 * providing extended stats support via
	 * {@link org.hibernate.cache.spi.ExtendedStatisticsSupport}.  If the provider
	 * does not support extended stats, {@link #NO_EXTENDED_STAT_SUPPORT_RETURN}
	 * is returned instead.
	 */
	long getElementCountInMemory();

	/**
	 * The number of elements currently stored to disk within the cache provider.
	 *
	 * This is an optional value contingent upon the underlying cache provider
	 * providing extended stats support via
	 * {@link org.hibernate.cache.spi.ExtendedStatisticsSupport}.  If the provider
	 * does not support extended stats, {@link #NO_EXTENDED_STAT_SUPPORT_RETURN}
	 * is returned instead.
	 */
	long getElementCountOnDisk();

	/**
	 * The size that the in-memory elements take up within the cache provider.
	 *
	 * This is an optional value contingent upon the underlying cache provider
	 * providing extended stats support via
	 * {@link org.hibernate.cache.spi.ExtendedStatisticsSupport}.  If the provider
	 * does not support extended stats, {@link #NO_EXTENDED_STAT_SUPPORT_RETURN}
	 * is returned instead.
	 */
	long getSizeInMemory();
}
