/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.stat.internal;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.hibernate.cache.spi.Region;
import org.hibernate.stat.CacheableDataStatistics;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCacheableDataStatistics implements CacheableDataStatistics {
	private final String cacheRegionName;

	private final AtomicLong cacheHitCount = new AtomicLong();
	private final AtomicLong cacheMissCount = new AtomicLong();
	private final AtomicLong cachePutCount = new AtomicLong();

	public AbstractCacheableDataStatistics(Supplier<Region> regionSupplier) {
		final Region region = regionSupplier.get();
		this.cacheRegionName = region != null ? region.getName() : null;
	}

	@Override
	public String getCacheRegionName() {
		return cacheRegionName;
	}

	public long getCacheHitCount() {
		return cacheHitCount.get();
	}

	public long getCachePutCount() {
		return cachePutCount.get();
	}

	public long getCacheMissCount() {
		return cacheMissCount.get();
	}

	void incrementCacheHitCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache hit count for non-cached data" );
		}

		cacheHitCount.getAndIncrement();
	}

	void incrementCacheMissCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache miss count for non-cached data" );
		}

		cacheMissCount.getAndIncrement();
	}

	void incrementCachePutCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache put count for non-cached data" );
		}

		cachePutCount.getAndIncrement();
	}

	protected void appendCacheStats(StringBuilder buf) {
		if ( cacheRegionName == null ) {
			return;
		}

		buf.append( ",cacheRegion=" ).append( cacheRegionName )
				.append( ",cacheHitCount=" ).append( getCacheHitCount() )
				.append( ",cacheMissCount=" ).append( getCacheMissCount() )
				.append( ",cachePutCount=" ).append( getCachePutCount() );

	}
}
