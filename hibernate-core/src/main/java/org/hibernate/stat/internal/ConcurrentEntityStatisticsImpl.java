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

import org.hibernate.stat.EntityStatistics;

/**
 * Entity related statistics
 *
 * @author Alex Snaps
 */
public class ConcurrentEntityStatisticsImpl extends CategorizedStatistics implements EntityStatistics {

	ConcurrentEntityStatisticsImpl(String name) {
		super(name);
	}

	private	AtomicLong loadCount			  =	new	AtomicLong();
	private	AtomicLong updateCount			  =	new	AtomicLong();
	private	AtomicLong insertCount			  =	new	AtomicLong();
	private	AtomicLong deleteCount			  =	new	AtomicLong();
	private	AtomicLong fetchCount			  =	new	AtomicLong();
	private	AtomicLong optimisticFailureCount =	new	AtomicLong();

	public long getDeleteCount() {
		return deleteCount.get();
	}

	public long getInsertCount() {
		return insertCount.get();
	}

	public long getLoadCount() {
		return loadCount.get();
	}

	public long getUpdateCount() {
		return updateCount.get();
	}

	public long getFetchCount() {
		return fetchCount.get();
	}

	public long getOptimisticFailureCount() {
		return optimisticFailureCount.get();
	}

	public String toString() {
		return new StringBuilder()
				.append("EntityStatistics")
				.append("[loadCount=").append(this.loadCount)
				.append(",updateCount=").append(this.updateCount)
				.append(",insertCount=").append(this.insertCount)
				.append(",deleteCount=").append(this.deleteCount)
				.append(",fetchCount=").append(this.fetchCount)
				.append(",optimisticLockFailureCount=").append(this.optimisticFailureCount)
				.append(']')
				.toString();
	}

	void incrementLoadCount() {
		loadCount.getAndIncrement();
	}

	void incrementFetchCount() {
		fetchCount.getAndIncrement();
	}

	void incrementUpdateCount() {
		updateCount.getAndIncrement();
	}

	void incrementInsertCount() {
		insertCount.getAndIncrement();
	}

	void incrementDeleteCount() {
		deleteCount.getAndIncrement();
	}

	void incrementOptimisticFailureCount() {
		optimisticFailureCount.getAndIncrement();
	}
}
