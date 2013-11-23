/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
