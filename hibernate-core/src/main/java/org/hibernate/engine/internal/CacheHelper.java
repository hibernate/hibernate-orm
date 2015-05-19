/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.Serializable;

import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.NaturalIdCacheKey;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Steve Ebersole
 */
public final class CacheHelper {
	private CacheHelper() {
	}

	public static Serializable fromSharedCache(
			SessionImplementor session,
			NaturalIdCacheKey cacheKey,
			NaturalIdRegionAccessStrategy cacheAccessStrategy) {
		return fromSharedCache( session, (Object) cacheKey, cacheAccessStrategy );
	}

	private static Serializable fromSharedCache(
			SessionImplementor session,
			Object cacheKey,
			RegionAccessStrategy cacheAccessStrategy) {
		Serializable cachedValue = null;
		try {
			session.getEventListenerManager().cacheGetStart();
			cachedValue = (Serializable) cacheAccessStrategy.get( cacheKey, session.getTimestamp() );
		}
		finally {
			session.getEventListenerManager().cacheGetEnd( cachedValue != null );
		}
		return cachedValue;
	}

	public static Serializable fromSharedCache(
			SessionImplementor session,
			CacheKey cacheKey,
			RegionAccessStrategy cacheAccessStrategy) {
		return fromSharedCache( session, (Object) cacheKey, cacheAccessStrategy );
	}
}
