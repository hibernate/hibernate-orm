package org.hibernate.cache.impl.bridge;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.NoCacheProvider;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.ReflectHelper;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;

/**
 * Acts as a bridge between the {@link RegionFactory} contract and the older
 * {@link CacheProvider} contract.
 *
 * @author Steve Ebersole
 */
public class RegionFactoryCacheProviderBridge implements RegionFactory {
	public static final String DEF_PROVIDER = NoCacheProvider.class.getName();
	private static final Log log = LogFactory.getLog( RegionFactoryCacheProviderBridge.class );

	private CacheProvider cacheProvider;
	private Settings settings;

	public RegionFactoryCacheProviderBridge(Properties properties) {
		String providerClassName = PropertiesHelper.getString( Environment.CACHE_PROVIDER, properties, DEF_PROVIDER );
		log.info( "Cache provider: " + providerClassName );
		try {
			cacheProvider = ( CacheProvider ) ReflectHelper.classForName( providerClassName ).newInstance();
		}
		catch ( Exception cnfe ) {
			throw new CacheException( "could not instantiate CacheProvider [" + providerClassName + "]", cnfe );
		}
	}

	public void start(Settings settings, Properties properties) throws CacheException {
		this.settings = settings;
		cacheProvider.start( properties );
	}

	public void stop() {
		cacheProvider.stop();
		cacheProvider = null;
	}

	public boolean isMinimalPutsEnabledByDefault() {
		return cacheProvider.isMinimalPutsEnabledByDefault();
	}

	public long nextTimestamp() {
		return cacheProvider.nextTimestamp();
	}

	public CacheProvider getCacheProvider() {
		return cacheProvider;
	}

	public EntityRegion buildEntityRegion(
			String regionName,
			Properties properties,
			CacheDataDescription metadata) throws CacheException {
		return new EntityRegionAdapter( cacheProvider.buildCache( regionName, properties ), settings, metadata );
	}

	public CollectionRegion buildCollectionRegion(
			String regionName,
			Properties properties,
			CacheDataDescription metadata) throws CacheException {
		return new CollectionRegionAdapter( cacheProvider.buildCache( regionName, properties ), settings, metadata );
	}

	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
		return new QueryResultsRegionAdapter( cacheProvider.buildCache( regionName, properties ), settings );
	}

	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
		return new TimestampsRegionAdapter( cacheProvider.buildCache( regionName, properties ), settings );
	}


}
