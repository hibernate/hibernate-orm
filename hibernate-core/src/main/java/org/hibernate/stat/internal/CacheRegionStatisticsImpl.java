/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.cache.spi.ExtendedStatisticsSupport;
import org.hibernate.cache.spi.Region;
import org.hibernate.stat.CacheRegionStatistics;

/**
 * Second level cache statistics of a specific region
 *
 * @author Alex Snaps
 */
public class CacheRegionStatisticsImpl implements CacheRegionStatistics, Serializable {
	private final transient Region region;

	private final LongAdder hitCount = new LongAdder();
	private final LongAdder missCount = new LongAdder();
	private final LongAdder putCount = new LongAdder();

	CacheRegionStatisticsImpl(Region region) {
		this.region = region;
	}

	@Override
	public String getRegionName() {
		return region.getName();
	}

	@Override
	public long getHitCount() {
		return hitCount.sum();
	}

	@Override
	public long getMissCount() {
		return missCount.sum();
	}

	@Override
	public long getPutCount() {
		return putCount.sum();
	}

	@Override
	public long getElementCountInMemory() {
		if ( region instanceof ExtendedStatisticsSupport ) {
			return ( (ExtendedStatisticsSupport) region ).getElementCountInMemory();
		}
		return NO_EXTENDED_STAT_SUPPORT_RETURN;
	}

	@Override
	public long getElementCountOnDisk() {
		if ( region instanceof ExtendedStatisticsSupport ) {
			return ( (ExtendedStatisticsSupport) region ).getElementCountOnDisk();
		}
		return NO_EXTENDED_STAT_SUPPORT_RETURN;
	}

	@Override
	public long getSizeInMemory() {
		if ( region instanceof ExtendedStatisticsSupport ) {
			return ( (ExtendedStatisticsSupport) region ).getSizeInMemory();
		}
		return NO_EXTENDED_STAT_SUPPORT_RETURN;
	}

	void incrementHitCount() {
		hitCount.increment();
	}

	void incrementMissCount() {
		missCount.increment();
	}

	void incrementPutCount() {
		putCount.increment();
	}

	@Override
	public String toString() {
		String buf = "CacheRegionStatistics" +
				"[region=" + region.getName() +
				",hitCount=" + this.hitCount +
				",missCount=" + this.missCount +
				",putCount=" + this.putCount +
				",elementCountInMemory=" + this.getElementCountInMemory() +
				",elementCountOnDisk=" + this.getElementCountOnDisk() +
				",sizeInMemory=" + this.getSizeInMemory() +
				']';
		return buf;
	}
}
