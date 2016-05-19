/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * average time in ms taken by the execution of this query onto the DB
	 */
	public long getExecutionAvgTime() {
		return (long) getExecutionAvgTimeAsDouble();
	}

	/**
	 * average time in ms as double taken by the execution of this query onto the DB
	 */
	public double getExecutionAvgTimeAsDouble() {
		// We write lock here to be sure that we always calculate the average time
		// with all updates from the executed applied: executionCount and totalExecutionTime
		// both used in the calculation
		writeLock.lock();
		try {
			double avgExecutionTime = 0;
			if ( executionCount.get() > 0 ) {
				avgExecutionTime = totalExecutionTime.get() / (double) executionCount
						.get();
			}
			return avgExecutionTime;
		}
		finally {
			writeLock.unlock();
		}
	}

	/**
	 * max time in ms taken by the execution of this query onto the DB
	 */
	public long getExecutionMaxTime() {
		return executionMaxTime.get();
	}

	/**
	 * min time in ms taken by the execution of this query onto the DB
	 */
	public long getExecutionMinTime() {
		return executionMinTime.get();
	}

	/**
	 * total time in ms taken by the execution of this query onto the DB
	 */
	public long getExecutionTotalTime() {
		return totalExecutionTime.get();
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
			for (long old = executionMinTime.get(); (time < old) && !executionMinTime.compareAndSet(old, time); old = executionMinTime.get()) {}
			for (long old = executionMaxTime.get(); (time > old) && !executionMaxTime.compareAndSet(old, time); old = executionMaxTime.get()) {}
			executionCount.getAndIncrement();
			executionRowCount.addAndGet(rows);
			totalExecutionTime.addAndGet(time);
		}
		finally {
			readLock.unlock();
		}
	}

	public String toString() {
		return "QueryStatistics"
				+ "[cacheHitCount=" + this.cacheHitCount
				+ ",cacheMissCount=" + this.cacheMissCount
				+ ",cachePutCount=" + this.cachePutCount
				+ ",executionCount=" + this.executionCount
				+ ",executionRowCount=" + this.executionRowCount
				+ ",executionAvgTime=" + this.getExecutionAvgTime()
				+ ",executionMaxTime=" + this.executionMaxTime
				+ ",executionMinTime=" + this.executionMinTime
				+ ']';
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
