/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.internal;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import org.hibernate.cache.spi.Region;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.stat.CacheableDataStatistics;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCacheableDataStatistics implements CacheableDataStatistics {
	private final @Nullable String cacheRegionName;
	private final @Nullable LongAdder cacheHitCount;
	private final @Nullable LongAdder cacheMissCount;
	private final @Nullable LongAdder cachePutCount;
	private final @Nullable LongAdder cacheRemoveCount;

	public AbstractCacheableDataStatistics(Supplier<@Nullable Region> regionSupplier) {
		final var region = regionSupplier.get();
		if ( region == null ) {
			cacheRegionName = null;
			cacheHitCount = null;
			cacheMissCount = null;
			cachePutCount = null;
			cacheRemoveCount = null;
		}
		else {
			cacheRegionName = region.getName();
			cacheHitCount = new LongAdder();
			cacheMissCount = new LongAdder();
			cachePutCount = new LongAdder();
			cacheRemoveCount = new LongAdder();
		}
	}

	@Override
	public @Nullable String getCacheRegionName() {
		return cacheRegionName;
	}

	@Override
	public long getCacheHitCount() {
		return cacheRegionName == null
				? NOT_CACHED_COUNT
				: NullnessUtil.castNonNull( cacheHitCount ).sum();

	}

	@Override
	public long getCachePutCount() {
		return cacheRegionName == null
				? NOT_CACHED_COUNT
				: NullnessUtil.castNonNull( cachePutCount ).sum();

	}

	@Override
	public long getCacheMissCount() {
		return cacheRegionName == null
				? NOT_CACHED_COUNT
				: NullnessUtil.castNonNull( cacheMissCount ).sum();

	}

	@Override
	public long getCacheRemoveCount() {
		return cacheRegionName == null
				? NOT_CACHED_COUNT
				: NullnessUtil.castNonNull( cacheRemoveCount ).sum();

	}

	public void incrementCacheHitCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache hit count for non-cached data" );
		}

		NullnessUtil.castNonNull( cacheHitCount ).increment();
	}

	public void incrementCacheMissCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache miss count for non-cached data" );
		}

		NullnessUtil.castNonNull( cacheMissCount ).increment();
	}

	public void incrementCachePutCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache put count for non-cached data" );
		}

		NullnessUtil.castNonNull( cachePutCount ).increment();
	}

	public void incrementCacheRemoveCount() {
		if ( cacheRegionName == null ) {
			throw new IllegalStateException( "Illegal attempt to increment cache put count for non-cached data" );
		}

		NullnessUtil.castNonNull( cacheRemoveCount ).increment();
	}

	protected void appendCacheStats(StringBuilder text) {
		text.append( ",cacheRegion=" ).append( cacheRegionName );
		if ( cacheRegionName != null ) {
			text.append( ",cacheHitCount=" ).append( getCacheHitCount() )
					.append( ",cacheMissCount=" ).append( getCacheMissCount() )
					.append( ",cachePutCount=" ).append( getCachePutCount() )
					.append( ",cacheRemoveCount=" ).append( getCacheRemoveCount() );
		}
	}
}
