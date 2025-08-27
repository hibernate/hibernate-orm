/*
 * SPDX-License-Identifier: Apache-2.0
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
	private final LongAdder removeCount = new LongAdder();

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
	public long getRemoveCount() {
		return removeCount.sum();
	}

	@Override
	public long getElementCountInMemory() {
		return region instanceof ExtendedStatisticsSupport extended
				? extended.getElementCountInMemory()
				: NO_EXTENDED_STAT_SUPPORT_RETURN;
	}

	@Override
	public long getElementCountOnDisk() {
		return region instanceof ExtendedStatisticsSupport extended
				? extended.getElementCountOnDisk()
				: NO_EXTENDED_STAT_SUPPORT_RETURN;
	}

	@Override
	public long getSizeInMemory() {
		return region instanceof ExtendedStatisticsSupport extended
				? extended.getSizeInMemory()
				: NO_EXTENDED_STAT_SUPPORT_RETURN;
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


	public void incrementRemoveCount() {
		removeCount.increment();
	}

	@Override
	public String toString() {
		String buf = "CacheRegionStatistics" +
				"[region=" + region.getName() +
				",hitCount=" + this.hitCount +
				",missCount=" + this.missCount +
				",putCount=" + this.putCount +
				",removeCount=" + this.removeCount +
				",elementCountInMemory=" + this.getElementCountInMemory() +
				",elementCountOnDisk=" + this.getElementCountOnDisk() +
				",sizeInMemory=" + this.getSizeInMemory() +
				']';
		return buf;
	}
}
