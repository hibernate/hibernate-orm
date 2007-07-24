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

import org.jboss.cache.Cache;
import org.jboss.cache.DefaultCacheFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.util.PropertiesHelper;
import org.hibernate.cache.jbc2.CacheInstanceManager;
import org.hibernate.cfg.Settings;

/**
 * A {@link org.hibernate.cache.jbc2.CacheInstanceManager} implementation where we use a single cache instance
 * we assume to be configured for invalidation if operating on a cluster.  Under that
 * assumption, we can store all data into the same {@link Cache} instance.
 *
 * @author Steve Ebersole
 */
public class InvalidationCacheInstanceManager implements CacheInstanceManager {
	public static final String CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc2.cfg.invalidation";
	public static final String DEFAULT_CACHE_RESOURCE = "treecache.xml";

	private static final Log log = LogFactory.getLog( InvalidationCacheInstanceManager.class );

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
		return cache;
	}

	/**
	 * {@inheritDoc}
	 */
	public Cache getTimestampsCacheInstance() {
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
