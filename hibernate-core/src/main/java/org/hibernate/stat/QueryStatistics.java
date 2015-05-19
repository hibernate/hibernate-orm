/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Query statistics (HQL and SQL)
 * <p/>
 * Note that for a cached query, the cache miss is equals to the db count
 *
 * @author Gavin King
 */
public interface QueryStatistics extends Serializable {
	long getExecutionCount();

	long getCacheHitCount();

	long getCachePutCount();

	long getCacheMissCount();

	long getExecutionRowCount();

	long getExecutionAvgTime();

	long getExecutionMaxTime();

	long getExecutionMinTime();
}
