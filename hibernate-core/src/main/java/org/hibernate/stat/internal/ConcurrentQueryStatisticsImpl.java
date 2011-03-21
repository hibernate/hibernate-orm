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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.stat.QueryStatistics;

/**
 * Query statistics (HQL and SQL)
 * <p/>
 * Note that for a cached query, the cache miss is equals to the db count
 *
 * @author Alex Snaps
 */
public class ConcurrentQueryStatisticsImpl extends CategorizedStatistics implements QueryStatistics {
	private final AtomicLong cacheHitCount = new AtomicLong();
	private final AtomicLong cacheMissCount = new AtomicLong();
	private final AtomicLong cachePutCount = new AtomicLong();
	private final AtomicLong executionCount = new AtomicLong();
	private final AtomicLong executionRowCount = new AtomicLong();
	private final AtomicLong executionMaxTime = new AtomicLong();
	private final AtomicLong executionMinTime = new AtomicLong(Long.MAX_VALUE);
	private final AtomicLong totalExecutionTime = new AtomicLong();

	private final Lock readLock;
	private final Lock writeLock;

	{
		ReadWriteLock lock = new ReentrantReadWriteLock();
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

	ConcurrentQueryStatisticsImpl(String query) {
		super(query);
	}

	/**
	 * queries executed to the DB
	 */
	public long getExecutionCount() {
		return executionCount.get();
	}

	/**
	 * Queries retrieved successfully from the cache
	 */
	public long getCacheHitCount() {
		return cacheHitCount.get();
	}

	public long getCachePutCount() {
		return cachePutCount.get();
	}

	public long getCacheMissCount() {
		return cacheMissCount.get();
	}

	/**
	 * Number of lines returned by all the executions of this query (from DB)
	 * For now, {@link org.hibernate.Query#iterate()}
	 * and {@link org.hibernate.Query#scroll()()} do not fill this statistic
	 *
	 * @return The number of rows cumulatively returned by the given query; iterate
	 *         and scroll queries do not effect this total as their number of returned rows
	 *         is not known at execution time.
	 */
	public long getExecutionRowCount() {
		return executionRowCount.get();
	}

	/**
	 * average time in ms taken by the excution of this query onto the DB
	 */
	public long getExecutionAvgTime() {
		// We write lock here to be sure that we always calculate the average time
		// with all updates from the executed applied: executionCount and totalExecutionTime
		// both used in the calculation
		writeLock.lock();
		try {
			long avgExecutionTime = 0;
			if (executionCount.get() > 0) {
				avgExecutionTime = totalExecutionTime.get() / executionCount.get();
			}
			return avgExecutionTime;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * max time in ms taken by the excution of this query onto the DB
	 */
	public long getExecutionMaxTime() {
		return executionMaxTime.get();
	}

	/**
	 * min time in ms taken by the excution of this query onto the DB
	 */
	public long getExecutionMinTime() {
		return executionMinTime.get();
	}

	/**
	 * add statistics report of a DB query
	 *
	 * @param rows rows count returned
	 * @param time time taken
	 */
	void executed(long rows, long time) {
		// read lock is enough, concurrent updates are supported by the underlying type AtomicLong
		// this only guards executed(long, long) to be called, when another thread is executing getExecutionAvgTime()
		readLock.lock();
		try {
			// Less chances for a context switch
			for (long old = executionMinTime.get(); (time < old) && !executionMinTime.compareAndSet(old, time); old = executionMinTime.get());
			for (long old = executionMaxTime.get(); (time > old) && !executionMaxTime.compareAndSet(old, time); old = executionMaxTime.get());
			executionCount.getAndIncrement();
			executionRowCount.addAndGet(rows);
			totalExecutionTime.addAndGet(time);
		} finally {
			readLock.unlock();
		}
	}

	public String toString() {
		return new StringBuilder()
				.append("QueryStatistics")
				.append("[cacheHitCount=").append(this.cacheHitCount)
				.append(",cacheMissCount=").append(this.cacheMissCount)
				.append(",cachePutCount=").append(this.cachePutCount)
				.append(",executionCount=").append(this.executionCount)
				.append(",executionRowCount=").append(this.executionRowCount)
				.append(",executionAvgTime=").append(this.getExecutionAvgTime())
				.append(",executionMaxTime=").append(this.executionMaxTime)
				.append(",executionMinTime=").append(this.executionMinTime)
				.append(']')
				.toString();
	}

	void incrementCacheHitCount() {
		cacheHitCount.getAndIncrement();
	}

	void incrementCacheMissCount() {
		cacheMissCount.getAndIncrement();
	}

	void incrementCachePutCount() {
		cachePutCount.getAndIncrement();
	}
}
