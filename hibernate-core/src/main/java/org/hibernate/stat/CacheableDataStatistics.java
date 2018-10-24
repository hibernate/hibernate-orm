/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.stat;

import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public interface CacheableDataStatistics extends Serializable {
	long NOT_CACHED_COUNT = Long.MIN_VALUE;

	/**
	 * The name of the region where this data is cached.
	 */
	String getCacheRegionName();

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
}
