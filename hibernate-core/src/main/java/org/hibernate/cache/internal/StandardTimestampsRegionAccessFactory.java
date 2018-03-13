/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.TimestampsRegionAccess;
import org.hibernate.cache.spi.TimestampsRegionAccessFactory;
import org.hibernate.cache.spi.TimestampsRegion;

/**
 * Standard Hibernate implementation of the QueryCacheFactory interface.  Returns instances of
 * {@link QueryResultRegionAccessImpl}.
 */
public class StandardTimestampsRegionAccessFactory implements TimestampsRegionAccessFactory {
	/**
	 * Singleton access
	 */
	public static final StandardTimestampsRegionAccessFactory INSTANCE = new StandardTimestampsRegionAccessFactory();

	@Override
	public TimestampsRegionAccess buildTimestampsRegionAccess(
			CacheImplementor cacheManager,
			TimestampsRegion timestampsRegion) {
		return new TimestampsRegionAccessEnabledImpl( timestampsRegion );
	}
}
