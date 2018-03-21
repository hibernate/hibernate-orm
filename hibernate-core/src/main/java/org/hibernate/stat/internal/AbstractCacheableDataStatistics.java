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

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCacheableDataStatistics implements CacheableDataStatistics {
	private static final Logger log = Logger.getLogger( AbstractCacheableDataStatistics.class );

	private final String cacheRegionName;

	private final AtomicLong cacheHitCount;
	private final AtomicLong cacheMissCount;
	private final AtomicLong cachePutCount;

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
			this.cacheHitCount = new AtomicLong();
			this.cacheMissCount = new AtomicLong();
			this.cachePutCount = new AtomicLong();
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

		return cacheHitCount.get();
	}

	public long getCachePutCount() {
		if ( cacheRegionName == null ) {
			return NOT_CACHED_COUNT;
		}

		return cachePutCount.get();
	}

	public long getCacheMissCount() {
		if ( cacheRegionName == null ) {
			return NOT_CACHED_COUNT;
		}

		return cacheMissCount.get();
	}

	public void incrementCacheHitCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache hit count for non-cached data" );
		}

		cacheHitCount.getAndIncrement();
	}

	public void incrementCacheMissCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache miss count for non-cached data" );
		}

		cacheMissCount.getAndIncrement();
	}

	public void incrementCachePutCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache put count for non-cached data" );
		}

		cachePutCount.getAndIncrement();
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
