/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.stat.internal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.ExtendedStatisticsSupport;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.stat.NaturalIdCacheStatistics;
import org.hibernate.stat.NaturalIdStatistics;

import static org.hibernate.stat.CacheRegionStatistics.NO_EXTENDED_STAT_SUPPORT_RETURN;

/**
 * @deprecated (since 5.3) {@link NaturalIdStatistics} is preferred over
 * {@link NaturalIdCacheStatistics}
 *
 * @author Eric Dalquist
 */
@Deprecated
public class DeprecatedNaturalIdCacheStatisticsImpl implements NaturalIdCacheStatistics, Serializable {
	private final String regionName;
	private final transient Set<NaturalIdDataAccess> accessStrategies;

	private final AtomicLong executionCount = new AtomicLong();
	private final AtomicLong executionMaxTime = new AtomicLong();
	private final AtomicLong executionMinTime = new AtomicLong( Long.MAX_VALUE );
	private final AtomicLong totalExecutionTime = new AtomicLong();

	private final AtomicLong cacheHitCount = new AtomicLong();
	private final AtomicLong cacheMissCount = new AtomicLong();
	private final AtomicLong cachePutCount = new AtomicLong();

	private final Lock readLock;
	private final Lock writeLock;

	DeprecatedNaturalIdCacheStatisticsImpl(String regionName, Set<NaturalIdDataAccess> accessStrategies) {
		this.regionName = regionName;
		this.accessStrategies = accessStrategies;
		final ReadWriteLock lock = new ReentrantReadWriteLock();
		this.readLock = lock.readLock();
		this.writeLock = lock.writeLock();
	}

	/**
	 * queries executed to the DB
	 */
	@Override
	public long getExecutionCount() {
		return this.executionCount.get();
	}

	/**
	 * average time in ms taken by the execution of this query onto the DB
	 */
	@Override
	public long getExecutionAvgTime() {
		// We write lock here to be sure that we always calculate the average time
		// with all updates from the executed applied: executionCount and totalExecutionTime
		// both used in the calculation
		this.writeLock.lock();
		try {
			long avgExecutionTime = 0;
			if ( this.executionCount.get() > 0 ) {
				avgExecutionTime = this.totalExecutionTime.get() / this.executionCount.get();
			}
			return avgExecutionTime;
		}
		finally {
			this.writeLock.unlock();
		}
	}

	/**
	 * max time in ms taken by the execution of this query onto the DB
	 */
	@Override
	public long getExecutionMaxTime() {
		return this.executionMaxTime.get();
	}

	/**
	 * min time in ms taken by the execution of this query onto the DB
	 */
	@Override
	public long getExecutionMinTime() {
		return this.executionMinTime.get();
	}

	@Override
	public long getHitCount() {
		return this.cacheHitCount.get();
	}

	@Override
	public long getMissCount() {
		return this.cacheMissCount.get();
	}

	@Override
	public long getPutCount() {
		return this.cachePutCount.get();
	}

	@Override
	public long getElementCountInMemory() {
		long count = 0;
		HashSet<Region> processedRegions = null;

		for ( NaturalIdDataAccess accessStrategy : accessStrategies ) {
			final DomainDataRegion region = accessStrategy.getRegion();
			if ( ExtendedStatisticsSupport.class.isInstance( region ) ) {

			}

			if ( region instanceof ExtendedStatisticsSupport ) {
				if ( processedRegions == null ) {
					processedRegions = new HashSet<>();
				}
				if ( processedRegions.add( region ) ) {
					count += ( (ExtendedStatisticsSupport) region ).getElementCountInMemory();
				}
			}

		}

		if ( count == 0 ) {
			return NO_EXTENDED_STAT_SUPPORT_RETURN;
		}

		return count;
	}

	@Override
	public long getElementCountOnDisk() {
		long count = 0;
		HashSet<Region> processedRegions = null;

		for ( NaturalIdDataAccess accessStrategy : accessStrategies ) {
			final DomainDataRegion region = accessStrategy.getRegion();
			if ( ExtendedStatisticsSupport.class.isInstance( region ) ) {

			}

			if ( region instanceof ExtendedStatisticsSupport ) {
				if ( processedRegions == null ) {
					processedRegions = new HashSet<>();
				}
				if ( processedRegions.add( region ) ) {
					count += ( (ExtendedStatisticsSupport) region ).getElementCountOnDisk();
				}
			}

		}

		if ( count == 0 ) {
			return NO_EXTENDED_STAT_SUPPORT_RETURN;
		}

		return count;
	}

	@Override
	public long getSizeInMemory() {
		long count = 0;
		HashSet<Region> processedRegions = null;

		for ( NaturalIdDataAccess accessStrategy : accessStrategies ) {
			final DomainDataRegion region = accessStrategy.getRegion();
			if ( ExtendedStatisticsSupport.class.isInstance( region ) ) {

			}

			if ( region instanceof ExtendedStatisticsSupport ) {
				if ( processedRegions == null ) {
					processedRegions = new HashSet<>();
				}
				if ( processedRegions.add( region ) ) {
					count += ( (ExtendedStatisticsSupport) region ).getElementCountOnDisk();
				}
			}

		}

		if ( count == 0 ) {
			return NO_EXTENDED_STAT_SUPPORT_RETURN;
		}

		return count;
	}

	void incrementHitCount() {
		cacheHitCount.getAndIncrement();
	}

	void incrementMissCount() {
		cacheMissCount.getAndIncrement();
	}

	void incrementPutCount() {
		cachePutCount.getAndIncrement();
	}

	void queryExecuted(long time) {
		// read lock is enough, concurrent updates are supported by the underlying type AtomicLong
		// this only guards executed(long, long) to be called, when another thread is executing getExecutionAvgTime()
		this.readLock.lock();
		try {
			// Less chances for a context switch
			//noinspection StatementWithEmptyBody
			for ( long old = this.executionMinTime.get(); time < old && !this.executionMinTime.compareAndSet( old, time ); old = this.executionMinTime.get() ) {
			}
			//noinspection StatementWithEmptyBody
			for ( long old = this.executionMaxTime.get(); time > old && !this.executionMaxTime.compareAndSet( old, time ); old = this.executionMaxTime.get() ) {
			}
			this.executionCount.getAndIncrement();
			this.totalExecutionTime.addAndGet( time );
		}
		finally {
			this.readLock.unlock();
		}
	}

	@Override
	public String toString() {
		final StringBuilder buf = new StringBuilder()
				.append( "NaturalIdCacheStatistics(deprecated)" )
				.append( "[regionName=" ).append( regionName )
				.append( ",executionCount=" ).append( getExecutionCount() )
				.append( ",executionAvgTime=" ).append( getExecutionAvgTime() )
				.append( ",executionMinTime=" ).append( getExecutionMinTime() )
				.append( ",executionMaxTime=" ).append( getExecutionMaxTime() );

		buf.append( ",hitCount=" ).append( getHitCount() )
				.append( ",missCount=" ).append( getMissCount() )
				.append( ",putCount=" ).append( getPutCount() )
				.append( ",elementCountInMemory=" ).append( this.getElementCountInMemory() )
				.append( ",elementCountOnDisk=" ).append( this.getElementCountOnDisk() )
				.append( ",sizeInMemory=" ).append( this.getSizeInMemory() );

		return buf.append( ']' ).toString();
	}
}
