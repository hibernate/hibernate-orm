package org.hibernate.stat;

import java.util.concurrent.atomic.AtomicLong;

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
