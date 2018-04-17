/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.stat;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @deprecated (since 5.3) Use {@link NaturalIdStatistics} - unfortunately the
 * old statistics contracts exposed these by region name, rather than the name of
 * the entity defining the natural-id
 *
 * @author Steve Ebersole
 */
@Deprecated
public interface NaturalIdCacheStatistics extends Serializable {
	/**
	 * Number of times (since last Statistics clearing) the "natural id
	 * resolution" query has been executed
	 */
	long getExecutionCount();

	/**
	 * The average amount of time it takes (since last Statistics clearing) for
	 * the execution of this "natural id resolution" query
	 */
	long getExecutionAvgTime();

	/**
	 * The maximum amount of time it takes (since last Statistics clearing) for
	 * the execution of this "natural id resolution" query
	 */
	long getExecutionMaxTime();

	/**
	 * The minimum amount of time it takes (since last Statistics clearing) for
	 * the execution of this "natural id resolution" query
	 */
	long getExecutionMinTime();

	long getHitCount();

	long getMissCount();

	long getPutCount();

	long getElementCountInMemory();

	long getElementCountOnDisk();

	long getSizeInMemory();

	default Map getEntries() {
		return Collections.emptyMap();
	}
}
