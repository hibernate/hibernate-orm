/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

/**
 * Cache statistics pertaining to a specific data region
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SecondLevelCacheStatistics {
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
	 * This is an optional value contingent upon the underlying provider defining
	 * {@link org.hibernate.cache.spi.ExtendedStatisticsSupport}
	 */
	long getElementCountInMemory();

	/**
	 * The number of elements currently stored to disk within the cache provider.
	 *
	 * This is an optional value contingent upon the underlying provider defining
	 * {@link org.hibernate.cache.spi.ExtendedStatisticsSupport}
	 */
	long getElementCountOnDisk();

	/**
	 * The size that the in-memory elements take up within the cache provider.
	 *
	 * This is an optional value contingent upon the underlying provider defining
	 * {@link org.hibernate.cache.spi.ExtendedStatisticsSupport}
	 */
	long getSizeInMemory();
}
