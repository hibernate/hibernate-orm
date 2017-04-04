/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

import java.io.Serializable;
import java.util.Map;

/**
 * NaturalId query statistics
 * <p/>
 * Note that for a cached natural id, the cache miss is equals to the db count
 *
 * @author Eric Dalquist
 */
public interface NaturalIdCacheStatistics extends Serializable {
	long getHitCount();

	long getMissCount();

	long getPutCount();
	
	long getExecutionCount();
	
	long getExecutionAvgTime();
	
	long getExecutionMaxTime();
	
	long getExecutionMinTime();

	long getElementCountInMemory();

	long getElementCountOnDisk();

	long getSizeInMemory();

	Map getEntries();
}
