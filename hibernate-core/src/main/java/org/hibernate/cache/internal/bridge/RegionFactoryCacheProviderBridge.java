/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.internal.bridge;

import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.NoCacheProvider;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CacheProvider;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Acts as a bridge between the {@link RegionFactory} contract and the older
 * {@link CacheProvider} contract.
 *
 * @author Steve Ebersole
 */
public class RegionFactoryCacheProviderBridge implements RegionFactory {
	public static final String DEF_PROVIDER = NoCacheProvider.class.getName();

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       RegionFactoryCacheProviderBridge.class.getName());

	private CacheProvider cacheProvider;
	private Settings settings;

	public RegionFactoryCacheProviderBridge(Properties properties) {
		String providerClassName = ConfigurationHelper.getString( Environment.CACHE_PROVIDER, properties, DEF_PROVIDER );
        LOG.cacheProvider(providerClassName);
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

	/**
	 * {@inheritDoc}
	 */
	public AccessType getDefaultAccessType() {
		// we really have no idea
		return null;
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
