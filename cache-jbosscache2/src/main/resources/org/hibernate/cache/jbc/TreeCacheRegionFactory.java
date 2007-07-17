package org.hibernate.cache.impl.jbc;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.DefaultCacheFactory;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cfg.Settings;
import org.hibernate.util.PropertiesHelper;

/**
 * A factory for building regions based on a JBossCache
 * {@link org.jboss.cache.Node}.  Here we are utilizing the
 * same underlying {@link org.jboss.cache.Node} instance for each jbcTreeCache region.
 *
 * @author Steve Ebersole
 */
public class TreeCacheRegionFactory implements RegionFactory {
	public static final String ENTITY_CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc.cfg.entity";
	public static final String COLL_CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc.cfg.collection";
	public static final String TS_CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc.cfg.ts";
	public static final String QUERY_CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc.cfg.query";

	public static final String DEF_ENTITY_RESOURCE = "entity-cache.xml";
	public static final String DEF_COLL_RESOURCE = "collection-cache.xml";
	public static final String DEF_TS_RESOURCE = "ts-cache.xml";
	public static final String DEF_QUERY_RESOURCE = "query-cache.xml";

	public static final String OPTIMISTIC_LOCKING_SCHEME = "OPTIMISTIC";

	private static final Log log = LogFactory.getLog( TreeCacheRegionFactory.class );

	private Cache jbcEntityCache;
	private Cache jbcCollectionCache;
	private Cache jbcTsCache;
	private Cache jbcQueryCache;
	private boolean useOptimisticLocking;

	public void start(Settings settings, Properties properties) throws CacheException {
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
			if ( settings.isQueryCacheEnabled() ) {
				jbcTsCache = buildTsRegionCacheInstance( properties );
				jbcQueryCache = buildQueryRegionCacheInstance( properties );
			}
		}
		catch( CacheException ce ) {
			throw ce;
		}
		catch( Throwable t ) {
			throw new CacheException( "Unable to start region factory", t );
		}
//		String resource = PropertiesHelper.getString( Environment.CACHE_PROVIDER_CONFIG, properties, DEFAULT_CONFIG );
//		log.debug( "Configuring basic TreeCache RegionFactory from resource [" + resource + "]" );
//		try {
//			jbcTreeCache = new TreeCache();
//			PropertyConfigurator config = new PropertyConfigurator();
//			config.configure( jbcTreeCache, resource );
//			TransactionManagerLookup transactionManagerLookup = settings.getTransactionManagerLookup();
//			if ( transactionManagerLookup != null ) {
//				jbcTreeCache.setTransactionManagerLookup(
//						new TransactionManagerLookupAdaptor( transactionManagerLookup, properties )
//				);
//			}
//			jbcTreeCache.start();
//			useOptimisticLocking = OPTIMISTIC_LOCKING_SCHEME.equalsIgnoreCase( jbcTreeCache.getNodeLockingScheme() );
//		}
//		catch ( Exception e ) {
//			throw new CacheException( e );
//		}
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

	public void stop() {
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

	public boolean isMinimalPutsEnabledByDefault() {
		return true;
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata) {
		if ( useOptimisticLocking && !metadata.isVersioned() ) {
			log.warn( "JBossCache configured to use optimistic locking, but entity to be cached is not versioned [" + regionName + "]" );
		}
		else if ( !useOptimisticLocking && metadata.isVersioned() ) {
			log.info( "Caching versioned entity without optimisitic locking; consider optimistic locking if all cached entities are versioned" );
		}
		return new EntityRegionAdapter( regionName, metadata );
	}

	public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		return null;
	}

	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
		return null;
	}

	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
		return null;
	}

	private class EntityRegionAdapter extends TreeCacheRegionAdapter implements EntityRegion {
		private final CacheDataDescription metadata;

		public EntityRegionAdapter(String regionName, CacheDataDescription metadata) {
			super( TreeCacheRegionFactory.this.jbcTreeCache, regionName );
			this.metadata = metadata;
		}

		public boolean isTransactionAware() {
			return jbcTreeCache.getTransactionManager() != null;
		}

		public CacheDataDescription getCacheDataDescription() {
			return metadata;
		}

		public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
			if ( ! ( AccessType.READ_ONLY.equals( accessType ) || AccessType.TRANSACTIONAL.equals( accessType ) ) ) {
				throw new CacheException( "TreeCacheRegionFactory only supports ( " + AccessType.READ_ONLY.getName() + " | " + AccessType.TRANSACTIONAL + " ) access strategies [" + accessType.getName() + "]" );
			}
			// todo : implement :)
			return null;
		}
	}
}
