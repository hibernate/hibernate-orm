/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import org.hibernate.engine.spi.CacheImplementor;

/**
 * Defines a factory for query cache instances.  These factories are responsible for
 * creating individual QueryCache instances.
 *
 * @author Steve Ebersole
 */
public interface QueryCacheFactory {
	/**
	 * Builds a named query cache.
	 *
	 * @param region The cache region
	 * @param cacheManager The CacheImplementor reference.
	 *
	 * @return The cache.
	 */
	QueryCache buildQueryCache(QueryResultsRegion region, CacheImplementor cacheManager);
}
