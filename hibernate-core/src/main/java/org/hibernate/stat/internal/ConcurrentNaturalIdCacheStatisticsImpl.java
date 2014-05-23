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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.cache.spi.NaturalIdCacheKey;
import org.hibernate.cache.spi.Region;
import org.hibernate.stat.NaturalIdCacheStatistics;

/**
 * NaturalId cache statistics of a specific region
 * 
 * @author Eric Dalquist
 */
public class ConcurrentNaturalIdCacheStatisticsImpl extends CategorizedStatistics implements NaturalIdCacheStatistics {
	private static final long serialVersionUID = 1L;
	private final transient Region region;
	private final AtomicLong hitCount = new AtomicLong();
	private final AtomicLong missCount = new AtomicLong();
	private final AtomicLong putCount = new AtomicLong();
	private final AtomicLong executionCount = new AtomicLong();
	private final AtomicLong executionMaxTime = new AtomicLong();
	private final AtomicLong executionMinTime = new AtomicLong( Long.MAX_VALUE );
	private final AtomicLong totalExecutionTime = new AtomicLong();

	private final Lock readLock;
	private final Lock writeLock;
	{
		final ReadWriteLock lock = new ReentrantReadWriteLock();
		this.readLock = lock.readLock();
		this.writeLock = lock.writeLock();
	}

	ConcurrentNaturalIdCacheStatisticsImpl(Region region) {
		super( region.getName() );
		this.region = region;
	}

	@Override
	public long getHitCount() {
		return this.hitCount.get();
	}

	@Override
	public long getMissCount() {
		return this.missCount.get();
	}

	@Override
	public long getPutCount() {
		return this.putCount.get();
	}

	/**
	 * queries executed to the DB
	 */
	@Override
	public long getExecutionCount() {
		return this.executionCount.get();
	}

	/**
	 * average time in ms taken by the excution of this query onto the DB
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
	 * max time in ms taken by the excution of this query onto the DB
	 */
	@Override
	public long getExecutionMaxTime() {
		return this.executionMaxTime.get();
	}

	/**
	 * min time in ms taken by the excution of this query onto the DB
	 */
	@Override
	public long getExecutionMinTime() {
		return this.executionMinTime.get();
	}

	@Override
	public long getElementCountInMemory() {
		return this.region.getElementCountInMemory();
	}

	@Override
	public long getElementCountOnDisk() {
		return this.region.getElementCountOnDisk();
	}

	@Override
	public long getSizeInMemory() {
		return this.region.getSizeInMemory();
	}

	@Override
	public Map getEntries() {
		final Map map = new HashMap();
		final Iterator iter = this.region.toMap().entrySet().iterator();
		while ( iter.hasNext() ) {
			final Map.Entry me = (Map.Entry) iter.next();
			map.put( ( (NaturalIdCacheKey) me.getKey() ).getNaturalIdValues(), me.getValue() );
		}
		return map;
	}

	@Override
	public String toString() {
		final StringBuilder buf = new StringBuilder()
			.append( "NaturalIdCacheStatistics" )
			.append( "[hitCount=" ).append( this.hitCount )
			.append( ",missCount=" ).append( this.missCount )
			.append( ",putCount=" ).append( this.putCount )
			.append( ",executionCount=" ).append( this.executionCount )
			.append( ",executionAvgTime=" ).append( this.getExecutionAvgTime() )
			.append( ",executionMinTime=" ).append( this.executionMinTime )
			.append( ",executionMaxTime=" ).append( this.executionMaxTime );
		// not sure if this would ever be null but wanted to be careful
		if ( this.region != null ) {
			buf.append( ",elementCountInMemory=" ).append( this.getElementCountInMemory() )
				.append( ",elementCountOnDisk=" ).append( this.getElementCountOnDisk() )
				.append( ",sizeInMemory=" ).append( this.getSizeInMemory() );
		}
		buf.append( ']' );
		return buf.toString();
	}

	void incrementHitCount() {
		this.hitCount.getAndIncrement();
	}

	void incrementMissCount() {
		this.missCount.getAndIncrement();
	}

	void incrementPutCount() {
		this.putCount.getAndIncrement();
	}

	void queryExecuted(long time) {
		// read lock is enough, concurrent updates are supported by the underlying type AtomicLong
		// this only guards executed(long, long) to be called, when another thread is executing getExecutionAvgTime()
		this.readLock.lock();
		try {
			// Less chances for a context switch
			for ( long old = this.executionMinTime.get(); time < old && !this.executionMinTime.compareAndSet( old, time ); old = this.executionMinTime.get() ) {;}
			for ( long old = this.executionMaxTime.get(); time > old && !this.executionMaxTime.compareAndSet( old, time ); old = this.executionMaxTime.get() ) {;}
			this.executionCount.getAndIncrement();
			this.totalExecutionTime.addAndGet( time );
		}
		finally {
			this.readLock.unlock();
		}
	}
}
