/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.engine.spi.CacheImplementor;

/**
 * Standard Hibernate implementation of the QueryCacheFactory interface.  Returns instances of
 * {@link StandardQueryCache}.
 */
public class StandardQueryCacheFactory implements QueryCacheFactory {
	/**
	 * Singleton access
	 */
	public static final StandardQueryCacheFactory INSTANCE = new StandardQueryCacheFactory();

	@Override
	public QueryCache buildQueryCache(QueryResultsRegion region, CacheImplementor cacheManager) {
		return new StandardQueryCache( region, cacheManager );
	}
}
