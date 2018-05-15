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

import org.hibernate.stat.NaturalIdQueryStatistics;

/**
 * NaturalId cache statistics of a specific region
 * 
 * @author Eric Dalquist
 */
public class ConcurrentNaturalIdQueryStatisticsImpl extends CategorizedStatistics implements NaturalIdQueryStatistics {
	private static final long serialVersionUID = 1L;

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

	ConcurrentNaturalIdQueryStatisticsImpl(String entityName) {
		super( entityName );
	}

	public String getEntityName() {
		return getCategoryName();
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
	public String toString() {
		final StringBuilder buf = new StringBuilder()
			.append( "NaturalIdCacheStatistics" )
			.append( ",executionCount=" ).append( this.executionCount )
			.append( ",executionAvgTime=" ).append( this.getExecutionAvgTime() )
			.append( ",executionMinTime=" ).append( this.executionMinTime )
			.append( ",executionMaxTime=" ).append( this.executionMaxTime );
		buf.append( ']' );
		return buf.toString();
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
}
