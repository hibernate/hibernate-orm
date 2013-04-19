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
package org.hibernate.cache.spi;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Settings;
import org.hibernate.service.Service;

/**
 * Contract for building second level cache regions.
 * <p/>
 * Implementors should define a constructor in one of two forms:<ul>
 * <li>MyRegionFactoryImpl({@link java.util.Properties})</li>
 * <li>MyRegionFactoryImpl()</li>
 * </ul>
 * Use the first when we need to read config properties prior to
 * {@link #start(Settings, Properties)} being called.
 *
 * @author Steve Ebersole
 */
public interface RegionFactory extends Service {

	/**
	 * Lifecycle callback to perform any necessary initialization of the
	 * underlying cache implementation(s).  Called exactly once during the
	 * construction of a {@link org.hibernate.internal.SessionFactoryImpl}.
	 *
	 * @param settings The settings in effect.
	 * @param properties The defined cfg properties
	 *
	 * @throws org.hibernate.cache.CacheException Indicates problems starting the L2 cache impl;
	 * considered as a sign to stop {@link org.hibernate.SessionFactory}
	 * building.
	 */
	public void start(Settings settings, Properties properties) throws CacheException;

	/**
	 * Lifecycle callback to perform any necessary cleanup of the underlying
	 * cache implementation(s).  Called exactly once during
	 * {@link org.hibernate.SessionFactory#close}.
	 */
	public void stop();

	/**
	 * By default should we perform "minimal puts" when using this second
	 * level cache implementation?
	 *
	 * @return True if "minimal puts" should be performed by default; false
	 *         otherwise.
	 */
	public boolean isMinimalPutsEnabledByDefault();

	/**
	 * Get the default access type for {@link EntityRegion entity} and
	 * {@link CollectionRegion collection} regions.
	 *
	 * @return This factory's default access type.
	 */
	public AccessType getDefaultAccessType();

	/**
	 * Generate a timestamp.
	 * <p/>
	 * This is generally used for cache content locking/unlocking purposes
	 * depending upon the access-strategy being used.
	 *
	 * @return The generated timestamp.
	 */
	public long nextTimestamp();

	/**
	 * Build a cache region specialized for storing entity data.
	 *
	 * @param regionName The name of the region.
	 * @param properties Configuration properties.
	 * @param metadata Information regarding the type of data to be cached
	 *
	 * @return The built region
	 *
	 * @throws CacheException Indicates problems building the region.
	 */
	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException;

	/**
	 * Build a cache region specialized for storing NaturalId to Primary Key mappings.
	 *
	 * @param regionName The name of the region.
	 * @param properties Configuration properties.
	 * @param metadata Information regarding the type of data to be cached
	 *
	 * @return The built region
	 *
	 * @throws CacheException Indicates problems building the region.
	 */
	public NaturalIdRegion buildNaturalIdRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException;

	/**
	 * Build a cache region specialized for storing collection data.
	 *
	 * @param regionName The name of the region.
	 * @param properties Configuration properties.
	 * @param metadata Information regarding the type of data to be cached
	 *
	 * @return The built region
	 *
	 * @throws CacheException Indicates problems building the region.
	 */
	public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException;

	/**
	 * Build a cache region specialized for storing query results.
	 *
	 * @param regionName The name of the region.
	 * @param properties Configuration properties.
	 *
	 * @return The built region
	 *
	 * @throws CacheException Indicates problems building the region.
	 */
	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException;

	/**
	 * Build a cache region specialized for storing update-timestamps data.
	 *
	 * @param regionName The name of the region.
	 * @param properties Configuration properties.
	 *
	 * @return The built region
	 *
	 * @throws CacheException Indicates problems building the region.
	 */
	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException;
}
