/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.stat.CollectionStatistics;

/**
 * Collection related statistics
 *
 * @author Alex Snaps
 */
public class CollectionStatisticsImpl extends AbstractCacheableDataStatistics implements CollectionStatistics, Serializable {
	private final String collectionRole;

	private AtomicLong loadCount = new AtomicLong();
	private AtomicLong fetchCount = new AtomicLong();
	private AtomicLong updateCount = new AtomicLong();
	private AtomicLong removeCount = new AtomicLong();
	private AtomicLong recreateCount = new AtomicLong();

	CollectionStatisticsImpl(CollectionPersister persister) {
		super(
				() -> persister.getCacheAccessStrategy() != null
						? persister.getCacheAccessStrategy().getRegion()
						: null
		);

		this.collectionRole = persister.getRole();
	}

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

	public String toString() {
		final StringBuilder buffer = new StringBuilder()
				.append( "CollectionStatistics" )
				.append( "[collectionRole=" ).append( collectionRole )
				.append( ",loadCount=" ).append( this.loadCount )
				.append( ",fetchCount=" ).append( this.fetchCount )
				.append( ",recreateCount=" ).append( this.recreateCount )
				.append( ",removeCount=" ).append( this.removeCount )
				.append( ",updateCount=" ).append( this.updateCount );
		appendCacheStats( buffer );
		return buffer.append(']').toString();
	}
}
