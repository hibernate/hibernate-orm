/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.util.Properties;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.NoCacheRegionFactoryAvailableException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;

/**
 * Factory used if no caching enabled in config...
 *
 * @author Steve Ebersole
 */
public class NoCachingRegionFactory implements RegionFactory {
	/**
	 * Singleton access
	 */
	public static final NoCachingRegionFactory INSTANCE = new NoCachingRegionFactory();

	/**
	 * Constructs a NoCachingRegionFactory.  Although access should generally use {@link #INSTANCE}
	 */
	public NoCachingRegionFactory() {
	}

	@Override
	public void start(SessionFactoryOptions settings, Properties properties) throws CacheException {
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isMinimalPutsEnabledByDefault() {
		return false;
	}

	@Override
	public AccessType getDefaultAccessType() {
		return null;
	}

	@Override
	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	@Override
	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		throw new NoCacheRegionFactoryAvailableException();
	}

	@Override
	public NaturalIdRegion buildNaturalIdRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		throw new NoCacheRegionFactoryAvailableException();
	}

	@Override
	public CollectionRegion buildCollectionRegion(
			String regionName,
			Properties properties,
			CacheDataDescription metadata) throws CacheException {
		throw new NoCacheRegionFactoryAvailableException();
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
		throw new NoCacheRegionFactoryAvailableException();
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
		throw new NoCacheRegionFactoryAvailableException();
	}
}
