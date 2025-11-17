/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Statistics relating to a particular query written in HQL or SQL.
 * <p>
 * Note that for a cached query, the number of cache misses is the
 * same as the number of queries actually executed against the
 * database.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface QueryStatistics extends Serializable {
	/**
	 * How many times has this query been executed?
	 */
	long getExecutionCount();

	/**
	 * How many {@link java.sql.ResultSet} rows have been processed
	 * for this query
	 */
	long getExecutionRowCount();

	/**
	 * What is the average amount time taken to execute this query?
	 */
	long getExecutionAvgTime();

	/**
	 * What is the max amount time taken to execute this query?
	 */
	long getExecutionMaxTime();

	/**
	 * What is the min amount time taken to execute this query?
	 */
	long getExecutionMinTime();

	/**
	 * How long, cumulatively, have all executions of this query
	 * taken?
	 */
	long getExecutionTotalTime();

	double getExecutionAvgTimeAsDouble();

	/**
	 * The number of cache hits for this query.
	 *
	 * @apiNote Note that a query can be saved into different
	 * regions at different times.  This value represents the
	 * sum total across all of those regions
	 */
	long getCacheHitCount();

	/**
	 * The number of cache misses for this query
	 *
	 * @apiNote Note that a query can be saved into different
	 * regions at different times.  This value represents the
	 * sum total across all of those regions
	 */
	long getCacheMissCount();

	/**
	 * The number of cache puts for this query
	 *
	 * @apiNote Note that a query can be saved into different
	 * regions at different times.  This value represents the
	 * sum total across all of those regions
	 */
	long getCachePutCount();

	/**
	 * The number of query plans successfully fetched from the
	 * cache.
	 */
	default long getPlanCacheHitCount() {
		//For backward compatibility
		return 0;
	}

	/**
	 * The number of query plans *not* fetched from the cache.
	 */
	default long getPlanCacheMissCount(){
		//For backward compatibility
		return 0;
	}

	/**
	 * The overall time spent to compile the plan for this
	 * particular query.
	 */
	default long getPlanCompilationTotalMicroseconds() {
		//For backward compatibility
		return 0;
	}
}
