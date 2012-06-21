/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.Cache;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.service.Service;

/**
 * Define internal contact of <tt>Cache API</tt>
 *
 * @author Strong Liu <stliu@hibernate.org>
 */
public interface CacheImplementor extends Service, Cache, Serializable {

	/**
	 * Close all cache regions.
	 */
	public void close();

	/**
	 * Get query cache by <tt>region name</tt> or create a new one if none exist.
	 * <p/>
	 * If the region name is null, then default query cache region will be returned.
	 *
	 * @param regionName Query cache region name.
	 * @return The {@code QueryCache} associated with the region name, or default query cache if the region name is <tt>null</tt>.
	 * @throws HibernateException {@code HibernateException} maybe thrown when the creation of new QueryCache instance.
	 */
	public QueryCache getQueryCache(String regionName) throws HibernateException;

	/**
	 * Get the default {@code QueryCache}.
	 */
	public QueryCache getQueryCache();

	/**
	 * Add {@code Region} to this Cache scope.
	 *
	 * @param name The region name.
	 * @param region The {@code Region} instance.
	 */
	public void addCacheRegion(String name, Region region);

	/**
	 * Get {@code UpdateTimestampsCache} instance managed by the {@code SessionFactory}.
	 */
	public UpdateTimestampsCache getUpdateTimestampsCache();

	/**
	 * Clean up the default {@code QueryCache}.
	 *
 	 * @throws HibernateException
	 */
	public void evictQueries() throws HibernateException;

	/**
	 * Get second level cache region by its name.
	 *
	 * @param regionName The region name.
	 * @return The second level cache region.
	 */
	public Region getSecondLevelCacheRegion(String regionName);

	/**
	 * Get natural id cache region by its name.
	 *
	 * @param regionName The region name.
	 * @return The natural id cache region.
	 */
	public Region getNaturalIdCacheRegion(String regionName);

	/**
	 * Get <tt>all</tt> cache regions, including query cache.
	 *
	 * @return The map contains all cache regions with region name as the key.
	 */
	public Map<String, Region> getAllSecondLevelCacheRegions();

	/**
	 *
	 * @return The {@code RegionFactory}
	 */
	public RegionFactory getRegionFactory();
}
