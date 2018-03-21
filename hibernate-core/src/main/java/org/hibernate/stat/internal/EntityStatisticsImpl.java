/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.EntityStatistics;

/**
 * Entity related statistics
 *
 * @author Alex Snaps
 */
public class EntityStatisticsImpl extends AbstractCacheableDataStatistics implements EntityStatistics, Serializable {
	private final String rootEntityName;

	private AtomicLong loadCount = new AtomicLong();
	private AtomicLong updateCount = new AtomicLong();
	private AtomicLong insertCount = new AtomicLong();
	private AtomicLong deleteCount = new AtomicLong();
	private AtomicLong fetchCount = new AtomicLong();
	private AtomicLong optimisticFailureCount = new AtomicLong();

	EntityStatisticsImpl(EntityPersister rootEntityDescriptor) {
		super(
				() -> rootEntityDescriptor.getCacheAccessStrategy() != null
						? rootEntityDescriptor.getCacheAccessStrategy().getRegion()
						: null
		);
		this.rootEntityName = rootEntityDescriptor.getRootEntityName();
	}

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

	public String toString() {
		final StringBuilder buffer = new StringBuilder()
				.append( "EntityStatistics" )
				.append( "[rootEntityName=" ).append( rootEntityName )
				.append( ",loadCount=" ).append( this.loadCount )
				.append( ",updateCount=" ).append( this.updateCount )
				.append( ",insertCount=" ).append( this.insertCount )
				.append( ",deleteCount=" ).append( this.deleteCount )
				.append( ",fetchCount=" ).append( this.fetchCount )
				.append( ",optimisticLockFailureCount=" ).append( this.optimisticFailureCount );
		appendCacheStats( buffer );
		return buffer.append( ']' ).toString();
	}
}
