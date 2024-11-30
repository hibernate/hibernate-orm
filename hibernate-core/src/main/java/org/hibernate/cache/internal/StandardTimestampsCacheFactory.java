/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.cache.spi.TimestampsRegion;

/**
 * Standard Hibernate implementation of the QueryCacheFactory interface.  Returns instances of
 * {@link QueryResultsCacheImpl}.
 */
public class StandardTimestampsCacheFactory implements TimestampsCacheFactory {
	/**
	 * Singleton access
	 */
	public static final StandardTimestampsCacheFactory INSTANCE = new StandardTimestampsCacheFactory();

	@Override
	public TimestampsCache buildTimestampsCache(
			CacheImplementor cacheManager,
			TimestampsRegion timestampsRegion) {
		return new TimestampsCacheEnabledImpl( timestampsRegion );
	}
}
