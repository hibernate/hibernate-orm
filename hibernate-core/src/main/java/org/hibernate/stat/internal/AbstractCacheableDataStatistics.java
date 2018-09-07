/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.stat.internal;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import org.hibernate.cache.spi.Region;
import org.hibernate.stat.CacheableDataStatistics;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCacheableDataStatistics implements CacheableDataStatistics {
	private final String cacheRegionName;
	private final LongAdder cacheHitCount;
	private final LongAdder cacheMissCount;
	private final LongAdder cachePutCount;

	public AbstractCacheableDataStatistics(Supplier<Region> regionSupplier) {
		final Region region = regionSupplier.get();
		if ( region == null ) {
			this.cacheRegionName = null;
			this.cacheHitCount = null;
			this.cacheMissCount = null;
			this.cachePutCount = null;
		}
		else {
			this.cacheRegionName = region.getName();
			this.cacheHitCount = new LongAdder();
			this.cacheMissCount = new LongAdder();
			this.cachePutCount = new LongAdder();
		}
	}

	@Override
	public String getCacheRegionName() {
		return cacheRegionName;
	}

	public long getCacheHitCount() {
		if ( cacheRegionName == null ) {
			return NOT_CACHED_COUNT;
		}

		return cacheHitCount.sum();
	}

	public long getCachePutCount() {
		if ( cacheRegionName == null ) {
			return NOT_CACHED_COUNT;
		}

		return cachePutCount.sum();
	}

	public long getCacheMissCount() {
		if ( cacheRegionName == null ) {
			return NOT_CACHED_COUNT;
		}

		return cacheMissCount.sum();
	}

	public void incrementCacheHitCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache hit count for non-cached data" );
		}

		cacheHitCount.increment();
	}

	public void incrementCacheMissCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache miss count for non-cached data" );
		}

		cacheMissCount.increment();
	}

	public void incrementCachePutCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache put count for non-cached data" );
		}

		cachePutCount.increment();
	}

	protected void appendCacheStats(StringBuilder buf) {
		buf.append( ",cacheRegion=" ).append( cacheRegionName );

		if ( cacheRegionName == null ) {
			return;
		}

		buf.append( ",cacheHitCount=" ).append( getCacheHitCount() )
				.append( ",cacheMissCount=" ).append( getCacheMissCount() )
				.append( ",cachePutCount=" ).append( getCachePutCount() );

	}
}
