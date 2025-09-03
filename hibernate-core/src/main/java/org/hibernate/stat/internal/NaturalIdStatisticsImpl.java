/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.NaturalIdStatistics;

/**
 * NaturalId cache statistics of a specific entity
 *
 * @author Eric Dalquist
 */
public class NaturalIdStatisticsImpl extends AbstractCacheableDataStatistics implements NaturalIdStatistics, Serializable {

	private final String rootEntityName;
	private final AtomicLong executionCount = new AtomicLong();
	private final AtomicLong executionMaxTime = new AtomicLong();
	private final AtomicLong executionMinTime = new AtomicLong( Long.MAX_VALUE );
	private final AtomicLong totalExecutionTime = new AtomicLong();

	private final Lock readLock;
	private final Lock writeLock;

	NaturalIdStatisticsImpl(EntityPersister rootEntityDescriptor) {
		super( () -> {
			final var cache = rootEntityDescriptor.getNaturalIdCacheAccessStrategy();
			return cache != null ? cache.getRegion() : null;
		} );
		rootEntityName = rootEntityDescriptor.getRootEntityName();
		final ReadWriteLock lock = new ReentrantReadWriteLock();
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

	/**
	 * queries executed to the DB
	 */
	@Override
	public long getExecutionCount() {
		return executionCount.get();
	}

	/**
	 * average time in ms taken by the execution of this query onto the DB
	 */
	@Override
	public long getExecutionAvgTime() {
		// We write lock here to be sure that we always calculate the average time
		// with all updates from the executed applied: executionCount and totalExecutionTime
		// both used in the calculation
		writeLock.lock();
		try {
			long avgExecutionTime = 0;
			if ( this.executionCount.get() > 0 ) {
				avgExecutionTime = totalExecutionTime.get() / executionCount.get();
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
	@Override
	public long getExecutionMaxTime() {
		return executionMaxTime.get();
	}

	/**
	 * min time in ms taken by the execution of this query onto the DB
	 */
	@Override
	public long getExecutionMinTime() {
		return executionMinTime.get();
	}

	void queryExecuted(long time) {
		// read lock is enough, concurrent updates are supported by the underlying type AtomicLong
		// this only guards executed(long, long) to be called, when another thread is executing getExecutionAvgTime()
		readLock.lock();
		try {
			// Less chance for a context switch
			//noinspection StatementWithEmptyBody
			for ( long old = executionMinTime.get();
				time < old && !executionMinTime.compareAndSet( old, time );
				old = executionMinTime.get() ) {
			}
			//noinspection StatementWithEmptyBody
			for ( long old = executionMaxTime.get();
				time > old && !executionMaxTime.compareAndSet( old, time );
				old = executionMaxTime.get() ) {
			}
			executionCount.getAndIncrement();
			totalExecutionTime.addAndGet( time );
		}
		finally {
			readLock.unlock();
		}
	}

	@Override
	public String toString() {
		final var text = new StringBuilder()
				.append( "NaturalIdCacheStatistics" )
				.append( "[rootEntityName=" ).append( rootEntityName )
				.append( ",executionCount=" ).append( this.executionCount )
				.append( ",executionAvgTime=" ).append( this.getExecutionAvgTime() )
				.append( ",executionMinTime=" ).append( this.executionMinTime )
				.append( ",executionMaxTime=" ).append( this.executionMaxTime );
		appendCacheStats( text );
		return text.append( ']' ).toString();
	}
}
