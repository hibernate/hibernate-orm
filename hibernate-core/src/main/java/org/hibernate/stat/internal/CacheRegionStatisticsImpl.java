/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.cache.spi.ExtendedStatisticsSupport;
import org.hibernate.cache.spi.Region;
import org.hibernate.stat.CacheRegionStatistics;

/**
 * Second level cache statistics of a specific region
 *
 * @author Alex Snaps
 */
public class CacheRegionStatisticsImpl implements CacheRegionStatistics, Serializable {
	private final String regionName;
	private final transient @Nullable ExtendedStatisticsSupport extendedStatisticsSupport;

	private final LongAdder hitCount = new LongAdder();
	private final LongAdder missCount = new LongAdder();
	private final LongAdder putCount = new LongAdder();
	private final LongAdder removeCount = new LongAdder();

	CacheRegionStatisticsImpl(Region region) {
		regionName = region.getName();
		extendedStatisticsSupport =
				region instanceof ExtendedStatisticsSupport extended
						? extended
						: null;
	}

	@Override
	public String getRegionName() {
		return regionName;
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
		return extendedStatisticsSupport == null
				? NO_EXTENDED_STAT_SUPPORT_RETURN
				: extendedStatisticsSupport.getElementCountInMemory();
	}

	@Override
	public long getElementCountOnDisk() {
		return extendedStatisticsSupport == null
				? NO_EXTENDED_STAT_SUPPORT_RETURN
				: extendedStatisticsSupport.getElementCountOnDisk();
	}

	@Override
	public long getSizeInMemory() {
		return extendedStatisticsSupport == null
				? NO_EXTENDED_STAT_SUPPORT_RETURN
				: extendedStatisticsSupport.getSizeInMemory();
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
		final var string =
				new StringBuilder("CacheRegionStatistics")
						.append( "[region=" ).append( regionName )
						.append( ",hitCount=" ).append( hitCount )
						.append( ",missCount=" ).append( missCount )
						.append( ",putCount=" ).append( putCount )
						.append( ",removeCount=" ).append( removeCount );
		if ( extendedStatisticsSupport != null ) {
			string.append( ",elementCountInMemory=" ).append( getElementCountInMemory() )
					.append( ",elementCountOnDisk=" ).append( getElementCountOnDisk() )
					.append( ",sizeInMemory=" ).append( getSizeInMemory() );
		}
		return string.append( ']' ).toString();
	}
}
