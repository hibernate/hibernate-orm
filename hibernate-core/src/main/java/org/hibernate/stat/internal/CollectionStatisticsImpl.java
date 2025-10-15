/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.stat.CollectionStatistics;

/**
 * Collection related statistics
 *
 * @author Alex Snaps
 */
public class CollectionStatisticsImpl extends AbstractCacheableDataStatistics implements CollectionStatistics, Serializable {

	private final String collectionRole;
	private final LongAdder loadCount = new LongAdder();
	private final LongAdder fetchCount = new LongAdder();
	private final LongAdder updateCount = new LongAdder();
	private final LongAdder removeCount = new LongAdder();
	private final LongAdder recreateCount = new LongAdder();

	CollectionStatisticsImpl(CollectionPersister persister) {
		super( () -> {
			final var cache = persister.getCacheAccessStrategy();
			return cache == null ? null : cache.getRegion();
		} );
		collectionRole = persister.getRole();
	}

	public long getLoadCount() {
		return loadCount.sum();
	}

	public long getFetchCount() {
		return fetchCount.sum();
	}

	public long getRecreateCount() {
		return recreateCount.sum();
	}

	public long getRemoveCount() {
		return removeCount.sum();
	}

	public long getUpdateCount() {
		return updateCount.sum();
	}

	void incrementLoadCount() {
		loadCount.increment();
	}

	void incrementFetchCount() {
		fetchCount.increment();
	}

	void incrementUpdateCount() {
		updateCount.increment();
	}

	void incrementRecreateCount() {
		recreateCount.increment();
	}

	void incrementRemoveCount() {
		removeCount.increment();
	}

	public String toString() {
		final var text = new StringBuilder()
				.append( "CollectionStatistics" )
				.append( "[collectionRole=" ).append( collectionRole )
				.append( ",loadCount=" ).append( this.loadCount )
				.append( ",fetchCount=" ).append( this.fetchCount )
				.append( ",recreateCount=" ).append( this.recreateCount )
				.append( ",removeCount=" ).append( this.removeCount )
				.append( ",updateCount=" ).append( this.updateCount );
		appendCacheStats( text );
		return text.append(']').toString();
	}
}
