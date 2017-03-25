/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;

import org.hibernate.Cache;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.Service;

/**
 * Define internal contact of <tt>Cache API</tt>
 *
 * @author Strong Liu
 */
public interface CacheImplementor extends Service, Cache, Serializable {

	/**
	 * Close all cache regions.
	 */
	void close();

	/**
	 * Get query cache by <tt>region name</tt> or create a new one if none exist.
	 * <p/>
	 * If the region name is null, then default query cache region will be returned.
	 *
	 * @param regionName Query cache region name.
	 * @return The {@code QueryCache} associated with the region name, or default query cache if the region name is <tt>null</tt>.
	 * @throws HibernateException {@code HibernateException} maybe thrown when the creation of new QueryCache instance.
	 */
	QueryCache getQueryCache(String regionName) throws HibernateException;

	/**
	 * Get the default {@code QueryCache}.
	 *
	 * @deprecated Use {@link #getDefaultQueryCache} instead.
	 */
	@Deprecated
	default QueryCache getQueryCache() {
		return getDefaultQueryCache();
	}

	/**
	 * Get the default {@code QueryCache}.
	 */
	QueryCache getDefaultQueryCache();

	/**
	 * Get {@code UpdateTimestampsCache} instance managed by the {@code SessionFactory}.
	 */
	UpdateTimestampsCache getUpdateTimestampsCache();

	/**
	 * Clean up the default {@code QueryCache}.
	 *
 	 * @throws HibernateException
	 */
	void evictQueries() throws HibernateException;

	/**
	 * The underlying RegionFactory in use.
	 *
	 * @return The {@code RegionFactory}
	 */
	RegionFactory getRegionFactory();

	/**
	 * Applies any defined prefix, handling all {@code null} checks.
	 *
	 * @param regionName The region name to qualify
	 *
	 * @return The qualified name
	 */
	String qualifyRegionName(String regionName);

	/**
	 * Get the names of <tt>all</tt> cache regions, including entity, collection, natural-id and query caches.
	 *
	 * @return All cache region names
	 */
	String[] getSecondLevelCacheRegionNames();

	/**
	 * Find the "access strategy" for the named entity cache region.
	 *
	 * @param regionName The name of the region
	 *
	 * @return That region's "access strategy"
	 */
	EntityRegionAccessStrategy getEntityRegionAccess(String regionName);

	/**
	 * Find the "access strategy" for the named collection cache region.
	 *
	 * @param regionName The name of the region
	 *
	 * @return That region's "access strategy"
	 */
	CollectionRegionAccessStrategy getCollectionRegionAccess(String regionName);

	/**
	 * Find the "access strategy" for the named natrual-id cache region.
	 *
	 * @param regionName The name of the region
	 *
	 * @return That region's "access strategy"
	 */
	NaturalIdRegionAccessStrategy getNaturalIdCacheRegionAccessStrategy(String regionName);

	EntityRegionAccessStrategy determineEntityRegionAccessStrategy(PersistentClass model);

	NaturalIdRegionAccessStrategy determineNaturalIdRegionAccessStrategy(PersistentClass model);

	CollectionRegionAccessStrategy determineCollectionRegionAccessStrategy(Collection model);
}
