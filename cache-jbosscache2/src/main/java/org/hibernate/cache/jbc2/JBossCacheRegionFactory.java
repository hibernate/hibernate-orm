/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.cache.jbc2;

import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.jbc2.builder.InvalidationCacheInstanceManager;
import org.hibernate.cache.jbc2.entity.EntityRegionImpl;
import org.hibernate.cfg.Settings;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class JBossCacheRegionFactory implements RegionFactory {
	private CacheInstanceManager cacheInstanceManager;

	public JBossCacheRegionFactory() {
	}

	public JBossCacheRegionFactory(CacheInstanceManager cacheInstanceManager) {
		this.cacheInstanceManager = cacheInstanceManager;
	}

	public void start(Settings settings, Properties properties) throws CacheException {
		if ( cacheInstanceManager == null ) {
			cacheInstanceManager = new InvalidationCacheInstanceManager( settings, properties );
		}
	}

	public void stop() {
		if ( cacheInstanceManager != null ) {
			cacheInstanceManager.release();
		}
	}

	public boolean isMinimalPutsEnabledByDefault() {
		return true;
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	public EntityRegion buildEntityRegion(
			String regionName,
			Properties properties,
			CacheDataDescription metadata) throws CacheException {
		return new EntityRegionImpl( cacheInstanceManager.getEntityCacheInstance(), regionName, metadata );
	}

	public CollectionRegion buildCollectionRegion(
			String regionName,
			Properties properties,
			CacheDataDescription metadata) throws CacheException {
		return null;
	}

	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
		return null;
	}

	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
		return null;
	}

}
