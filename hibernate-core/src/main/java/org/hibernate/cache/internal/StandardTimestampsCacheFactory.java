/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
