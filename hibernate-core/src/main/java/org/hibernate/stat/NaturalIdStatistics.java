/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Statistics pertaining to the execution of the "natural id resolution" query.
 *
 * @apiNote The natural-id resolution data is allowed to be stored in the
 * second level cache, and if so stored will have available caching stats as
 * well available via {@link Statistics#getDomainDataRegionStatistics} using the
 * configured region name
 *
 * todo (6.0) : consider a means to get the cache Region statistics for:
 * 		1) an entity by name
 * 		2) a collection by role
 * 		3) a natural-id by entity name
 *
 * @author Eric Dalquist
 * @author Steve Ebersole
 */
public interface NaturalIdStatistics extends CacheableDataStatistics, Serializable {
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
}
