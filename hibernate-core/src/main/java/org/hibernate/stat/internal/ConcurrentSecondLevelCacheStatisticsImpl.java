/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.stat.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.Region;
import org.hibernate.stat.SecondLevelCacheStatistics;

/**
 * Second level cache statistics of a specific region
 *
 * @author Alex Snaps
 */
public class ConcurrentSecondLevelCacheStatisticsImpl extends CategorizedStatistics implements SecondLevelCacheStatistics {
	private final transient Region region;
	private AtomicLong hitCount = new AtomicLong();
	private AtomicLong missCount = new AtomicLong();
	private AtomicLong putCount = new AtomicLong();

	ConcurrentSecondLevelCacheStatisticsImpl(Region region) {
		super( region.getName() );
		this.region = region;
	}

	public long getHitCount() {
		return hitCount.get();
	}

	public long getMissCount() {
		return missCount.get();
	}

	public long getPutCount() {
		return putCount.get();
	}

	public long getElementCountInMemory() {
		return region.getElementCountInMemory();
	}

	public long getElementCountOnDisk() {
		return region.getElementCountOnDisk();
	}

	public long getSizeInMemory() {
		return region.getSizeInMemory();
	}

	public Map getEntries() {
		Map map = new HashMap();
		Iterator iter = region.toMap().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry me = (Map.Entry) iter.next();
			map.put(((CacheKey) me.getKey()).getKey(), me.getValue());
		}
		return map;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder()
				.append("SecondLevelCacheStatistics")
				.append("[hitCount=").append(this.hitCount)
				.append(",missCount=").append(this.missCount)
				.append(",putCount=").append(this.putCount);
		//not sure if this would ever be null but wanted to be careful
		if (region != null) {
			buf.append(",elementCountInMemory=").append(this.getElementCountInMemory())
					.append(",elementCountOnDisk=").append(this.getElementCountOnDisk())
					.append(",sizeInMemory=").append(this.getSizeInMemory());
		}
		buf.append(']');
		return buf.toString();
	}

	void incrementHitCount() {
		hitCount.getAndIncrement();
	}

	void incrementMissCount() {
		missCount.getAndIncrement();
	}

	void incrementPutCount() {
		putCount.getAndIncrement();
	}
}
