package org.hibernate.cache.impl;

import java.util.Properties;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.NoCachingEnabledException;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cfg.Settings;

/**
 * Factory used if no caching enabled in config...
 *
 * @author Steve Ebersole
 */
public class NoCachingRegionFactory implements RegionFactory {


	public NoCachingRegionFactory(Properties properties) {
	}

	public void start(Settings settings, Properties properties) throws CacheException {
	}

	public void stop() {
	}

	public boolean isMinimalPutsEnabledByDefault() {
		return false;
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		throw new NoCachingEnabledException();
	}

	public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		throw new NoCachingEnabledException();
	}

	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
		throw new NoCachingEnabledException();
	}

	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
		throw new NoCachingEnabledException();
	}
}
