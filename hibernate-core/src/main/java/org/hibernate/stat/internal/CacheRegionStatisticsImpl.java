/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.cache.spi.ExtendedStatisticsSupport;
import org.hibernate.cache.spi.Region;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;

/**
 * Second level cache statistics of a specific region
 *
 * @author Alex Snaps
 */
public class CacheRegionStatisticsImpl implements CacheRegionStatistics, SecondLevelCacheStatistics, Serializable {
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
		StringBuilder buf = new StringBuilder().append( "CacheRegionStatistics" )
				.append( "[region=").append( region.getName() )
				.append( ",hitCount=").append( this.hitCount )
				.append( ",missCount=").append( this.missCount )
				.append( ",putCount=").append( this.putCount )
				.append( ",elementCountInMemory=" ).append( this.getElementCountInMemory() )
				.append( ",elementCountOnDisk=" ).append( this.getElementCountOnDisk() )
				.append( ",sizeInMemory=" ).append( this.getSizeInMemory() )
				.append( ']' );
		return buf.toString();
	}
}
