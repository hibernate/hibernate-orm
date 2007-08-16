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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.jbc2.CacheInstanceManager;
import org.hibernate.cfg.Settings;
import org.hibernate.util.PropertiesHelper;

/**
 * Here we build separate {@link Cache} instances for each type of region, but
 * using the jgroups multiplexer under the covers to re-use the same group
 * communication stack.
 * <p/>
 * todo : this can get simplified once JBC implemants their "configuration factory" (the stuff akin to channel factory) - http://jira.jboss.com/jira/browse/JBCACHE-1156
 * 
 * @author Steve Ebersole
 */
public class MultiplexingCacheInstanceManager implements CacheInstanceManager {
	public static final String ENTITY_CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc2.cfg.entity";
	public static final String COLL_CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc2.cfg.collection";
	public static final String TS_CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc2.cfg.ts";
	public static final String QUERY_CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc2.cfg.query";

	public static final String DEF_ENTITY_RESOURCE = "entity-cache.xml";
	public static final String DEF_COLL_RESOURCE = "collection-cache.xml";
	public static final String DEF_TS_RESOURCE = "ts-cache.xml";
	public static final String DEF_QUERY_RESOURCE = "query-cache.xml";

	public static final String OPTIMISTIC_LOCKING_SCHEME = "OPTIMISTIC";

	private static final Logger log = LoggerFactory.getLogger( MultiplexingCacheInstanceManager.class );

	private final Cache jbcEntityCache;
	private final Cache jbcCollectionCache;
	private final Cache jbcTsCache;
	private final Cache jbcQueryCache;

	public MultiplexingCacheInstanceManager(Settings settings, Properties properties) {
		try {
			TransactionManager tm = settings.getTransactionManagerLookup() == null
					? null
					: settings.getTransactionManagerLookup().getTransactionManager( properties );
			if ( settings.isSecondLevelCacheEnabled() ) {
				jbcEntityCache = buildEntityRegionCacheInstance( properties );
				jbcCollectionCache = buildCollectionRegionCacheInstance( properties );
				if ( tm != null ) {
					jbcEntityCache.getConfiguration().getRuntimeConfig().setTransactionManager( tm );
					jbcCollectionCache.getConfiguration().getRuntimeConfig().setTransactionManager( tm );
				}
			}
			else {
				jbcEntityCache = null;
				jbcCollectionCache = null;
			}
			if ( settings.isQueryCacheEnabled() ) {
				jbcTsCache = buildTsRegionCacheInstance( properties );
				jbcQueryCache = buildQueryRegionCacheInstance( properties );
			}
			else {
				jbcTsCache = null;
				jbcQueryCache = null;
			}
		}
		catch( CacheException ce ) {
			throw ce;
		}
		catch( Throwable t ) {
			throw new CacheException( "Unable to start region factory", t );
		}
	}

	public MultiplexingCacheInstanceManager(Cache jbcEntityCache, Cache jbcCollectionCache, Cache jbcTsCache, Cache jbcQueryCache) {
		this.jbcEntityCache = jbcEntityCache;
		this.jbcCollectionCache = jbcCollectionCache;
		this.jbcTsCache = jbcTsCache;
		this.jbcQueryCache = jbcQueryCache;
	}

	protected Cache buildEntityRegionCacheInstance(Properties properties) {
		try {
			String configResource = PropertiesHelper.getString( ENTITY_CACHE_RESOURCE_PROP, properties, DEF_ENTITY_RESOURCE );
			return DefaultCacheFactory.getInstance().createCache( configResource );
		}
		catch( Throwable t ) {
			throw new CacheException( "unable to build entity region cache instance", t );
		}
	}

	protected Cache buildCollectionRegionCacheInstance(Properties properties) {
		try {
			String configResource = PropertiesHelper.getString( COLL_CACHE_RESOURCE_PROP, properties, DEF_COLL_RESOURCE );
			return DefaultCacheFactory.getInstance().createCache( configResource );
		}
		catch( Throwable t ) {
			throw new CacheException( "unable to build collection region cache instance", t );
		}
	}

	protected Cache buildTsRegionCacheInstance(Properties properties) {
		try {
			String configResource = PropertiesHelper.getString( TS_CACHE_RESOURCE_PROP, properties, DEF_TS_RESOURCE );
			return DefaultCacheFactory.getInstance().createCache( configResource );
		}
		catch( Throwable t ) {
			throw new CacheException( "unable to build timestamps region cache instance", t );
		}
	}

	protected Cache buildQueryRegionCacheInstance(Properties properties) {
		try {
			String configResource = PropertiesHelper.getString( QUERY_CACHE_RESOURCE_PROP, properties, DEF_QUERY_RESOURCE );
			return DefaultCacheFactory.getInstance().createCache( configResource );
		}
		catch( Throwable t ) {
			throw new CacheException( "unable to build query region cache instance", t );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Cache getEntityCacheInstance() {
		return jbcEntityCache;
	}

	/**
	 * {@inheritDoc}
	 */
	public Cache getCollectionCacheInstance() {
		return jbcCollectionCache;
	}

	/**
	 * {@inheritDoc}
	 */
	public Cache getQueryCacheInstance() {
		return jbcQueryCache;
	}

	/**
	 * {@inheritDoc}
	 */
	public Cache getTimestampsCacheInstance() {
		return jbcTsCache;
	}

	/**
	 * {@inheritDoc}
	 */
	public void release() {
		if ( jbcEntityCache != null ) {
			try {
				jbcEntityCache.stop();
			}
			catch( Throwable t ) {
				log.info( "Unable to stop entity cache instance", t );
			}
		}
		if ( jbcCollectionCache != null ) {
			try {
				jbcCollectionCache.stop();
			}
			catch( Throwable t ) {
				log.info( "Unable to stop collection cache instance", t );
			}
		}
		if ( jbcTsCache != null ) {
			try {
				jbcTsCache.stop();
			}
			catch( Throwable t ) {
				log.info( "Unable to stop timestamp cache instance", t );
			}
		}
		if ( jbcQueryCache != null ) {
			try {
				jbcQueryCache.stop();
			}
			catch( Throwable t ) {
				log.info( "Unable to stop query cache instance", t );
			}
		}
	}
}
