/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
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
public class QueryStatisticsImpl implements QueryStatistics {
	private final String query;

	private final LongAdder cacheHitCount = new LongAdder();
	private final LongAdder cacheMissCount = new LongAdder();
	private final LongAdder cachePutCount = new LongAdder();
	private final LongAdder executionCount = new LongAdder();
	private final LongAdder executionRowCount = new LongAdder();
	private final AtomicLong executionMaxTime = new AtomicLong();
	private final AtomicLong executionMinTime = new AtomicLong(Long.MAX_VALUE);
	private final AtomicLong totalExecutionTime = new AtomicLong();

	private final LongAdder planCacheHitCount = new LongAdder();
	private final LongAdder planCacheMissCount = new LongAdder();
	private final AtomicLong planCompilationTotalMicroseconds = new AtomicLong();


	private final Lock readLock;
	private final Lock writeLock;

	QueryStatisticsImpl(String query) {
		this.query = query;
		ReadWriteLock lock = new ReentrantReadWriteLock();
		this.readLock = lock.readLock();
		this.writeLock = lock.writeLock();
	}

	/**
	 * queries executed to the DB
	 */
	public long getExecutionCount() {
		return executionCount.sum();
	}

	/**
	 * Queries retrieved successfully from the cache
	 */
	public long getCacheHitCount() {
		return cacheHitCount.sum();
	}

	public long getCachePutCount() {
		return cachePutCount.sum();
	}

	public long getCacheMissCount() {
		return cacheMissCount.sum();
	}

	/**
	 * Number of lines returned by all the executions of this query (from DB)
	 * For now, {@link org.hibernate.Query#iterate()}
	 * and {@link org.hibernate.Query#scroll()} do not fill this statistic
	 *
	 * @return The number of rows cumulatively returned by the given query; iterate
	 *         and scroll queries do not effect this total as their number of returned rows
	 *         is not known at execution time.
	 */
	public long getExecutionRowCount() {
		return executionRowCount.sum();
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
			final long ec = executionCount.sum();
			if ( ec > 0 ) {
				avgExecutionTime = totalExecutionTime.get() / (double) ec;
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
	 * Query plan successfully fetched from the cache
	 */
	public long getPlanCacheHitCount() {
		return planCacheHitCount.sum();
	}

	/**
	 * Query plan not fetched from the cache
	 */
	public long getPlanCacheMissCount() {
		return planCacheMissCount.sum();
	}

	/**
	 * Query plan overall compiled total
	 */
	public long getPlanCompilationTotalMicroseconds() {
		return planCompilationTotalMicroseconds.get();
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
			for ( long old = executionMinTime.get(); (time < old) && !executionMinTime.compareAndSet(old, time); old = executionMinTime.get() ) {}
			for ( long old = executionMaxTime.get(); (time > old) && !executionMaxTime.compareAndSet(old, time); old = executionMaxTime.get() ) {}
			executionCount.increment();
			executionRowCount.add( rows );
			totalExecutionTime.addAndGet( time );
		}
		finally {
			readLock.unlock();
		}
	}

	/**
	 * add plan statistics report of a DB query
	 *
	 * @param microseconds time taken
	 */
	void compiled(long microseconds) {
		planCacheMissCount.increment();
		planCompilationTotalMicroseconds.addAndGet( microseconds );
	}

	void incrementCacheHitCount() {
		cacheHitCount.increment();
	}

	void incrementCacheMissCount() {
		cacheMissCount.increment();
	}

	void incrementCachePutCount() {
		cachePutCount.increment();
	}

	void incrementPlanCacheHitCount() {
		planCacheHitCount.increment();
	}

	public String toString() {
		return "QueryStatistics"
				+ "[query=" + query
				+ ",cacheHitCount=" + this.cacheHitCount
				+ ",cacheMissCount=" + this.cacheMissCount
				+ ",cachePutCount=" + this.cachePutCount
				+ ",planCacheHitCount=" + this.planCacheHitCount
				+ ",planCacheMissCount=" + this.planCacheMissCount
				+ ",executionCount=" + this.executionCount
				+ ",executionRowCount=" + this.executionRowCount
				+ ",executionAvgTime=" + this.getExecutionAvgTime()
				+ ",executionMaxTime=" + this.executionMaxTime
				+ ",executionMinTime=" + this.executionMinTime
				+ ']';
	}
}
