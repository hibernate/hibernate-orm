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
package org.hibernate.cache.jbc2.builder;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.DefaultCacheFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.jbc2.CacheInstanceManager;
import org.hibernate.cache.jbc2.util.CacheModeHelper;
import org.hibernate.cfg.Settings;
import org.hibernate.util.PropertiesHelper;

/**
 * A {@link CacheInstanceManager} implementation where we use a single cache instance
 * we assume to be configured for invalidation if operating on a cluster.  Under that
 * assumption, we can store all data into the same {@link Cache} instance.
 * <p/>
 * todo : this is built on the assumption that JBC clustered invalidation is changed to keep the "cache node" around on the other "cluster nodes"
 *
 * @author Steve Ebersole
 */
public class InvalidationCacheInstanceManager implements CacheInstanceManager {
	public static final String CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc2.cfg.invalidation";
	public static final String DEFAULT_CACHE_RESOURCE = "treecache.xml";

	private static final Logger log = LoggerFactory.getLogger( InvalidationCacheInstanceManager.class );

	private final Cache cache;

	public InvalidationCacheInstanceManager(Settings settings, Properties properties) {
		String configResource = PropertiesHelper.getString( CACHE_RESOURCE_PROP, properties, DEFAULT_CACHE_RESOURCE );
		cache = DefaultCacheFactory.getInstance().createCache( configResource, false );
		if ( settings.getTransactionManagerLookup() != null ) {
			TransactionManager tm = settings.getTransactionManagerLookup().getTransactionManager( properties );
			if ( tm != null ) {
				cache.getConfiguration().getRuntimeConfig().setTransactionManager( tm );
			}
		}
		cache.start();
	}

	public InvalidationCacheInstanceManager(Cache cache) {
		this.cache = cache;
	}

	/**
	 * {@inheritDoc}
	 */
	public Cache getEntityCacheInstance() {
		return cache;
	}

	/**
	 * {@inheritDoc}
	 */
	public Cache getCollectionCacheInstance() {
		return cache;
	}

	/**
	 * {@inheritDoc}
	 */
	public Cache getQueryCacheInstance() {
		if ( CacheModeHelper.isClusteredInvalidation( cache ) ) {
			throw new CacheException( "Query cache not supported for clustered invalidation" );
		}
		return cache;
	}

	/**
	 * {@inheritDoc}
	 */
	public Cache getTimestampsCacheInstance() {
		if ( CacheModeHelper.isClusteredInvalidation( cache ) ) {
			throw new CacheException( "Query cache not supported for clustered invalidation" );
		}
		return cache;
	}

	/**
	 * {@inheritDoc}
	 */
	public void release() {
		if ( cache != null ) {
			try {
				cache.stop();
			}
			catch( Throwable t ) {
				log.warn( "Unable to stop cache instance", t );
			}
		}
	}
}
