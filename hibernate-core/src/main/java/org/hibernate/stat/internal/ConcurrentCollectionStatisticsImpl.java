/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.stat.CollectionStatistics;

/**
 * Collection related statistics
 *
 * @author Alex Snaps
 */
public class ConcurrentCollectionStatisticsImpl extends CategorizedStatistics implements CollectionStatistics {
	ConcurrentCollectionStatisticsImpl(String role) {
		super(role);
	}

	private	AtomicLong loadCount	 = new AtomicLong();
	private	AtomicLong fetchCount	 = new AtomicLong();
	private	AtomicLong updateCount	 = new AtomicLong();
	private	AtomicLong removeCount	 = new AtomicLong();
	private	AtomicLong recreateCount = new AtomicLong();

	public long getLoadCount() {
		return loadCount.get();
	}

	public long getFetchCount() {
		return fetchCount.get();
	}

	public long getRecreateCount() {
		return recreateCount.get();
	}

	public long getRemoveCount() {
		return removeCount.get();
	}

	public long getUpdateCount() {
		return updateCount.get();
	}

	public String toString() {
		return new StringBuilder()
				.append("CollectionStatistics")
				.append("[loadCount=").append(this.loadCount)
				.append(",fetchCount=").append(this.fetchCount)
				.append(",recreateCount=").append(this.recreateCount)
				.append(",removeCount=").append(this.removeCount)
				.append(",updateCount=").append(this.updateCount)
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

	void incrementRecreateCount() {
		recreateCount.getAndIncrement();
	}

	void incrementRemoveCount() {
		removeCount.getAndIncrement();
	}
}
